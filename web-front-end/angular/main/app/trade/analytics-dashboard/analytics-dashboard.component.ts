import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Account } from 'main/app/model/account.model';
import { Trade, Position, Side, State } from 'main/app/model/trade.model';
import { PositionService } from 'main/app/service/position.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';
import { ChartConfiguration, ChartData } from 'chart.js';

interface SecurityVolume {
    security: string;
    totalQuantity: number;
}

interface StateCount {
    state: string;
    count: number;
}

@Component({
    selector: 'app-analytics-dashboard',
    templateUrl: './analytics-dashboard.component.html',
    styleUrls: ['./analytics-dashboard.component.scss']
})
export class AnalyticsDashboardComponent implements OnChanges, OnDestroy {
    @Input() account?: Account;

    trades: Trade[] = [];
    positions: Position[] = [];
    isPending = true;
    pendingTrades: Trade[] = [];
    pendingPositions: Position[] = [];

    tradeUnsubscribeFn: Function;
    positionUnsubscribeFn: Function;

    totalTrades = 0;
    buyCount = 0;
    sellCount = 0;
    buyRatioPercent = 0;
    sellRatioPercent = 0;

    volumeChartData: ChartData<'bar'> = { labels: [], datasets: [] };
    volumeChartOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false },
            tooltip: {
                backgroundColor: '#1E293B',
                titleColor: '#FFFFFF',
                bodyColor: '#CBD5E1',
                borderColor: '#22D3EE',
                borderWidth: 1
            }
        },
        scales: {
            x: {
                ticks: { color: '#CBD5E1' },
                grid: { color: 'rgba(255,255,255,0.05)' }
            },
            y: {
                ticks: { color: '#CBD5E1' },
                grid: { color: 'rgba(255,255,255,0.05)' }
            }
        }
    };

    stateChartData: ChartData<'doughnut'> = { labels: [], datasets: [] };
    stateChartOptions: ChartConfiguration<'doughnut'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'bottom',
                labels: { color: '#CBD5E1', padding: 12 }
            },
            tooltip: {
                backgroundColor: '#1E293B',
                titleColor: '#FFFFFF',
                bodyColor: '#CBD5E1',
                borderColor: '#22D3EE',
                borderWidth: 1
            }
        }
    };

    positionChartData: ChartData<'bar'> = { labels: [], datasets: [] };
    positionChartOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
            legend: { display: false },
            tooltip: {
                backgroundColor: '#1E293B',
                titleColor: '#FFFFFF',
                bodyColor: '#CBD5E1',
                borderColor: '#60A5FA',
                borderWidth: 1
            }
        },
        scales: {
            x: {
                ticks: { color: '#CBD5E1' },
                grid: { color: 'rgba(255,255,255,0.05)' }
            },
            y: {
                ticks: { color: '#CBD5E1' },
                grid: { color: 'rgba(255,255,255,0.05)' }
            }
        }
    };

    buySellChartData: ChartData<'doughnut'> = { labels: [], datasets: [] };
    buySellChartOptions: ChartConfiguration<'doughnut'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'bottom',
                labels: { color: '#CBD5E1', padding: 12 }
            },
            tooltip: {
                backgroundColor: '#1E293B',
                titleColor: '#FFFFFF',
                bodyColor: '#CBD5E1',
                borderColor: '#22D3EE',
                borderWidth: 1
            }
        }
    };

    private readonly accentColors = ['#22D3EE', '#60A5FA', '#818CF8', '#A78BFA', '#4ADE80', '#F87171'];
    private readonly stateColors: Record<string, string> = {
        [State.New]: '#22D3EE',
        [State.Processing]: '#60A5FA',
        [State.Pending]: '#818CF8',
        [State.Settled]: '#4ADE80'
    };

    constructor(
        private tradeFeed: TradeFeedService,
        private positionService: PositionService
    ) {}

    ngOnChanges(change: SimpleChanges) {
        if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
            const accountId = change.account.currentValue.id;
            this.resetData();

            this.positionService.getTrades(accountId).subscribe((trades: Trade[]) => {
                this.trades = trades;
                this.processPendingTrades();
                this.recalculate();
            });

            this.positionService.getPositions(accountId).subscribe((positions: Position[]) => {
                this.positions = positions;
                this.processPendingPositions();
                this.recalculate();
            });

            this.tradeUnsubscribeFn?.();
            this.tradeUnsubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/trades`, (data: Trade) => {
                this.handleTradeUpdate(data);
            });

            this.positionUnsubscribeFn?.();
            this.positionUnsubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/positions`, (data: Position) => {
                this.handlePositionUpdate(data);
            });
        }
    }

    ngOnDestroy() {
        this.tradeUnsubscribeFn?.();
        this.positionUnsubscribeFn?.();
    }

    private resetData() {
        this.trades = [];
        this.positions = [];
        this.pendingTrades = [];
        this.pendingPositions = [];
        this.isPending = true;
        this.totalTrades = 0;
        this.buyCount = 0;
        this.sellCount = 0;
        this.buyRatioPercent = 0;
        this.sellRatioPercent = 0;
    }

    private handleTradeUpdate(data: Trade) {
        if (this.isPending) {
            this.pendingTrades.push(data);
        } else {
            this.applyTradeUpdate(data);
            this.recalculate();
        }
    }

    private handlePositionUpdate(data: Position) {
        if (this.isPending) {
            this.pendingPositions.push(data);
        } else {
            this.applyPositionUpdate(data);
            this.recalculate();
        }
    }

    private applyTradeUpdate(data: Trade) {
        const idx = this.trades.findIndex(t => t.id === data.id);
        if (idx >= 0) {
            this.trades[idx] = { ...this.trades[idx], ...data };
        } else {
            this.trades = [data, ...this.trades];
        }
    }

    private applyPositionUpdate(data: Position) {
        const idx = this.positions.findIndex(p => p.security === data.security);
        if (idx >= 0) {
            this.positions[idx] = { ...this.positions[idx], ...data };
        } else {
            this.positions = [data, ...this.positions];
        }
    }

    private processPendingTrades() {
        this.pendingTrades.forEach(t => this.applyTradeUpdate(t));
        this.pendingTrades = [];
        this.isPending = false;
    }

    private processPendingPositions() {
        this.pendingPositions.forEach(p => this.applyPositionUpdate(p));
        this.pendingPositions = [];
    }

    private recalculate() {
        this.calcTradeMetrics();
        this.buildVolumeChart();
        this.buildStateChart();
        this.buildBuySellChart();
        this.buildPositionChart();
    }

    private calcTradeMetrics() {
        this.totalTrades = this.trades.length;
        this.buyCount = this.trades.filter(t => t.side === Side.Buy).length;
        this.sellCount = this.trades.filter(t => t.side === Side.Sell).length;
        this.buyRatioPercent = this.totalTrades > 0 ? Math.round((this.buyCount / this.totalTrades) * 100) : 0;
        this.sellRatioPercent = this.totalTrades > 0 ? Math.round((this.sellCount / this.totalTrades) * 100) : 0;
    }

    private buildVolumeChart() {
        const volumeMap = new Map<string, number>();
        this.trades.forEach(t => {
            volumeMap.set(t.security, (volumeMap.get(t.security) || 0) + t.quantity);
        });

        const sorted = [...volumeMap.entries()].sort((a, b) => b[1] - a[1]).slice(0, 10);
        const labels = sorted.map(e => e[0]);
        const data = sorted.map(e => e[1]);
        const bgColors = labels.map((_, i) => this.accentColors[i % this.accentColors.length]);

        this.volumeChartData = {
            labels,
            datasets: [{
                data,
                backgroundColor: bgColors,
                borderColor: bgColors,
                borderWidth: 1,
                borderRadius: 4
            }]
        };
    }

    private buildStateChart() {
        const stateMap = new Map<string, number>();
        this.trades.forEach(t => {
            const state = t.state || 'Unknown';
            stateMap.set(state, (stateMap.get(state) || 0) + 1);
        });

        const labels = [...stateMap.keys()];
        const data = [...stateMap.values()];
        const bgColors = labels.map(l => this.stateColors[l] || '#94A3B8');

        this.stateChartData = {
            labels,
            datasets: [{
                data,
                backgroundColor: bgColors,
                borderColor: '#0F172A',
                borderWidth: 1
            }]
        };
    }

    private buildBuySellChart() {
        this.buySellChartData = {
            labels: ['Buy', 'Sell'],
            datasets: [{
                data: [this.buyCount, this.sellCount],
                backgroundColor: ['#4ADE80', '#F87171'],
                borderColor: '#0F172A',
                borderWidth: 1
            }]
        };
    }

    private buildPositionChart() {
        const sorted = [...this.positions]
            .sort((a, b) => Math.abs(b.quantity) - Math.abs(a.quantity))
            .slice(0, 10);

        const labels = sorted.map(p => p.security);
        const data = sorted.map(p => p.quantity);
        const bgColors = data.map(v => v >= 0 ? '#4ADE80' : '#F87171');

        this.positionChartData = {
            labels,
            datasets: [{
                data,
                backgroundColor: bgColors,
                borderColor: bgColors,
                borderWidth: 1,
                borderRadius: 4
            }]
        };
    }
}
