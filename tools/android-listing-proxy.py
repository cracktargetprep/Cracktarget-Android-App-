"""Local QA CONNECT proxy for Android emulators without working DNS."""
import select
import socket
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOSTS = {
    'cracktarget.com', 'www.cracktarget.com', 'c0.wp.com',
    'i0.wp.com', 'i1.wp.com', 'i2.wp.com', '0.gravatar.com',
    '1.gravatar.com', '2.gravatar.com', 'fonts-api.wp.com',
    'fonts.wp.com', 'fonts.googleapis.com', 'fonts.gstatic.com',
}


class ConnectHandler(BaseHTTPRequestHandler):
    def do_CONNECT(self):
        if self.path.rpartition(':')[::2] not in {(host, '443') for host in HOSTS}:
            self.send_error(403)
            return
        host = self.path.rsplit(':', 1)[0]
        try:
            upstream = socket.create_connection((host, 443), timeout=15)
        except OSError:
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
