const sockio = require("socket.io");
const app = require('express')();
const winston = require('winston');
const http = require('http').createServer(app);

const io = new sockio.Server(http, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});
const port = process.env.TRADE_FEED_PORT || 18086;

const log = winston.createLogger({
  transports: [
    new winston.transports.Console()
  ]
});

// command names
const SUBSCRIBE = "subscribe";
const UNSUBSCRIBE = "unusbscribe";
const PUBLISH = "publish";

const MAX_TOPIC_LENGTH = 256;

function isValidTopic(topic) {
  return typeof topic === "string" && topic.length > 0 && topic.length <= MAX_TOPIC_LENGTH;
}

function isValidPublish(data) {
  return data !== null && typeof data === "object" && !Array.isArray(data) && isValidTopic(data.topic);
}

app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
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

function joinMessage(user,topic){
  return wrapMessage('System',topic,'message',{message: `New Joiner ${user} to topic ${topic}`});
}

function leaveMessage(user,topic){
  return wrapMessage('System',topic,'message',{message: `${user} has left ${topic}`});
}

function broadcast(from, data) {
  var message=wrapMessage(from,data.topic,data.type,data.payload);
  log.info(`Publish ${data.topic} -> ${JSON.stringify(message)}`);
  io.sockets.in([data.topic, "/*"]).emit(PUBLISH, message);
}

// A throwing listener would otherwise surface as an uncaught exception and
// take down the whole feed; drop the offending client instead.
function guarded(socket, command, handler) {
  return (...args) => {
    try {
      handler(...args);
    } catch (err) {
      log.error(`Error handling ${command} from ${socket.id}, disconnecting: ${err && err.stack || err}`);
      socket.disconnect(true);
    }
  };
}

io.on('connection', (socket) => {
  log.info(`New Connection from ${socket.id}`);
  socket.on(SUBSCRIBE, guarded(socket, SUBSCRIBE, (topic) => {
    if (!isValidTopic(topic)) {
      log.warn(`Ignoring ${SUBSCRIBE} with invalid topic from ${socket.id}`);
      return;
    }
    log.info(`Subscribe ${topic}`);
    socket.join(topic);
    broadcast('System', joinMessage(socket.id,topic));
  }));
  socket.on(UNSUBSCRIBE, guarded(socket, UNSUBSCRIBE, (topic) => {
    if (!isValidTopic(topic)) {
      log.warn(`Ignoring ${UNSUBSCRIBE} with invalid topic from ${socket.id}`);
      return;
    }
    log.info(`Unsubscribe ${topic}`);
    broadcast('System', leaveMessage(socket.id,topic));
    socket.leave(topic);
  }));
  socket.on(PUBLISH, guarded(socket, PUBLISH, (data) => {
    if (!isValidPublish(data)) {
      log.warn(`Ignoring malformed ${PUBLISH} from ${socket.id}`);
      return;
    }
    broadcast(socket.id, data);
  }));
});

process.on('uncaughtException', (err) => {
  log.error(`Uncaught exception: ${err && err.stack || err}`);
});


http.listen(port, () => {
  log.info(`Socket.IO server running at http://localhost:${port}/`);
});