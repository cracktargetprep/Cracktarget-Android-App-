"""Run against a local PHP fixture. No production data or Google accounts are used."""
import hashlib
import http.client
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import tempfile
import time
from urllib.parse import urlencode


def run(port):
    def request(action, state=None, *, form=None, cookies='', method=None, **fields):
        params = {'ctapp_v2': action, **fields}
        if state is not None:
            params['state'] = state
        conn = http.client.HTTPConnection('127.0.0.1', port, timeout=5)
        headers = {'Cookie': cookies}
        if form is not None:
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
        conn.request(method or ('POST' if form is not None else 'GET'), '/?' + urlencode(params),
                     urlencode(form) if form is not None else None, headers)
        reply = conn.getresponse()
        result = reply.status, dict(reply.getheaders()), reply.read().decode()
        conn.close()
        return result

    state, verifier = secrets.token_hex(32), secrets.token_hex(32)
    challenge = hashlib.sha256(verifier.encode()).hexdigest()
    assert request('start', ['bad'], challenge=challenge)[0] == 400
    status, headers, _ = request('start', state, challenge=challenge)
    assert status == 303 and 'no-store' in headers['Cache-Control']
    assert all(x in headers['Set-Cookie'].lower() for x in ('secure', 'httponly', 'samesite=lax'))
    browser = headers['Set-Cookie'].split(';', 1)[0]
    assert request('start', state, challenge=challenge)[0] == 409
    assert request('confirm', state)[0] == 403
    status, headers, _ = request('confirm', state, cookies=browser)
    assert status == 303 and '/my-account/?redirect_to=' in headers['Location']
    assert request('confirm', state, cookies=browser + '; fixture_user=9')[0] == 403
    customer = browser + '; fixture_user=7'
    assert 'Continue as Fixture student' in request('confirm', state, cookies=customer)[2]
    assert request('approve', state, cookies=customer)[0] == 405
    assert request('approve', state, form={}, cookies=customer)[0] == 403
    status, _, html = request('approve', state, form={'ct_android_nonce': 'fixture-nonce'}, cookies=customer)
    assert status == 200 and 'package=com.cracktarget.app' in html
    code = re.search(r'code=([a-f0-9]{64})', html).group(1)
    assert request('approve', state, form={'ct_android_nonce': 'fixture-nonce'}, cookies=customer)[0] == 403
    assert request('verify', state, cookies='fixture_user=7')[0] == 403
    assert request('exchange', state, form={'code': code, 'verifier': '0' * 64})[0] == 403
    assert request('exchange', state, code=code, verifier=verifier)[0] == 405
    proper = {'code': code, 'verifier': verifier}
    assert request('exchange', state, form=proper, cookies='fixture_user=8')[0] == 403
    status, headers, _ = request('exchange', state, form=proper)
    assert status == 303 and headers['Set-Cookie'].startswith('fixture_user=7;')
    assert request('verify', state)[0] == 403
    assert request('verify', state, cookies='fixture_user=8')[0] == 403
    status, headers, _ = request('verify', state, cookies='fixture_user=7')
    assert status == 303 and headers['Location'].endswith('/my-account/?ctapp_signed_in=' + state)
    assert request('exchange', state, form=proper)[0] == 403
    print('Bridge contract passed: browser binding, CSRF, customer role, proof, POST exchange, cookie receipt and replay rejection.')


if __name__ == '__main__':
    fixture = Path(__file__).with_name('bridge-fixture.php').resolve()
    with tempfile.TemporaryDirectory() as temp, socket.socket() as probe:
        probe.bind(('127.0.0.1', 0))
        port = probe.getsockname()[1]
        probe.close()
        with subprocess.Popen([os.environ.get('PHP', 'php'), '-n', '-S', f'127.0.0.1:{port}', str(fixture)],
                              env={**os.environ, 'CT_TEST_DB': str(Path(temp) / 'db.json')},
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) as server:
            try:
                for _ in range(50):
                    try:
                        with socket.create_connection(('127.0.0.1', port), timeout=0.1):
                            break
                    except OSError:
                        time.sleep(0.1)
                else:
                    raise RuntimeError('PHP fixture did not start')
                run(port)
            finally:
                server.terminate()
