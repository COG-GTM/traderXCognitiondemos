import { io, Socket } from 'socket.io-client';

// Self-contained trade-feed helper for the Position Blotter, following the
// pattern in src/socket.ts and the Angular TradeFeedService. Kept local so this
// piece does not depend on any other migration piece's files.
const tradeFeedUrl = `http://${window.location.hostname}:18086`;

let socket: Socket | undefined;

const getSocket = (): Socket => {
	if (!socket) {
		socket = io(tradeFeedUrl);
	}
	return socket;
};

/**
 * Subscribe to a trade-feed topic. Ignores messages originating from 'System'
 * and messages for other topics. Returns an unsubscribe function.
 */
export const subscribe = (
	topic: string,
	callback: (payload: any) => void,
): (() => void) => {
	const s = getSocket();
	const handler = (args: any) => {
		if (args?.from !== 'System' && args?.topic === topic) {
			callback(args.payload);
		}
	};
	s.on('publish', handler);
	s.emit('subscribe', topic);
	return () => {
		s.emit('unsubscribe', topic);
		s.off('publish', handler);
	};
};
