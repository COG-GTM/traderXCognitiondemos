const crypto = require('crypto');

const ACCOUNT_ID = '[1-9][0-9]{0,9}';
const ACCOUNT_TOPIC = new RegExp(`^/accounts/${ACCOUNT_ID}/(trades|positions)$`);

// Service identities, the env var holding each one's token, and what each may publish/subscribe.
const SERVICES = {
  'trade-service': {
    tokenEnv: 'TRADE_FEED_TRADE_SERVICE_TOKEN',
    publish: [{ topic: /^\/trades$/, type: 'TradeOrder' }],
    subscribe: []
  },
  'trade-processor': {
    tokenEnv: 'TRADE_FEED_TRADE_PROCESSOR_TOKEN',
    publish: [
      { topic: new RegExp(`^/accounts/${ACCOUNT_ID}/trades$`), type: 'Trade' },
      { topic: new RegExp(`^/accounts/${ACCOUNT_ID}/positions$`), type: 'Position' }
    ],
    subscribe: [/^\/trades$/]
  }
};

const MIN_TOKEN_LENGTH = 32;
const DEFAULT_ALLOWED_ORIGINS = 'http://localhost:8080,http://localhost:18093,http://localhost:18094,http://localhost:18086';

function splitList(value) {
  return (value || '').split(',').map((s) => s.trim()).filter(Boolean);
}

function loadConfig(env) {
  const serviceTokens = new Map();
  for (const [name, service] of Object.entries(SERVICES)) {
    const token = env[service.tokenEnv] || '';
    if (token.length < MIN_TOKEN_LENGTH) {
      throw new Error(`${service.tokenEnv} must be set to a secret of at least ${MIN_TOKEN_LENGTH} characters (e.g. openssl rand -hex 32)`);
    }
    serviceTokens.set(name, token);
  }
  if (serviceTokens.get('trade-service') === serviceTokens.get('trade-processor')) {
    throw new Error('TRADE_FEED_TRADE_SERVICE_TOKEN and TRADE_FEED_TRADE_PROCESSOR_TOKEN must differ');
  }
  return {
    port: env.TRADE_FEED_PORT || 18086,
    serviceTokens,
    allowedOrigins: splitList(env.TRADE_FEED_ALLOWED_ORIGINS || DEFAULT_ALLOWED_ORIGINS),
    allowAnonymousViewers: env.TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS === 'true'
  };
}

function safeEqual(a, b) {
  const ab = Buffer.from(String(a));
  const bb = Buffer.from(String(b));
  return ab.length === bb.length && crypto.timingSafeEqual(ab, bb);
}

// Returns the identity for the handshake credentials, or null when the connection must be refused.
function authenticate(handshakeAuth, config) {
  const token = handshakeAuth && typeof handshakeAuth.token === 'string' ? handshakeAuth.token : '';
  if (token) {
    for (const [name, serviceToken] of config.serviceTokens) {
      if (safeEqual(token, serviceToken)) return { kind: 'service', name };
    }
    return null;
  }
  return config.allowAnonymousViewers ? { kind: 'viewer', name: 'anonymous' } : null;
}

function canSubscribe(identity, topic) {
  if (typeof topic !== 'string') return false;
  if (identity.kind === 'viewer') return ACCOUNT_TOPIC.test(topic);
  return identity.kind === 'service' && SERVICES[identity.name].subscribe.some((re) => re.test(topic));
}

function canPublish(identity, data) {
  if (identity.kind !== 'service') return false;
  if (!data || typeof data !== 'object' || typeof data.topic !== 'string' || typeof data.type !== 'string') return false;
  if (!data.payload || typeof data.payload !== 'object') return false;
  return SERVICES[identity.name].publish.some((rule) => rule.topic.test(data.topic) && rule.type === data.type);
}

// Browsers always send Origin; the Java services send none and are gated by their token instead.
function isOriginAllowed(origin, allowedOrigins) {
  return !origin || allowedOrigins.includes(origin);
}

module.exports = { loadConfig, authenticate, canSubscribe, canPublish, isOriginAllowed };
