import { Component, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { Account } from '../model/account.model';
import { AccountService } from '../service/account.service';
import { TradeFeedService } from '../service/trade-feed.service';

@Component({
    selector: 'app-technical-analysis',
    templateUrl: './technical-analysis.component.html',
    styleUrls: ['./technical-analysis.component.scss']
})
export class TechnicalAnalysisComponent implements OnInit {
    accounts: Account[] = [];
    accountModel?: Account = undefined;
    selectedSymbol: string = 'AAPL';
    priceData: any[] = [];
    macdData: any[] = [];
    fibonacciLevels: any[] = [];
    rsiData: any[] = [];
    bollingerBands: any[] = [];
    movingAverages: any[] = [];
    
    private account = new Subject<Account>();

    constructor(
        private accountService: AccountService,
        private tradeFeedService: TradeFeedService
    ) { }

    ngOnInit(): void {
        this.loadAccounts();
        this.initializeTechnicalAnalysis();
        this.subscribeToRealTimeData();
    }

    private loadAccounts(): void {
        this.accountService.getAccounts().subscribe((accounts) => {
            this.accounts = accounts;
            if (accounts.length > 0) {
                this.setAccount(accounts[0]);
            }
        });
    }

    private initializeTechnicalAnalysis(): void {
        this.generateSamplePriceData();
        this.calculateMACD();
        this.calculateFibonacci();
        this.calculateRSI();
        this.calculateBollingerBands();
        this.calculateMovingAverages();
    }

    private subscribeToRealTimeData(): void {
        this.tradeFeedService.subscribe('trades', (tradeData: any) => {
            this.updateTechnicalIndicators(tradeData);
        });
    }

    private generateSamplePriceData(): void {
        const basePrice = 150;
        const dataPoints = 100;
        
        for (let i = 0; i < dataPoints; i++) {
            const price = basePrice + Math.sin(i * 0.1) * 10 + Math.random() * 5;
            this.priceData.push({
                timestamp: new Date(Date.now() - (dataPoints - i) * 60000),
                price: price,
                volume: Math.floor(Math.random() * 1000000)
            });
        }
    }

    private calculateMACD(): void {
        const prices = this.priceData.map(d => d.price);
        const ema12 = this.calculateEMA(prices, 12);
        const ema26 = this.calculateEMA(prices, 26);
        
        this.macdData = [];
        for (let i = 0; i < prices.length; i++) {
            if (ema12[i] !== null && ema26[i] !== null) {
                const macdLine = ema12[i]! - ema26[i]!;
                this.macdData.push({
                    timestamp: this.priceData[i].timestamp,
                    macd: macdLine,
                    signal: null,
                    histogram: null
                });
            }
        }
        
        const macdValues = this.macdData.map(d => d.macd);
        const signalLine = this.calculateEMA(macdValues, 9);
        
        for (let i = 0; i < this.macdData.length; i++) {
            if (signalLine[i] !== null) {
                this.macdData[i].signal = signalLine[i];
                this.macdData[i].histogram = this.macdData[i].macd - signalLine[i]!;
            }
        }
    }

    private calculateFibonacci(): void {
        if (this.priceData.length < 2) return;
        
        const prices = this.priceData.map(d => d.price);
        const high = Math.max(...prices);
        const low = Math.min(...prices);
        const range = high - low;
        
        const fibLevels = [0, 0.236, 0.382, 0.5, 0.618, 0.786, 1];
        
        this.fibonacciLevels = fibLevels.map(level => ({
            level: level,
            price: high - (range * level),
            label: `${(level * 100).toFixed(1)}%`
        }));
    }

    private calculateRSI(): void {
        const prices = this.priceData.map(d => d.price);
        const period = 14;
        
        if (prices.length < period + 1) return;
        
        const gains: number[] = [];
        const losses: number[] = [];
        
        for (let i = 1; i < prices.length; i++) {
            const change = prices[i] - prices[i - 1];
            gains.push(change > 0 ? change : 0);
            losses.push(change < 0 ? Math.abs(change) : 0);
        }
        
        this.rsiData = [];
        for (let i = period; i < prices.length; i++) {
            const avgGain = gains.slice(i - period, i).reduce((a, b) => a + b, 0) / period;
            const avgLoss = losses.slice(i - period, i).reduce((a, b) => a + b, 0) / period;
            
            const rs = avgGain / avgLoss;
            const rsi = 100 - (100 / (1 + rs));
            
            this.rsiData.push({
                timestamp: this.priceData[i].timestamp,
                rsi: rsi
            });
        }
    }

    private calculateBollingerBands(): void {
        const prices = this.priceData.map(d => d.price);
        const period = 20;
        const multiplier = 2;
        
        this.bollingerBands = [];
        
        for (let i = period - 1; i < prices.length; i++) {
            const slice = prices.slice(i - period + 1, i + 1);
            const sma = slice.reduce((a, b) => a + b, 0) / period;
            
            const variance = slice.reduce((sum, price) => sum + Math.pow(price - sma, 2), 0) / period;
            const stdDev = Math.sqrt(variance);
            
            this.bollingerBands.push({
                timestamp: this.priceData[i].timestamp,
                middle: sma,
                upper: sma + (stdDev * multiplier),
                lower: sma - (stdDev * multiplier)
            });
        }
    }

    private calculateMovingAverages(): void {
        const prices = this.priceData.map(d => d.price);
        const periods = [10, 20, 50];
        
        this.movingAverages = [];
        
        for (let i = 0; i < prices.length; i++) {
            const ma: any = { timestamp: this.priceData[i].timestamp };
            
            periods.forEach(period => {
                if (i >= period - 1) {
                    const slice = prices.slice(i - period + 1, i + 1);
                    ma[`ma${period}`] = slice.reduce((a, b) => a + b, 0) / period;
                }
            });
            
            this.movingAverages.push(ma);
        }
    }

    private calculateEMA(prices: number[], period: number): (number | null)[] {
        const ema: (number | null)[] = [];
        const multiplier = 2 / (period + 1);
        
        for (let i = 0; i < prices.length; i++) {
            if (i === 0) {
                ema[i] = prices[i];
            } else if (i < period - 1) {
                ema[i] = null;
            } else if (i === period - 1) {
                const sum = prices.slice(0, period).reduce((a, b) => a + b, 0);
                ema[i] = sum / period;
            } else {
                ema[i] = (prices[i] * multiplier) + (ema[i - 1]! * (1 - multiplier));
            }
        }
        
        return ema;
    }

    private updateTechnicalIndicators(tradeData: any): void {
        if (tradeData && tradeData.symbol === this.selectedSymbol) {
            this.priceData.push({
                timestamp: new Date(),
                price: tradeData.price,
                volume: tradeData.quantity
            });
            
            if (this.priceData.length > 200) {
                this.priceData.shift();
            }
            
            this.calculateMACD();
            this.calculateFibonacci();
            this.calculateRSI();
            this.calculateBollingerBands();
            this.calculateMovingAverages();
        }
    }

    onAccountChange(account: Account): void {
        this.setAccount(account);
    }

    onSymbolChange(symbol: string): void {
        this.selectedSymbol = symbol;
        this.priceData = [];
        this.initializeTechnicalAnalysis();
    }

    getAccountName(item: Account): string {
        return item.displayName;
    }

    private setAccount(account: Account): void {
        this.accountModel = account;
        this.account.next(account);
    }

    getBollingerPosition(): string {
        if (this.bollingerBands.length === 0 || this.priceData.length === 0) return '0';
        
        const latestBB = this.bollingerBands[this.bollingerBands.length - 1];
        const currentPrice = this.priceData[this.priceData.length - 1].price;
        
        const position = ((currentPrice - latestBB.lower) / (latestBB.upper - latestBB.lower)) * 100;
        return Math.max(0, Math.min(100, position)).toFixed(1);
    }

    getTrendAnalysis(): string {
        if (this.movingAverages.length < 2) return 'Insufficient data';
        
        const latest = this.movingAverages[this.movingAverages.length - 1];
        const previous = this.movingAverages[this.movingAverages.length - 2];
        
        if (latest.ma10 && latest.ma20 && previous.ma10 && previous.ma20) {
            if (latest.ma10 > latest.ma20 && previous.ma10 <= previous.ma20) {
                return 'Bullish crossover detected - Short MA crossed above Long MA';
            } else if (latest.ma10 < latest.ma20 && previous.ma10 >= previous.ma20) {
                return 'Bearish crossover detected - Short MA crossed below Long MA';
            } else if (latest.ma10 > latest.ma20) {
                return 'Uptrend - Short MA above Long MA';
            } else {
                return 'Downtrend - Short MA below Long MA';
            }
        }
        
        return 'Trend analysis unavailable';
    }

    getMomentumAnalysis(): string {
        if (this.rsiData.length === 0 || this.macdData.length === 0) return 'Insufficient data';
        
        const latestRSI = this.rsiData[this.rsiData.length - 1].rsi;
        const latestMACD = this.macdData[this.macdData.length - 1];
        
        let analysis = '';
        
        if (latestRSI > 70) {
            analysis += 'RSI indicates overbought conditions. ';
        } else if (latestRSI < 30) {
            analysis += 'RSI indicates oversold conditions. ';
        } else {
            analysis += 'RSI in neutral zone. ';
        }
        
        if (latestMACD.macd > latestMACD.signal) {
            analysis += 'MACD shows bullish momentum.';
        } else {
            analysis += 'MACD shows bearish momentum.';
        }
        
        return analysis;
    }

    getSupportResistanceAnalysis(): string {
        if (this.fibonacciLevels.length === 0) return 'Insufficient data';
        
        const currentPrice = this.priceData.length > 0 ? this.priceData[this.priceData.length - 1].price : 0;
        
        let nearestSupport = 0;
        let nearestResistance = Infinity;
        
        this.fibonacciLevels.forEach(level => {
            if (level.price < currentPrice && level.price > nearestSupport) {
                nearestSupport = level.price;
            }
            if (level.price > currentPrice && level.price < nearestResistance) {
                nearestResistance = level.price;
            }
        });
        
        return `Support: $${nearestSupport.toFixed(2)}, Resistance: $${nearestResistance.toFixed(2)}`;
    }

    getOverallSignal(): string {
        if (this.rsiData.length === 0 || this.macdData.length === 0 || this.movingAverages.length === 0) {
            return 'NEUTRAL';
        }
        
        let bullishSignals = 0;
        let bearishSignals = 0;
        
        const latestRSI = this.rsiData[this.rsiData.length - 1].rsi;
        const latestMACD = this.macdData[this.macdData.length - 1];
        const latest = this.movingAverages[this.movingAverages.length - 1];
        
        if (latestRSI < 30) bullishSignals++;
        if (latestRSI > 70) bearishSignals++;
        
        if (latestMACD.macd > latestMACD.signal) bullishSignals++;
        else bearishSignals++;
        
        if (latest.ma10 && latest.ma20 && latest.ma10 > latest.ma20) bullishSignals++;
        else if (latest.ma10 && latest.ma20) bearishSignals++;
        
        if (bullishSignals > bearishSignals) return 'BULLISH';
        if (bearishSignals > bullishSignals) return 'BEARISH';
        return 'NEUTRAL';
    }

    getOverallSignalClass(): string {
        const signal = this.getOverallSignal();
        return signal.toLowerCase();
    }
}
