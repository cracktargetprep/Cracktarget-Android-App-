"""Local QA CONNECT proxy for Android emulators without working DNS."""
import base64
import http.client
import select
import socket
from urllib.parse import unquote, urlsplit
from urllib.request import getproxies, proxy_bypass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOSTS = {
    'cracktarget.com', 'www.cracktarget.com', 'c0.wp.com',
    'i0.wp.com', 'i1.wp.com', 'i2.wp.com', '0.gravatar.com',
    '1.gravatar.com', '2.gravatar.com', 'fonts-api.wp.com',
    'fonts.wp.com', 'fonts.googleapis.com', 'fonts.gstatic.com',
}


def connect_upstream(host):
    proxy = getproxies().get('https')
    if not proxy or proxy_bypass(host):
        return socket.create_connection((host, 443), timeout=15)

    location = urlsplit(proxy)
    if location.scheme != 'http' or not location.hostname:
        raise OSError('Expected an HTTP upstream proxy')
    connection = http.client.HTTPConnection(location.hostname, location.port or 80, timeout=15)
    headers = {}
    if location.username is not None:
        user = unquote(location.username)
        password = unquote(location.password or '')
        token = base64.b64encode(f'{user}:{password}'.encode()).decode('ascii')
        headers['Proxy-Authorization'] = f'Basic {token}'
    connection.set_tunnel(host, 443, headers=headers)
    connection.connect()
    upstream, connection.sock = connection.sock, None
    return upstream


class ConnectHandler(BaseHTTPRequestHandler):
    def do_CONNECT(self):
        host, _, port = self.path.rpartition(':')
        if host not in HOSTS or port != '443':
            self.send_error(403)
            return
        try:
            upstream = connect_upstream(host)
        except (OSError, http.client.HTTPException):
            self.send_error(502)
            return
        with upstream:
            self.send_response(200, 'Connection established')
            self.end_headers()
            sockets = (self.connection, upstream)
            try:
                while ready := select.select(sockets, [], [], 45)[0]:
                    for source in ready:
                        data = source.recv(65536)
                        if not data:
                            return
                        sockets[1 - sockets.index(source)].sendall(data)
            except OSError:
                return


if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', 18888), ConnectHandler).serve_forever()
