const test = require('node:test');
const assert = require('node:assert');
const net = require('node:net');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { io } = require('socket.io-client');

function freePort() {
  return new Promise((resolve, reject) => {
    const srv = net.createServer();
    srv.once('error', reject);
    srv.listen(0, '127.0.0.1', () => {
      const { port } = srv.address();
      srv.close(() => resolve(port));
    });
  });
}

function startServer(port) {
  const child = spawn(process.execPath, [path.join(__dirname, '..', 'index.js')], {
    env: { ...process.env, TRADE_FEED_PORT: String(port) },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('server did not start in time')), 10000);
    child.stdout.on('data', (chunk) => {
      if (chunk.toString().includes('Socket.IO server running')) {
        clearTimeout(timer);
        resolve(child);
      }
    });
    child.once('error', reject);
  });
}

function connect(port) {
  const socket = io(`http://127.0.0.1:${port}`, { transports: ['websocket'] });
  return new Promise((resolve, reject) => {
    socket.once('connect', () => resolve(socket));
    socket.once('connect_error', reject);
  });
}

test('subscriber receives a message published on its topic', async (t) => {
  const port = await freePort();
  const server = await startServer(port);
  const subscriber = await connect(port);
  const publisher = await connect(port);

  t.after(() => {
    subscriber.close();
    publisher.close();
    server.kill();
  });

  const topic = 'trades';
  const payload = { id: 42, ticker: 'AAPL' };

  const received = new Promise((resolve) => {
    subscriber.on('publish', (message) => {
      if (message.type === 'trade') resolve(message);
    });
  });

  subscriber.emit('subscribe', topic);
  await new Promise((resolve) => setTimeout(resolve, 200));
  publisher.emit('publish', { topic, type: 'trade', payload });

  const message = await received;
  assert.strictEqual(message.topic, topic);
  assert.strictEqual(message.from, publisher.id);
  assert.deepStrictEqual(message.payload, payload);
});
