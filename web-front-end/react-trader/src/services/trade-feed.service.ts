import { io, Socket } from 'socket.io-client';
import { environment } from '../environments/environment';

interface FeedMessage {
  from: string;
  topic: string;
  payload: unknown;
}

let socket: Socket | undefined;

function getSocket(): Socket {
  if (!socket) {
    socket = io(environment.tradeFeedUrl);
    socket.on('connect', () => {
      console.log('Trade feed is connected, connection id ' + socket?.id);
    });
    socket.on('disconnect', () => {
      console.log('Trade feed is disconnected, connection id was ' + socket?.id);
    });
  }
  return socket;
}

export function subscribe(topic: string, callback: (payload: never) => void): () => void {
  const feed = getSocket();
  const callbackFn = (args: FeedMessage) => {
    if (args.from !== 'System' && args.topic === topic) {
      callback(args.payload as never);
    }
  };
  feed.on('publish', callbackFn);
  feed.emit('subscribe', topic);
  return () => {
    unSubscribe(topic, callbackFn);
  };
}

export function unSubscribe(topic: string, callback: (args: FeedMessage) => void) {
  const feed = getSocket();
  feed.emit('unsubscribe', topic);
  feed.off('publish', callback);
}
