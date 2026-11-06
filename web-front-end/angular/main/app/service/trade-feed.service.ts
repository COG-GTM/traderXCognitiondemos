import { Injectable } from '@angular/core';
import { environment } from 'main/environments/environment';
import { io, Socket } from "socket.io-client";

@Injectable({
    providedIn: 'root'
})
export class TradeFeedService {
    private socket: Socket;
    private readonly topicRefCounts = new Map<string, number>();

    constructor() {
        this.connect();
    }

    private connect() {
        // create socketio client with long polling only
        this.socket = io(environment.tradeFeedUrl);
        
        this.socket.on("connect", this.onConnect);
        this.socket.on("disconnect", this.onDisconnect);
    }

    private onConnect = () => {
        console.log('Trade feed is connected, connection id' + this.socket.id);
        // The feed forgets subscriptions when it restarts, so rejoin every topic on (re)connect.
        this.topicRefCounts.forEach((_count, topic) => this.socket.emit('subscribe', topic));
    }

    private onDisconnect = () => {
        console.log('Trade feed is disconnected, connection id was ' + this.socket.id);
    }

    public subscribe(topic: string, callback: (...args: any[]) => void) {
       
        const callbackFn = (args: any) => {
            console.log("received message -> "+ JSON.stringify(args));
            if (args.from !== 'System' && args.topic === topic) {
                callback(args.payload);
            }
        }
        this.socket.on('publish', callbackFn);
        this.topicRefCounts.set(topic, (this.topicRefCounts.get(topic) ?? 0) + 1);
        if (this.socket.connected) {
            this.socket.emit('subscribe', topic);
        }
        console.log('subscribing', topic);
        return () => {
            this.unSubscribe(topic, callbackFn);
        }
    }

    public unSubscribe(topic: string, callback: (...args: any[]) => void) {
        console.log('unsubscribing' + topic)
        const remaining = (this.topicRefCounts.get(topic) ?? 1) - 1;
        if (remaining > 0) {
            this.topicRefCounts.set(topic, remaining);
        } else {
            this.topicRefCounts.delete(topic);
            this.socket.emit('unsubscribe', topic);
        }
        this.socket.off('publish', callback)
    }
}
