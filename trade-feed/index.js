const winston = require('winston');
const { loadConfig } = require('./auth');
const { createFeedServer } = require('./server');

const log = winston.createLogger({
  transports: [
    new winston.transports.Console()
  ]
});

let config;
try {
  config = loadConfig(process.env);
} catch (e) {
  log.error(`Invalid trade-feed configuration: ${e.message}`);
  process.exit(1);
}

if (config.allowAnonymousViewers) {
  log.warn('TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS=true: unauthenticated clients may read any /accounts/<id>/(trades|positions) topic');
}

const { http } = createFeedServer(config, log);

http.listen(config.port, () => {
  log.info(`Socket.IO server running at http://localhost:${config.port}/`);
});
