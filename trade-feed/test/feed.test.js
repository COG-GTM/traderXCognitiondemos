const test = require('node:test');
const assert = require('node:assert');
const crypto = require('node:crypto');
const { io } = require('socket.io-client');
const auth = require('../auth');
const { createFeedServer } = require('../server');

const TRADE_SERVICE_TOKEN = 'ts-' + 'a'.repeat(40);
const PROCESSOR_TOKEN = 'tp-' + 'b'.repeat(40);
const USER_SECRET = 'u'.repeat(48);
const silentLog = { info() {}, warn() {}, error() {} };

function userToken(claims, secret = USER_SECRET, alg = 'HS256') {
  const enc = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const head = enc({ alg, typ: 'JWT' });
  const body = enc(claims);
  const sig = crypto.createHmac('sha256', secret).update(`${head}.${body}`).digest('base64url');
  return `${head}.${body}.${sig}`;
}

function config(overrides = {}) {
  return auth.loadConfig({
    TRADE_FEED_SERVICE_TOKENS: `trade-service=${TRADE_SERVICE_TOKEN},trade-processor=${PROCESSOR_TOKEN}`,
    TRADE_FEED_USER_TOKEN_SECRET: USER_SECRET,
    TRADE_FEED_ALLOWED_ORIGINS: 'http://localhost:18093',
    ...overrides
  });
}

async function startFeed(cfg) {
  const feed = createFeedServer(cfg, silentLog);
  await new Promise((resolve) => feed.http.listen(0, '127.0.0.1', resolve));
  feed.url = `http://127.0.0.1:${feed.http.address().port}`;
  return feed;
}

function connect(url, opts = {}) {
  const socket = io(url, { transports: ['websocket'], reconnection: false, forceNew: true, ...opts });
  return new Promise((resolve, reject) => {
    socket.once('connect', () => resolve(socket));
    socket.once('connect_error', (err) => { socket.close(); reject(err); });
  });
}

function emitAck(socket, event, arg) {
  return new Promise((resolve) => socket.emit(event, arg, resolve));
}

function nextPublish(socket, predicate, ms = 300) {
  return new Promise((resolve) => {
    const timer = setTimeout(() => { socket.off('publish', handler); resolve(null); }, ms);
    function handler(msg) {
      if (!predicate || predicate(msg)) {
        clearTimeout(timer);
        socket.off('publish', handler);
        resolve(msg);
      }
    }
    socket.on('publish', handler);
  });
}

const order = { topic: '/trades', type: 'TradeOrder', payload: { accountId: 22214, security: 'IBM', side: 'Sell', quantity: 100 } };
const notSystem = (m) => m.from !== 'System';

test('config requires service tokens', () => {
  assert.throws(() => auth.loadConfig({}), /TRADE_FEED_SERVICE_TOKENS/);
  assert.throws(() => auth.loadConfig({ TRADE_FEED_SERVICE_TOKENS: 'trade-service=short' }), /at least 32/);
  assert.throws(() => auth.loadConfig({ TRADE_FEED_SERVICE_TOKENS: `evil=${'x'.repeat(40)}` }), /unknown service/);
});

test('user tokens: signature, alg, expiry and accounts are enforced', () => {
  const now = 1000;
  const good = { sub: 'alice', accounts: [22214], exp: now + 60 };
  assert.deepStrictEqual([...auth.verifyUserToken(userToken(good), USER_SECRET, now).accounts], [22214]);
  assert.strictEqual(auth.verifyUserToken(userToken(good, 'w'.repeat(48)), USER_SECRET, now), null);
  assert.strictEqual(auth.verifyUserToken(userToken(good, USER_SECRET, 'none'), USER_SECRET, now), null);
  assert.strictEqual(auth.verifyUserToken(userToken({ ...good, exp: now }), USER_SECRET, now), null);
  assert.strictEqual(auth.verifyUserToken(userToken({ ...good, accounts: ['*'] }), USER_SECRET, now), null);
  const [h, , s] = userToken(good).split('.');
  const forged = Buffer.from(JSON.stringify({ ...good, accounts: [1] })).toString('base64url');
  assert.strictEqual(auth.verifyUserToken(`${h}.${forged}.${s}`, USER_SECRET, now), null);
});

test('connections without valid credentials are refused', async () => {
  const feed = await startFeed(config());
  try {
    await assert.rejects(connect(feed.url), /unauthorized/);
    await assert.rejects(connect(feed.url, { auth: { token: 'wrong' } }), /unauthorized/);
  } finally {
    feed.io.close();
  }
});

