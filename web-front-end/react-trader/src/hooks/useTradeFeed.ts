import { useEffect } from 'react';
import { getTradeFeedService, TradeFeedService } from '../services/trade-feed.service';

export interface TradeFeedLike {
  subscribe(topic: string, callback: (...args: any[]) => void): () => void;
}

export function useTradeFeed(
  topic: string,
  callback: (payload: any) => void,
  feed: TradeFeedLike | TradeFeedService = getTradeFeedService()
) {
  useEffect(() => {
    const unsubscribe = feed.subscribe(topic, callback);
    return unsubscribe;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topic]);
}
