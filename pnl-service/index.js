const express = require('express');
const cors = require('cors');
const axios = require('axios');
const { io } = require('socket.io-client');
const winston = require('winston');

// Configuration from environment variables
const TRADE_FEED_HOST = process.env.TRADE_FEED_HOST || 'localhost';
const POSITION_SERVICE_HOST = process.env.POSITION_SERVICE_HOST || 'localhost';
const REFERENCE_DATA_HOST = process.env.REFERENCE_DATA_HOST || 'localhost';
const PNL_SERVICE_PORT = parseInt(process.env.PNL_SERVICE_PORT, 10) || 18095;

const POSITION_SERVICE_URL = `http://${POSITION_SERVICE_HOST}:18090`;
const REFERENCE_DATA_URL = `http://${REFERENCE_DATA_HOST}:18085`;
const TRADE_FEED_URL = `http://${TRADE_FEED_HOST}:18086`;

const log = winston.createLogger({
  transports: [new winston.transports.Console()],
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.printf(({ timestamp, level, message }) => `${timestamp} [${level}]: ${message}`)
  ),
});

// ── In-memory PnL state ──────────────────────────────────────────────
// Key: `${accountId}:${security}`
const pnlMap = new Map();

function pnlKey(accountId, security) {
  return `${accountId}:${security}`;
}

function getOrCreateEntry(accountId, security) {
  const key = pnlKey(accountId, security);
  if (!pnlMap.has(key)) {
    pnlMap.set(key, {
      accountId: Number(accountId),
      security: security,
      netQuantity: 0,
      avgCostBasis: 0,
      realizedPnL: 0,
      currentPrice: 0,
      marketValue: 0,
      unrealizedPnL: 0,
      totalPnL: 0,
      lastUpdated: new Date().toISOString(),
    });
  }
  return pnlMap.get(key);
}

function computeDerived(entry) {
  entry.marketValue = Math.round(entry.currentPrice * entry.netQuantity * 100) / 100;
  entry.unrealizedPnL = Math.round((entry.currentPrice - entry.avgCostBasis) * entry.netQuantity * 100) / 100;
  entry.totalPnL = Math.round((entry.realizedPnL + entry.unrealizedPnL) * 100) / 100;
  entry.lastUpdated = new Date().toISOString();
}

// ── Price fetching ───────────────────────────────────────────────────
async function fetchPrice(ticker) {
  try {
    const res = await axios.get(`${REFERENCE_DATA_URL}/prices/${ticker}`);
    return res.data.price;
  } catch (err) {
    log.warn(`Failed to fetch price for ${ticker}: ${err.message}`);
    return null;
  }
}

// ── Bootstrap ────────────────────────────────────────────────────────
async function bootstrap() {
  log.info('Starting PnL service bootstrap...');

  // 1. Fetch all positions
  let positions = [];
  try {
    const posRes = await axios.get(`${POSITION_SERVICE_URL}/positions/`);
    positions = posRes.data || [];
    log.info(`Fetched ${positions.length} positions`);
  } catch (err) {
    log.error(`Failed to fetch positions: ${err.message}`);
  }

  // 2. Fetch all trades
  let trades = [];
  try {
    const tradeRes = await axios.get(`${POSITION_SERVICE_URL}/trades/`);
    trades = tradeRes.data || [];
    log.info(`Fetched ${trades.length} trades`);
  } catch (err) {
    log.error(`Failed to fetch trades: ${err.message}`);
  }

  // 3. Filter to only Settled trades and sort by created ascending
  const settledTrades = trades
    .filter((t) => t.state === 'Settled')
    .sort((a, b) => new Date(a.created).getTime() - new Date(b.created).getTime());

  log.info(`Processing ${settledTrades.length} settled trades`);

  // Initialize position quantities from position-service data
  for (const pos of positions) {
    const entry = getOrCreateEntry(pos.accountId || pos.accountid, pos.security);
    entry.netQuantity = pos.quantity || 0;
  }

  // 4-6. Process settled trades to compute cost basis and realized P&L
  // We track running long quantity separately for ACB calculation
  const runningState = new Map(); // key -> { longQty, acb }

  for (const trade of settledTrades) {
    const accountId = trade.accountId || trade.accountid;
    const security = trade.security;
    const key = pnlKey(accountId, security);
    const entry = getOrCreateEntry(accountId, security);

    if (!runningState.has(key)) {
      runningState.set(key, { longQty: 0, acb: 0 });
    }
    const state = runningState.get(key);

    const side = trade.side;
    const qty = trade.quantity || 0;
    const price = trade.price || 0;

    if (side === 'Buy') {
      // Update weighted average cost basis
      if (state.longQty + qty > 0) {
        state.acb = (state.acb * state.longQty + price * qty) / (state.longQty + qty);
      }
      state.longQty += qty;
    } else if (side === 'Sell') {
      // Compute realized P&L for this sell
      entry.realizedPnL += (price - state.acb) * qty;
      entry.realizedPnL = Math.round(entry.realizedPnL * 100) / 100;
      state.longQty -= qty;
    }

    entry.avgCostBasis = Math.round(state.acb * 100) / 100;
  }

  // 7. Fetch current prices for all held securities
  const securities = new Set();
  for (const entry of pnlMap.values()) {
    securities.add(entry.security);
  }

  for (const ticker of securities) {
    const price = await fetchPrice(ticker);
    if (price !== null) {
      // Update all entries with this security
      for (const entry of pnlMap.values()) {
        if (entry.security === ticker) {
          entry.currentPrice = price;
          computeDerived(entry);
        }
      }
    }
  }

  log.info(`Bootstrap complete. ${pnlMap.size} P&L entries initialized.`);
}

