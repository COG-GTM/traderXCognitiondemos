// Ported from web-front-end/angular/main/app/service/trade-feed.service.ts
// Wraps the socket.io trade feed (:18086). Self-contained local client.
import { useEffect } from 'react';
import { io, Socket } from 'socket.io-client';
import { ServiceUrls } from './config';

interface PublishMessage {
	from: string;
	topic: string;
	payload: unknown;
}

let socket: Socket | null = null;

function getSocket(): Socket {
	if (!socket) {
		socket = io(ServiceUrls.tradeFeed);
		socket.on('connect', () => console.log('Trade feed connected, id ' + socket?.id));
		socket.on('disconnect', () => console.log('Trade feed disconnected'));
	}
	return socket;
}

/**
 * Subscribe to a trade-feed topic. Invokes `callback` with the message payload
 * for matching topics, ignoring messages whose `from` is 'System' (mirrors the
 * Angular TradeFeedService). Returns an unsubscribe function.
 */
export function subscribe(
	topic: string,
	callback: (payload: unknown) => void,
): () => void {
	const s = getSocket();
	const handler = (message: PublishMessage) => {
		if (message.from !== 'System' && message.topic === topic) {
			callback(message.payload);
		}
	};
	s.on('publish', handler);
	s.emit('subscribe', topic);
	return () => {
		s.emit('unsubscribe', topic);
		s.off('publish', handler);
	};
}

/**
 * React hook: subscribes to `topic` for the lifetime of the component and
 * cleans up on unmount / dependency change.
 */
export function useTradeFeed(topic: string, callback: (payload: unknown) => void): void {
	useEffect(() => {
		const unsubscribe = subscribe(topic, callback);
		return unsubscribe;
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [topic]);
}
