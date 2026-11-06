const sockio = require("socket.io");
const express = require('express');
const winston = require('winston');
const http = require('http');
const { loadConfig, authenticate, canSubscribe, canPublish, isOriginAllowed } = require('./auth');

const log = winston.createLogger({
  transports: [
    new winston.transports.Console()
  ]
});

// command names
const SUBSCRIBE = "subscribe";
const UNSUBSCRIBE = "unsubscribe";
const PUBLISH = "publish";

function wrapMessage( sender, topic, payloadType, payload){
  return {
    type: payloadType || "message",
    from: sender,
    topic: topic,
    date: new Date().getTime(),
    payload: payload
  }
}

function joinMessage(user,topic){
  return wrapMessage('System',topic,'message',{message: `New Joiner ${user} to topic ${topic}`});
}

function leaveMessage(user,topic){
  return wrapMessage('System',topic,'message',{message: `${user} has left ${topic}`});
}

function ack(callback, ok, message) {
  if (typeof callback === 'function') callback({ ok, message });
}

function createServer(config) {
  const app = express();
  const server = http.createServer(app);

  const io = new sockio.Server(server, {
    cors: {
      origin: config.allowedOrigins,
      methods: ["GET", "POST"]
    },
    // CORS headers do not stop WebSocket upgrades, so check Origin on every request.
    allowRequest: (req, callback) => callback(null, isOriginAllowed(req.headers.origin, config.allowedOrigins))
  });

  app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
  });

  function broadcast(from, data) {
    const message = wrapMessage(from, data.topic, data.type, data.payload);
    log.info(`Publish ${data.topic} -> ${JSON.stringify(message)}`);
    io.to(data.topic).emit(PUBLISH, message);
  }

  io.use((socket, next) => {
    const identity = authenticate(socket.handshake.auth, config);
    if (!identity) {
      log.warn(`Rejected unauthenticated connection from ${socket.handshake.address}`);
      return next(new Error('unauthorized'));
    }
    socket.data.identity = identity;
    next();
  });

  io.on('connection', (socket) => {
    const identity = socket.data.identity;
    log.info(`New Connection from ${socket.id} as ${identity.kind}:${identity.name}`);
    socket.on(SUBSCRIBE, (topic, callback) => {
      if (!canSubscribe(identity, topic)) {
        log.warn(`Denied subscribe ${topic} for ${identity.name} (${socket.id})`);
        return ack(callback, false, 'forbidden');
      }
      log.info(`Subscribe ${topic}`);
      socket.join(topic);
      broadcast('System', joinMessage(socket.id, topic));
      ack(callback, true);
    });
    socket.on(UNSUBSCRIBE, (topic, callback) => {
      if (typeof topic !== 'string' || !socket.rooms.has(topic)) return ack(callback, false, 'not subscribed');
      log.info(`Unsubscribe ${topic}`);
      broadcast('System', leaveMessage(socket.id, topic));
      socket.leave(topic);
      ack(callback, true);
    });
    socket.on(PUBLISH, (data, callback) => {
      if (!canPublish(identity, data)) {
        log.warn(`Denied publish to ${data && data.topic} for ${identity.name} (${socket.id})`);
        return ack(callback, false, 'forbidden');
      }
      // The sender is the authenticated identity, never a client-supplied value.
      broadcast(identity.name, data);
      ack(callback, true);
    });
  });

  return { app, server, io };
}

if (require.main === module) {
  const config = loadConfig(process.env);
  const { server } = createServer(config);
  server.listen(config.port, () => {
    log.info(`Socket.IO server running at http://localhost:${config.port}/`);
  });
}

module.exports = { createServer };
