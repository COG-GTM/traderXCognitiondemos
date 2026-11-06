const { test, before, after } = require('node:test');
const assert = require('node:assert');
const { spawn } = require('node:child_process');
const path = require('node:path');
const { io } = require('socket.io-client');

const port = 18000 + Math.floor(Math.random() * 1000);
const url = `http://127.0.0.1:${port}`;
let server;
let exited = false;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function client() {
  const socket = io(url, { transports: ['websocket'], reconnection: false });
  return new Promise((resolve, reject) => {
    socket.on('connect', () => resolve(socket));
    socket.on('connect_error', reject);
  });
}

before(async () => {
  server = spawn(process.execPath, ['index.js'], {
    cwd: path.join(__dirname, '..'),
    env: { ...process.env, TRADE_FEED_PORT: String(port) },
  });
  server.on('exit', () => { exited = true; });
  await new Promise((resolve) => server.stdout.on('data', (d) => {
    if (d.toString().includes('running')) resolve();
  }));
});

after(() => server.kill());

test('malformed messages are ignored and do not crash the feed', async () => {
  const subscriber = await client();
  const received = [];
  subscriber.on('publish', (m) => { if (m.from !== 'System') received.push(m); });
  subscriber.emit('subscribe', '/trades');

  const attacker = await client();
  attacker.emit('publish');
  attacker.emit('publish', null);
  attacker.emit('publish', 'not-an-object');
  attacker.emit('publish', 42);
  attacker.emit('publish', [1, 2]);
  attacker.emit('publish', { topic: { nested: true } });
  attacker.emit('publish', { topic: 'x'.repeat(1000) });
  attacker.emit('subscribe');
  attacker.emit('subscribe', { topic: '/trades' });
  attacker.emit('unusbscribe', null);
  await sleep(300);

  assert.strictEqual(exited, false, 'trade-feed process exited');
  assert.strictEqual(received.length, 0);

  const publisher = await client();
  publisher.emit('publish', { topic: '/trades', type: 'TradeOrder', payload: { id: 'T1' } });
  await sleep(300);

  assert.strictEqual(received.length, 1);
  assert.strictEqual(received[0].topic, '/trades');
  assert.strictEqual(received[0].type, 'TradeOrder');
  assert.deepStrictEqual(received[0].payload, { id: 'T1' });

  [subscriber, attacker, publisher].forEach((s) => s.close());
});
