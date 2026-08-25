import { TestBed } from '@angular/core/testing';
import { TradeFeedService } from './trade-feed.service';

/** Minimal in-memory stand-in for the socket.io client socket. */
class FakeSocket {
    id = 'fake-socket';
    handlers = new Map<string, Function[]>();
    emitted: Array<{ event: string; payload: any }> = [];
    disconnected = false;

    on(event: string, handler: Function) {
        const handlers = this.handlers.get(event) ?? [];
        handlers.push(handler);
        this.handlers.set(event, handlers);
    }

    off(event: string, handler: Function) {
        this.handlers.set(event, (this.handlers.get(event) ?? []).filter((h) => h !== handler));
    }

    emit(event: string, payload?: any) {
        this.emitted.push({ event, payload });
    }

    deliver(event: string, payload: any) {
        (this.handlers.get(event) ?? []).forEach((handler) => handler(payload));
    }

    disconnect() {
        this.disconnected = true;
    }
}

describe('TradeFeedService message routing', () => {
    let service: TradeFeedService;
    let socket: FakeSocket;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [TradeFeedService] });
        service = TestBed.inject(TradeFeedService);
        // drop the real socket.io connection created in the constructor
        (service as any).socket?.disconnect?.();
        socket = new FakeSocket();
        (service as any).socket = socket;
    });

    // UI-11c
    it('should only invoke the callback for its own topic', () => {
        const received: any[] = [];
        service.subscribe('/accounts/1/trades', (payload) => received.push(payload));

        socket.deliver('publish', { from: 'trade-processor', topic: '/accounts/2/trades', payload: { id: 'other' } });
        socket.deliver('publish', { from: 'trade-processor', topic: '/accounts/1/trades', payload: { id: 'mine' } });

        expect(received).toEqual([{ id: 'mine' }]);
    });

    // UI-11d
    it('should drop messages published by System on its own topic', () => {
        const received: any[] = [];
        service.subscribe('/accounts/1/trades', (payload) => received.push(payload));

        socket.deliver('publish', { from: 'System', topic: '/accounts/1/trades', payload: { id: 'system' } });

        expect(received).toEqual([]);
    });

    // UI-15f
    it('should stop delivering messages once the returned unsubscribe function is called', () => {
        const received: any[] = [];
        const unsubscribe = service.subscribe('/accounts/1/trades', (payload) => received.push(payload));

        unsubscribe();
        socket.deliver('publish', { from: 'trade-processor', topic: '/accounts/1/trades', payload: { id: 'after' } });

        expect(received).toEqual([]);
        expect(socket.emitted).toContain({ event: 'unsubscribe', payload: '/accounts/1/trades' });
    });

    // UI-15g
    it('should keep the other subscription alive when one of two topics is torn down', () => {
        const first: any[] = [];
        const second: any[] = [];
        const unsubscribeFirst = service.subscribe('/accounts/1/trades', (payload) => first.push(payload));
        service.subscribe('/accounts/2/trades', (payload) => second.push(payload));

        unsubscribeFirst();
        socket.deliver('publish', { from: 'trade-processor', topic: '/accounts/1/trades', payload: { id: 'one' } });
        socket.deliver('publish', { from: 'trade-processor', topic: '/accounts/2/trades', payload: { id: 'two' } });

        expect(first).toEqual([]);
        expect(second).toEqual([{ id: 'two' }]);
    });

    // UI-13h
    it('should throw when a publish frame arrives with no envelope', () => {
        service.subscribe('/accounts/1/trades', () => undefined);

        expect(() => socket.deliver('publish', null)).toThrowError(TypeError);
    });
});
