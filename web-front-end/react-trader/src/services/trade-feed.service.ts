import { io } from 'socket.io-client';
import type { Socket } from 'socket.io-client';
import { environment } from '../environments/environment';

interface PublishMessage {
  from: string;
  topic: string;
  payload: unknown;
}

export class TradeFeedService {
  private socket: Socket;

  constructor(url: string = environment.tradeFeedUrl) {
    this.socket = io(url);
    this.socket.on('connect', this.onConnect);
    this.socket.on('disconnect', this.onDisconnect);
  }

  private onConnect = () => {
    console.log('Trade feed is connected, connection id' + this.socket.id);
  };

  private onDisconnect = () => {
    console.log('Trade feed is disconnected, connection id was ' + this.socket.id);
  };

  public subscribe(topic: string, callback: (...args: any[]) => void) {
    const callbackFn = (args: PublishMessage) => {
      if (args.from !== 'System' && args.topic === topic) {
        callback(args.payload);
      }
    };
    this.socket.on('publish', callbackFn);
    this.socket.emit('subscribe', topic);
    return () => {
      this.unSubscribe(topic, callbackFn);
    };
  }

  public unSubscribe(topic: string, callback: (...args: any[]) => void) {
    this.socket.emit('unsubscribe', topic);
    this.socket.off('publish', callback);
  }
}

let instance: TradeFeedService | undefined;

export function getTradeFeedService(): TradeFeedService {
  if (!instance) {
    instance = new TradeFeedService();
  }
  return instance;
}

export function subscribe(topic: string, callback: (...args: any[]) => void): () => void {
  return getTradeFeedService().subscribe(topic, callback);
}

export function unSubscribe(topic: string, callback: (...args: any[]) => void): void {
  getTradeFeedService().unSubscribe(topic, callback);
}
