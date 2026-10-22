const sockio = require("socket.io");
const express = require('express');
const winston = require('winston');
const { authenticate, canSubscribe, canPublish, isOriginAllowed } = require('./auth');

// command names
const SUBSCRIBE = "subscribe";
const UNSUBSCRIBE = "unsubscribe";
const MAX_TIMER_MS = 2147483647;
const PUBLISH = "publish";

const defaultLog = winston.createLogger({
  transports: [
    new winston.transports.Console()
  ]
});

function wrapMessage( sender, topic, payloadType, payload){
  return {
    type: payloadType || "message",
    from: sender,
    topic: topic,
    date: new Date().getTime(),
    payload: payload
  }
}

function systemMessage(topic, text) {
  return wrapMessage('System', topic, 'message', { message: text });
}

function reply(socket, ack, ok, message) {
  if (typeof ack === 'function') {
    ack({ ok: ok, message: message.payload.message });
  }
  socket.emit(PUBLISH, message);
}

function createFeedServer(config, log) {
  log = log || defaultLog;
  const app = express();
  const http = require('http').createServer(app);

  const io = new sockio.Server(http, {
    cors: {
      origin: config.allowedOrigins,
      methods: ["GET", "POST"]
    },
    // CORS headers do not cover WebSocket upgrades, so check the Origin for every transport.
    allowRequest: (req, callback) => {
      callback(null, isOriginAllowed(req.headers.origin, config.allowedOrigins));
    }
  });

  app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
  });

  io.use((socket, next) => {
    const identity = authenticate(socket.handshake.auth, config, Math.floor(Date.now() / 1000));
    if (!identity) {
      log.warn(`Rejected unauthenticated connection ${socket.id}`);
      return next(new Error('unauthorized'));
    }
    socket.data.identity = identity;
    next();
  });

  function broadcast(identity, data) {
    // The sender is stamped from the authenticated identity, never taken from the client.
    const message = wrapMessage(identity.name, data.topic, data.type, data.payload);
    log.info(`Publish ${data.topic} type=${data.type} from=${identity.name}`);
    io.to(data.topic).emit(PUBLISH, message);
  }

  io.on('connection', (socket) => {
    const identity = socket.data.identity;
    log.info(`New Connection from ${socket.id} as ${identity.kind}:${identity.name}`);
    if (identity.expiresAt) {
      // Drop the socket when the user token expires; the client must reconnect with a fresh token.
      const msLeft = Math.min(identity.expiresAt * 1000 - Date.now(), MAX_TIMER_MS);
      const expiry = setTimeout(() => {
        log.info(`Token expired for ${identity.kind}:${identity.name}, disconnecting ${socket.id}`);
        socket.disconnect(true);
      }, Math.max(msLeft, 0));
      socket.on('disconnect', () => clearTimeout(expiry));
    }
    socket.on(SUBSCRIBE, (topic, ack) => {
      if (!canSubscribe(identity, topic)) {
        log.warn(`Denied subscribe ${JSON.stringify(topic)} for ${identity.kind}:${identity.name}`);
        return reply(socket, ack, false, systemMessage(topic, `Subscription to ${topic} denied`));
      }
      log.info(`Subscribe ${topic} for ${identity.kind}:${identity.name}`);
      socket.join(topic);
      reply(socket, ack, true, systemMessage(topic, `Subscribed to ${topic}`));
    });
    socket.on(UNSUBSCRIBE, (topic, ack) => {
      if (typeof topic !== 'string') return;
      log.info(`Unsubscribe ${topic} for ${identity.kind}:${identity.name}`);
      socket.leave(topic);
      reply(socket, ack, true, systemMessage(topic, `Unsubscribed from ${topic}`));
    });
    socket.on(PUBLISH, (data, ack) => {
      if (!canPublish(identity, data)) {
        const topic = data && typeof data.topic === 'string' ? data.topic : undefined;
        log.warn(`Denied publish to ${JSON.stringify(topic)} for ${identity.kind}:${identity.name}`);
        return reply(socket, ack, false, systemMessage(topic, `Publish to ${topic} denied`));
      }
      broadcast(identity, data);
      if (typeof ack === 'function') ack({ ok: true });
    });
  });

  return { app, http, io };
}

module.exports = { createFeedServer, wrapMessage };
