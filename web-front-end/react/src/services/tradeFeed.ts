import { io, Socket } from 'socket.io-client';
import { environment } from '../config/environment';

// Singleton socket.io connection to the trade-feed message bus.
// Mirrors the Angular `TradeFeedService`: components subscribe to a topic
// and receive only the payloads published (by a non-System sender) to it.
class TradeFeed {
  private socket: Socket;

  constructor() {
    this.socket = io(environment.tradeFeedUrl);
    this.socket.on('connect', () =>
      console.log('Trade feed connected, id ' + this.socket.id)
    );
    this.socket.on('disconnect', () =>
      console.log('Trade feed disconnected, id was ' + this.socket.id)
    );
  }

  /**
   * Subscribe to a feed topic. The callback is invoked with the message
   * payload for every matching publish. Returns an unsubscribe function.
   */
  subscribe(topic: string, callback: (payload: any) => void): () => void {
    const handler = (args: any) => {
      if (args.from !== 'System' && args.topic === topic) {
        callback(args.payload);
      }
    };
    this.socket.on('publish', handler);
    this.socket.emit('subscribe', topic);
    return () => {
      this.socket.emit('unsubscribe', topic);
      this.socket.off('publish', handler);
    };
  }
}

export const tradeFeed = new TradeFeed();
