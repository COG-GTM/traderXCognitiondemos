import { ColDef, GridApi, GridReadyEvent, GetRowIdParams, IRowNode, ModelUpdatedEvent } from 'ag-grid-community';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Account } from 'main/app/model/account.model';
import { PositionService } from 'main/app/service/position.service';
import { Observable } from 'rxjs';
import { StateFilter, Trade } from '../../model/trade.model';
import { TradeFeedService } from 'main/app/service/trade-feed.service';

@Component({
    selector: 'app-trade-blotter',
    templateUrl: 'trade-blotter.component.html'
})
export class TradeBlotterComponent implements OnChanges, OnDestroy {
    trades$: Observable<Trade[]>;
    @Input() account?: Account;
    @Input() stateFilter: StateFilter = 'All';
    @Input() securityFilter = '';
    trades: Trade[] = [];
    visibleCount = 0;
    gridApi: GridApi;
    pendingTrades: Trade[] = [];
    isPending = true;
    socketUnSubscribeFn: Function;
    columnDefs: ColDef[] = [
        {
            headerName: 'SECURITY',
            field: 'security'
        },
        {
            headerName: 'QUANTITY',
            field: 'quantity'
        },
        {
            headerName: 'SIDE',
            field: 'side'
        },
        {
            headerName: 'STATE',
            field: 'state',
            enableCellChangeFlash: true
        }
    ];

    constructor(private tradeFeed: TradeFeedService, private tradeService: PositionService) { }

    ngOnChanges(change: SimpleChanges) {
        if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
            const accountId = change.account.currentValue.id;
            this.isPending = true;
            this.tradeService.getTrades(accountId).subscribe((trades: Trade[]) => {
                this.trades = trades;
                this.processPendingTrades();
            });
            this.socketUnSubscribeFn?.();
            this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/trades`, (data: Trade) => {
                console.log('Trade blotter feed...', data);
                this.updateTrades(data);
            });
        }
        if (change.stateFilter || change.securityFilter) {
            this.gridApi?.onFilterChanged();
        }
    }

    isExternalFilterPresent = (): boolean => this.stateFilter !== 'All' || this.securityFilter.trim() !== '';

    doesExternalFilterPass = (node: IRowNode<Trade>): boolean => {
        const trade = node.data;
        if (!trade) { return true; }
        const security = this.securityFilter.trim().toLowerCase();
        return (this.stateFilter === 'All' || trade.state === this.stateFilter)
            && (security === '' || trade.security.toLowerCase().includes(security));
    };

    onModelUpdated(params: ModelUpdatedEvent) {
        this.visibleCount = params.api.getDisplayedRowCount();
    }

    onGridReady(params: GridReadyEvent) {
        console.log('trade blotter is ready...');
        this.gridApi = params.api;
        this.gridApi.sizeColumnsToFit();
    }

    getRowId(params: GetRowIdParams<any>):string {
        return  `Trade-${params.data.id}`;
    }

    ngOnDestroy() {
        this.socketUnSubscribeFn?.();
    }

    private processPendingTrades() {
        this.pendingTrades.forEach((tradeUpdate) => this.update(tradeUpdate));
        this.pendingTrades = [];
        this.isPending = false;
    }

    private update(data: Trade) {
        const row = this.gridApi.getRowNode(data.id);
        let tradeData;
        if (row) {
            tradeData = {
                update: [Object.assign(row.data, { state: data.state })]
            };
        } else {
            tradeData = {
                add: [{
                    accountid: data.accountid,
                    created: data.created,
                    id: data.id,
                    quantity: data.quantity,
                    security: data.security,
                    side: data.side,
                    state: data.state,
                    updated: data.updated
                }],
                addIndex: 0
            };
        }
        this.gridApi.applyTransaction(tradeData);
    }

    private updateTrades(data: Trade) {
        if (this.isPending) {
            this.pendingTrades.push(data);
        } else {
            this.update(data);
        }
    }
}