test('disallowed browser origins are refused even with a valid token', async () => {
  const feed = await startFeed(config());
  try {
    await assert.rejects(connect(feed.url, { auth: { token: TRADE_SERVICE_TOKEN }, extraHeaders: { Origin: 'https://evil.example' } }));
    const ok = await connect(feed.url, { auth: { token: TRADE_SERVICE_TOKEN }, extraHeaders: { Origin: 'http://localhost:18093' } });
    ok.close();
  } finally {
    feed.io.close();
  }
});

test('only trade-service can publish TradeOrders, and the sender is stamped by the server', async () => {
  const feed = await startFeed(config({ TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS: 'true' }));
  const sockets = [];
  try {
    const processor = await connect(feed.url, { auth: { token: PROCESSOR_TOKEN } }); sockets.push(processor);
    const tradeService = await connect(feed.url, { auth: { token: TRADE_SERVICE_TOKEN } }); sockets.push(tradeService);
    const viewer = await connect(feed.url); sockets.push(viewer);
    const user = await connect(feed.url, { auth: { token: userToken({ sub: 'alice', accounts: [22214], exp: Math.floor(Date.now() / 1000) + 60 }) } }); sockets.push(user);
    assert.strictEqual((await emitAck(processor, 'subscribe', '/trades')).ok, true);

    for (const attacker of [viewer, user, processor]) {
      const got = nextPublish(processor, notSystem);
      assert.strictEqual((await emitAck(attacker, 'publish', order)).ok, false);
      assert.strictEqual(await got, null);
    }
    const wrongType = nextPublish(processor, notSystem);
    assert.strictEqual((await emitAck(tradeService, 'publish', { ...order, type: 'Trade' })).ok, false);
    assert.strictEqual(await wrongType, null);

    const got = nextPublish(processor, notSystem);
    assert.strictEqual((await emitAck(tradeService, 'publish', { ...order, from: 'spoofed' })).ok, true);
    const msg = await got;
    assert.strictEqual(msg.type, 'TradeOrder');
    assert.strictEqual(msg.from, 'trade-service');
    assert.deepStrictEqual(msg.payload, order.payload);
  } finally {
    sockets.forEach((s) => s.close());
    feed.io.close();
  }
});

test('clients cannot subscribe to the wildcard, /trades or other accounts', async () => {
  const feed = await startFeed(config({ TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS: 'true' }));
  const sockets = [];
  try {
    const processor = await connect(feed.url, { auth: { token: PROCESSOR_TOKEN } }); sockets.push(processor);
    const viewer = await connect(feed.url); sockets.push(viewer);
    const user = await connect(feed.url, { auth: { token: userToken({ sub: 'alice', accounts: [22214], exp: Math.floor(Date.now() / 1000) + 60 }) } }); sockets.push(user);

    for (const topic of ['/*', '*', '/trades', '/accounts/22214/*', '/accounts/x/trades', { $gt: '' }]) {
      assert.strictEqual((await emitAck(viewer, 'subscribe', topic)).ok, false, `viewer ${JSON.stringify(topic)}`);
      assert.strictEqual((await emitAck(user, 'subscribe', topic)).ok, false, `user ${JSON.stringify(topic)}`);
    }
    assert.strictEqual((await emitAck(user, 'subscribe', '/accounts/52355/trades')).ok, false);
    assert.strictEqual((await emitAck(user, 'subscribe', '/accounts/22214/trades')).ok, true);
    assert.strictEqual((await emitAck(viewer, 'subscribe', '/accounts/52355/positions')).ok, true);

    const userGot = nextPublish(user, notSystem);
    const viewerGot = nextPublish(viewer, notSystem);
    await emitAck(processor, 'publish', { topic: '/accounts/52355/positions', type: 'Position', payload: { accountId: 52355, security: 'IBM', quantity: 5 } });
    assert.strictEqual((await viewerGot).from, 'trade-processor');
    assert.strictEqual(await userGot, null);

    const userGot2 = nextPublish(user, notSystem);
    await emitAck(processor, 'publish', { topic: '/accounts/22214/trades', type: 'Trade', payload: { accountId: 22214 } });
    assert.strictEqual((await userGot2).topic, '/accounts/22214/trades');
  } finally {
    sockets.forEach((s) => s.close());
    feed.io.close();
  }
});

test('anonymous viewers are refused unless explicitly enabled', async () => {
  const feed = await startFeed(config());
  try {
    await assert.rejects(connect(feed.url), /unauthorized/);
  } finally {
    feed.io.close();
  }
});
