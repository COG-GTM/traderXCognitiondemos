const test = require('node:test');
const assert = require('node:assert');
const { io: connect } = require('socket.io-client');
const { createServer } = require('../index');
const { loadConfig } = require('../auth');
const { randomBytes } = require('node:crypto');

const TOO_SHORT = 'short';

const TRADE_SERVICE_TOKEN = randomBytes(32).toString('hex');
const TRADE_PROCESSOR_TOKEN = randomBytes(32).toString('hex');
const ALLOWED_ORIGIN = 'http://localhost:18093';

const config = loadConfig({
  TRADE_FEED_TRADE_SERVICE_TOKEN: TRADE_SERVICE_TOKEN,
  TRADE_FEED_TRADE_PROCESSOR_TOKEN: TRADE_PROCESSOR_TOKEN,
  TRADE_FEED_ALLOWED_ORIGINS: ALLOWED_ORIGIN,
  TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS: 'true'
});

const order = { topic: '/trades', type: 'TradeOrder', payload: { accountId: 22214, security: 'MSFT', side: 'Sell', quantity: 1000000 } };

let server;
let url;
const sockets = [];

test.before(async () => {
  ({ server } = createServer(config));
  await new Promise((resolve) => server.listen(0, resolve));
  url = `http://localhost:${server.address().port}`;
});

test.after(() => {
  sockets.forEach((s) => s.close());
  server.close();
});

function client(opts = {}) {
  const s = connect(url, { transports: ['websocket'], forceNew: true, reconnection: false, ...opts });
  sockets.push(s);
  return s;
}

function connected(s) {
  return new Promise((resolve, reject) => {
    s.once('connect', resolve);
    s.once('connect_error', reject);
  });
}

function emitAck(s, event, arg) {
  return new Promise((resolve) => s.emit(event, arg, resolve));
}

function collect(s) {
  const messages = [];
  s.on('publish', (m) => { if (m.from !== 'System') messages.push(m); });
  return messages;
}

const settle = () => new Promise((r) => setTimeout(r, 200));

test('loadConfig refuses to start without strong, distinct service tokens', () => {
  assert.throws(() => loadConfig({}), /TRADE_FEED_TRADE_SERVICE_TOKEN/);
  assert.throws(() => loadConfig({ TRADE_FEED_TRADE_SERVICE_TOKEN: TOO_SHORT, TRADE_FEED_TRADE_PROCESSOR_TOKEN: TRADE_PROCESSOR_TOKEN }), /at least 32/);
  assert.throws(() => loadConfig({ TRADE_FEED_TRADE_SERVICE_TOKEN: TRADE_SERVICE_TOKEN, TRADE_FEED_TRADE_PROCESSOR_TOKEN: TRADE_SERVICE_TOKEN }), /must differ/);
});

test('anonymous client cannot publish a forged TradeOrder on /trades', async () => {
  const processor = client({ auth: { token: TRADE_PROCESSOR_TOKEN } });
  await connected(processor);
  const received = collect(processor);
  assert.deepStrictEqual(await emitAck(processor, 'subscribe', '/trades'), { ok: true });

  const attacker = client();
  await connected(attacker);
  assert.deepStrictEqual(await emitAck(attacker, 'publish', order), { ok: false, message: 'forbidden' });
  await settle();
  assert.strictEqual(received.length, 0);
});

test('anonymous client cannot subscribe to /* or /trades', async () => {
  const attacker = client();
  await connected(attacker);
  assert.strictEqual((await emitAck(attacker, 'subscribe', '/*')).ok, false);
  assert.strictEqual((await emitAck(attacker, 'subscribe', '/trades')).ok, false);
  const received = collect(attacker);

  const tradeService = client({ auth: { token: TRADE_SERVICE_TOKEN } });
  await connected(tradeService);
  assert.strictEqual((await emitAck(tradeService, 'publish', order)).ok, true);
  await settle();
  assert.strictEqual(received.length, 0);
});

test('anonymous client cannot spoof position events', async () => {
  const attacker = client();
  await connected(attacker);
  const res = await emitAck(attacker, 'publish', { topic: '/accounts/22214/positions', type: 'Position', payload: { quantity: -1 } });
  assert.strictEqual(res.ok, false);
});

test('wrong token and disallowed browser origin are refused at handshake', async () => {
  await assert.rejects(connected(client({ auth: { token: randomBytes(32).toString('hex') } })), /unauthorized/);
  await assert.rejects(connected(client({ transports: ['polling'], extraHeaders: { Origin: 'https://evil.example' } })));
});

test('trade-service can publish orders and trade-processor receives them with a trusted sender', async () => {
  const processor = client({ auth: { token: TRADE_PROCESSOR_TOKEN } });
  await connected(processor);
  const received = collect(processor);
  await emitAck(processor, 'subscribe', '/trades');

  const tradeService = client({ auth: { token: TRADE_SERVICE_TOKEN }, extraHeaders: { Origin: ALLOWED_ORIGIN } });
  await connected(tradeService);
  assert.strictEqual((await emitAck(tradeService, 'publish', { ...order, from: 'spoofed' })).ok, true);
  await settle();
  assert.strictEqual(received.length, 1);
  assert.strictEqual(received[0].from, 'trade-service');
  assert.strictEqual(received[0].type, 'TradeOrder');
});

test('trade-service cannot publish positions; trade-processor cannot publish orders', async () => {
  const tradeService = client({ auth: { token: TRADE_SERVICE_TOKEN } });
  const processor = client({ auth: { token: TRADE_PROCESSOR_TOKEN } });
  await Promise.all([connected(tradeService), connected(processor)]);
  assert.strictEqual((await emitAck(tradeService, 'publish', { topic: '/accounts/1/positions', type: 'Position', payload: {} })).ok, false);
  assert.strictEqual((await emitAck(processor, 'publish', order)).ok, false);
});

test('UI viewer from an allowed origin still receives account updates', async () => {
  const viewer = client({ extraHeaders: { Origin: ALLOWED_ORIGIN }, transports: ['polling'] });
  await connected(viewer);
  const received = collect(viewer);
  assert.strictEqual((await emitAck(viewer, 'subscribe', '/accounts/22214/trades')).ok, true);

  const processor = client({ auth: { token: TRADE_PROCESSOR_TOKEN } });
  await connected(processor);
  await emitAck(processor, 'publish', { topic: '/accounts/22214/trades', type: 'Trade', payload: { id: 't1' } });
  await settle();
  assert.strictEqual(received.length, 1);
  assert.strictEqual(received[0].from, 'trade-processor');
});
