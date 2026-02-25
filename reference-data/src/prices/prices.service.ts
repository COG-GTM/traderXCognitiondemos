import { Injectable } from '@nestjs/common';
import { Price } from './interfaces/price.interface';
import { StocksService } from '../stocks/stocks.service';

@Injectable()
export class PricesService {
    private priceCache: Map<string, number> = new Map();

    constructor(private stocksService: StocksService) {}

    async getPrice(ticker: string): Promise<Price | undefined> {
        const stock = await this.stocksService.findByTicker(ticker);
        if (!stock) {
            return undefined;
        }

        let currentPrice = this.priceCache.get(ticker);
        if (currentPrice === undefined) {
            // Seed from a deterministic base price derived from the ticker string
            currentPrice = this.seedPrice(ticker);
        }

        // Apply a small random walk: +/- up to 0.5%
        const change = currentPrice * (Math.random() - 0.5) * 0.01;
        currentPrice = Math.round((currentPrice + change) * 100) / 100;
        this.priceCache.set(ticker, currentPrice);

        return { ticker, price: currentPrice };
    }

    private seedPrice(ticker: string): number {
        // Generate a deterministic seed price between 20 and 500 based on ticker hash
        let hash = 0;
        for (let i = 0; i < ticker.length; i++) {
            hash = ((hash << 5) - hash) + ticker.charCodeAt(i);
            hash |= 0;
        }
        const normalized = (Math.abs(hash) % 48000) / 100 + 20;
        return Math.round(normalized * 100) / 100;
    }
}