// ── Trade Feed Connection ────────────────────────────────────────────
let socket = null;
const subscribedAccounts = new Set();

function connectToTradeFeed() {
  log.info(`Connecting to trade-feed at ${TRADE_FEED_URL}`);
  socket = io(TRADE_FEED_URL);

  socket.on('connect', () => {
    log.info(`Connected to trade-feed, socket id: ${socket.id}`);

    // Subscribe to all known accounts
    for (const entry of pnlMap.values()) {
      subscribeToAccount(entry.accountId);
    }

    // Also subscribe to wildcard for new accounts
    socket.emit('subscribe', '/accounts/*/positions');
  });

  socket.on('disconnect', () => {
    log.warn('Disconnected from trade-feed');
  });

  socket.on('publish', async (message) => {
    if (message.from === 'System') return;
    const topic = message.topic;
    const data = message.payload;

    // Match /accounts/{accountId}/positions
    const posMatch = topic.match(/^\/accounts\/(\d+)\/positions$/);
    if (posMatch && data) {
      const accountId = parseInt(posMatch[1], 10);
      await handlePositionUpdate(accountId, data);
    }
  });
}

function subscribeToAccount(accountId) {
  if (!subscribedAccounts.has(accountId) && socket && socket.connected) {
    socket.emit('subscribe', `/accounts/${accountId}/positions`);
    subscribedAccounts.add(accountId);
    log.info(`Subscribed to /accounts/${accountId}/positions`);
  }
}

async function handlePositionUpdate(accountId, data) {
  const security = data.security;
  if (!security) return;

  const entry = getOrCreateEntry(accountId, security);
  entry.netQuantity = data.quantity || 0;

  // Re-fetch the current price
  const price = await fetchPrice(security);
  if (price !== null) {
    entry.currentPrice = price;
  }

  computeDerived(entry);

  // Publish updated PnL to trade-feed
  if (socket && socket.connected) {
    socket.emit('publish', {
      topic: `/accounts/${accountId}/pnl`,
      type: 'pnl-update',
      payload: entry,
    });
    log.info(`Published P&L update for account ${accountId}, security ${security}`);
  }

  // Subscribe to this account if new
  subscribeToAccount(accountId);
}

// ── REST API ─────────────────────────────────────────────────────────
const app = express();
app.use(cors());
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ state: 'HEALTHY' });
});

// Get all account P&L summaries
app.get('/pnl/', (req, res) => {
  const accountMap = new Map();

  for (const entry of pnlMap.values()) {
    if (!accountMap.has(entry.accountId)) {
      accountMap.set(entry.accountId, {
        accountId: entry.accountId,
        totalMarketValue: 0,
        totalRealizedPnL: 0,
        totalUnrealizedPnL: 0,
        totalPnL: 0,
      });
    }
    const summary = accountMap.get(entry.accountId);
    summary.totalMarketValue = Math.round((summary.totalMarketValue + entry.marketValue) * 100) / 100;
    summary.totalRealizedPnL = Math.round((summary.totalRealizedPnL + entry.realizedPnL) * 100) / 100;
    summary.totalUnrealizedPnL = Math.round((summary.totalUnrealizedPnL + entry.unrealizedPnL) * 100) / 100;
    summary.totalPnL = Math.round((summary.totalPnL + entry.totalPnL) * 100) / 100;
  }

  res.json(Array.from(accountMap.values()));
});

// Get P&L for a specific account
app.get('/pnl/:accountId', (req, res) => {
  const accountId = parseInt(req.params.accountId, 10);
  const entries = [];
  for (const entry of pnlMap.values()) {
    if (entry.accountId === accountId) {
      entries.push(entry);
    }
  }
  res.json(entries);
});

// Get P&L for a specific account and ticker
app.get('/pnl/:accountId/:ticker', (req, res) => {
  const accountId = parseInt(req.params.accountId, 10);
  const ticker = req.params.ticker;
  const key = pnlKey(accountId, ticker);
  const entry = pnlMap.get(key);
  if (!entry) {
    return res.status(404).json({ error: `No P&L entry for account ${accountId}, ticker ${ticker}` });
  }
  res.json(entry);
});

// ── Startup ──────────────────────────────────────────────────────────
async function start() {
  try {
    await bootstrap();
  } catch (err) {
    log.error(`Bootstrap failed: ${err.message}. Starting with empty state.`);
  }

  connectToTradeFeed();

  app.listen(PNL_SERVICE_PORT, '0.0.0.0', () => {
    log.info(`PnL service running at http://localhost:${PNL_SERVICE_PORT}/`);
  });
}

start();
