# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Local benchmark proxies: no request/response bodies or credentials persisted."""
import asyncio, concurrent.futures, http.client, http.server, json, pathlib, threading, time, socket
from hpack import Decoder

ROOT = pathlib.Path('/workspace/hubble-perf')
LOCK = threading.Lock()
def emit(obj):
    with LOCK, (ROOT / 'trace.jsonl').open('a') as f:
        f.write(json.dumps(dict(ts=time.time(), **obj)) + '\n')

def http_proxy(port, upstream, kind):
    class Handler(http.server.BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'
        def setup(self):
            super().setup()
            self.connection.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        def log_message(self, *args): pass
        def forward(self):
            start = time.perf_counter()
            body = self.rfile.read(int(self.headers.get('Content-Length', 0)))
            headers = {k: v for k, v in self.headers.items() if k.lower() not in ('host', 'connection', 'accept-encoding')}
            conn = http.client.HTTPConnection('127.0.0.1', upstream, timeout=310)
            status = 502
            size = 0
            try:
                conn.request(self.command, self.path, body=body, headers=headers)
                response = conn.getresponse()
                status = response.status
                data = response.read()
                size = len(data)
                self.send_response(status)
                for k, v in response.getheaders():
                    if k.lower() not in ('transfer-encoding', 'content-length', 'connection'):
                        self.send_header(k, v)
                self.send_header('Content-Length', str(size))
                self.end_headers()
                self.wfile.write(data)
            except Exception as e:
                emit(dict(kind=kind, error=str(e), path=self.path))
            finally:
                conn.close()
                emit(dict(kind=kind, method=self.command, path=self.path, status=status, bytes=size, ms=(time.perf_counter()-start)*1000))
        do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = forward
    http.server.ThreadingHTTPServer(('127.0.0.1', port), Handler).serve_forever()

class Frames:
    def __init__(self, request, streams):
        self.buf = b''
        self.preface = request
        self.request = request
        self.streams = streams
        self.decoder = Decoder()
        self.decoder.max_allowed_table_size = 65536
        self.blocks = {}
    def feed(self, data):
        self.buf += data
        if self.preface:
            if len(self.buf) < 24: return
            self.buf = self.buf[24:]
            self.preface = False
        while len(self.buf) >= 9:
            length = int.from_bytes(self.buf[:3], 'big')
            if len(self.buf) < length + 9: return
            kind, flags = self.buf[3], self.buf[4]
            sid = int.from_bytes(self.buf[5:9], 'big') & 0x7fffffff
            payload, self.buf = self.buf[9:9+length], self.buf[9+length:]
            if kind in (1, 9):
                if kind == 1:
                    if flags & 8: payload = payload[1:len(payload)-payload[0]]
                    if flags & 32: payload = payload[5:]
                self.blocks[sid] = self.blocks.get(sid, b'') + payload
                if flags & 4:
                    try:
                        headers = dict(self.decoder.decode(self.blocks.pop(sid)))
                        if self.request and ':path' in headers:
                            self.streams[sid] = dict(path=headers[':path'], start=time.perf_counter(), bytes=0)
                        elif not self.request and 'grpc-status' in headers and sid in self.streams:
                            entry = self.streams.pop(sid)
                            emit(dict(kind='pd-grpc', path=entry['path'], status=headers['grpc-status'], bytes=entry['bytes'], ms=(time.perf_counter()-entry['start'])*1000))
                    except Exception as e:
                        emit(dict(kind='grpc-parser-error', error=str(e)))
            elif kind == 0 and not self.request and sid in self.streams:
                self.streams[sid]['bytes'] += len(payload)

async def grpc_proxy(reader, writer):
    other_reader, other_writer = await asyncio.open_connection('127.0.0.1', 8686)
    streams = {}
    async def pump(src, dst, request):
        parser = Frames(request, streams)
        try:
            while data := await src.read(65536):
                parser.feed(data)
                dst.write(data)
                await dst.drain()
        finally:
            dst.close()
    await asyncio.gather(pump(reader, other_writer, True), pump(other_reader, writer, False), return_exceptions=True)

async def main():
    for args in [(8080, 18080, 'server-http'), (18620, 8620, 'pd-http')]:
        threading.Thread(target=http_proxy, args=args, daemon=True).start()
    server = await asyncio.start_server(grpc_proxy, '127.0.0.1', 18686)
    async with server: await server.serve_forever()

if __name__ == '__main__': asyncio.run(main())
