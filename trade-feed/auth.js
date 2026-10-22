const crypto = require('crypto');

const ACCOUNT_TOPIC = /^\/accounts\/([1-9][0-9]{0,9})\/(trades|positions)$/;

// Which topics and message types each service identity may publish.
const SERVICE_PUBLISH_POLICY = {
  'trade-service': [{ topic: /^\/trades$/, type: 'TradeOrder' }],
  'trade-processor': [
    { topic: /^\/accounts\/[1-9][0-9]{0,9}\/trades$/, type: 'Trade' },
    { topic: /^\/accounts\/[1-9][0-9]{0,9}\/positions$/, type: 'Position' }
  ]
};

// Topics each service identity may subscribe to (in addition to account topics).
const SERVICE_SUBSCRIBE_POLICY = {
  'trade-service': [],
  'trade-processor': [/^\/trades$/]
};

function splitList(value) {
  return (value || '').split(',').map((s) => s.trim()).filter(Boolean);
}

function parseServiceTokens(value) {
  const tokens = new Map();
  for (const entry of splitList(value)) {
    const idx = entry.indexOf('=');
    const name = idx > 0 ? entry.slice(0, idx).trim() : '';
    const token = idx > 0 ? entry.slice(idx + 1).trim() : '';
    if (!SERVICE_PUBLISH_POLICY[name]) {
      throw new Error(`TRADE_FEED_SERVICE_TOKENS: unknown service "${name || entry}"`);
    }
    if (token.length < 32) {
      throw new Error(`TRADE_FEED_SERVICE_TOKENS: token for "${name}" must be at least 32 characters`);
    }
    tokens.set(name, token);
  }
  return tokens;
}

function loadConfig(env) {
  const serviceTokens = parseServiceTokens(env.TRADE_FEED_SERVICE_TOKENS);
  if (serviceTokens.size === 0) {
    throw new Error('TRADE_FEED_SERVICE_TOKENS must be set (e.g. "trade-service=<token>,trade-processor=<token>")');
  }
  const userTokenSecret = env.TRADE_FEED_USER_TOKEN_SECRET || '';
  if (userTokenSecret && userTokenSecret.length < 32) {
    throw new Error('TRADE_FEED_USER_TOKEN_SECRET must be at least 32 characters');
  }
  return {
    port: env.TRADE_FEED_PORT || 18086,
    allowedOrigins: splitList(env.TRADE_FEED_ALLOWED_ORIGINS || 'http://localhost:18086,http://localhost:18093,http://localhost:18094,http://localhost:8080'),
    serviceTokens,
    userTokenSecret,
    userTokenAudience: env.TRADE_FEED_USER_TOKEN_AUDIENCE || 'trade-feed',
    allowAnonymousViewers: env.TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS === 'true'
  };
}

function safeEqual(a, b) {
  const ab = Buffer.from(String(a));
  const bb = Buffer.from(String(b));
  return ab.length === bb.length && crypto.timingSafeEqual(ab, bb);
}

function base64urlJson(part) {
  return JSON.parse(Buffer.from(part, 'base64url').toString('utf8'));
}

// Verifies an HS256 JWT carrying the account ids the user may view:
// { "sub": "<user>", "aud": "trade-feed", "accounts": [22214, 22903], "exp": <unix seconds> }
function verifyUserToken(token, secret, nowSeconds, audience = 'trade-feed') {
  if (!secret || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  let header;
  let claims;
  try {
    header = base64urlJson(parts[0]);
    claims = base64urlJson(parts[1]);
  } catch (e) {
    return null;
  }
  if (!header || header.alg !== 'HS256') return null;
  const expected = crypto.createHmac('sha256', secret).update(`${parts[0]}.${parts[1]}`).digest('base64url');
  if (!safeEqual(expected, parts[2])) return null;
  if (!claims || typeof claims.sub !== 'string' || !claims.sub) return null;
  if (typeof claims.exp !== 'number' || claims.exp <= nowSeconds) return null;
  const aud = Array.isArray(claims.aud) ? claims.aud : [claims.aud];
  if (!aud.includes(audience)) return null;
  if (!Array.isArray(claims.accounts) || !claims.accounts.every((a) => Number.isInteger(a) && a > 0)) return null;
  return { kind: 'user', name: claims.sub, accounts: new Set(claims.accounts), expiresAt: claims.exp };
}

// Resolves the handshake credentials to an identity, or null when the connection must be refused.
function authenticate(handshakeAuth, config, nowSeconds) {
  const token = handshakeAuth && typeof handshakeAuth.token === 'string' ? handshakeAuth.token : '';
  if (token) {
    for (const [name, serviceToken] of config.serviceTokens) {
      if (safeEqual(token, serviceToken)) return { kind: 'service', name };
    }
    return verifyUserToken(token, config.userTokenSecret, nowSeconds, config.userTokenAudience);
  }
  if (config.allowAnonymousViewers) return { kind: 'viewer', name: 'anonymous' };
  return null;
}

function canSubscribe(identity, topic) {
  if (typeof topic !== 'string') return false;
  const account = ACCOUNT_TOPIC.exec(topic);
  if (identity.kind === 'service') {
    return Boolean(account) || (SERVICE_SUBSCRIBE_POLICY[identity.name] || []).some((re) => re.test(topic));
  }
  if (!account) return false;
  if (identity.kind === 'user') return identity.accounts.has(Number(account[1]));
  return identity.kind === 'viewer';
}

function canPublish(identity, data) {
  if (identity.kind !== 'service') return false;
  if (!data || typeof data !== 'object' || typeof data.topic !== 'string' || typeof data.type !== 'string') return false;
  if (!data.payload || typeof data.payload !== 'object') return false;
  return (SERVICE_PUBLISH_POLICY[identity.name] || []).some((rule) => rule.topic.test(data.topic) && rule.type === data.type);
}

function isOriginAllowed(origin, allowedOrigins) {
  // Non-browser clients (the Java services) send no Origin header; they are gated by token auth.
  if (!origin) return true;
  return allowedOrigins.includes(origin);
}

module.exports = {
  loadConfig,
  parseServiceTokens,
  verifyUserToken,
  authenticate,
  canSubscribe,
  canPublish,
  isOriginAllowed
};
