import { ColDef, GridApi, GridReadyEvent, GetRowIdParams } from 'ag-grid-community';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Account } from 'main/app/model/account.model';
import { PnlEntry, PnlService } from 'main/app/service/pnl.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';

@Component({
  selector: 'app-pnl-blotter',
  templateUrl: './pnl-blotter.component.html'
})
export class PnlBlotterComponent implements OnChanges, OnDestroy {
  @Input() account?: Account;
  pnlEntries: PnlEntry[] = [];
  gridApi: GridApi;
  pendingUpdates: PnlEntry[] = [];
  isPending = true;
  socketUnSubscribeFn: Function;

  columnDefs: ColDef[] = [
    {
      headerName: 'SECURITY',
      field: 'security'
    },
    {
      headerName: 'NET QTY',
      field: 'netQuantity',
      enableCellChangeFlash: true
    },
    {
      headerName: 'AVG COST',
      field: 'avgCostBasis',
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    },
    {
      headerName: 'PRICE',
      field: 'currentPrice',
      enableCellChangeFlash: true,
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    },
    {
      headerName: 'MKT VALUE',
      field: 'marketValue',
      enableCellChangeFlash: true,
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    },
    {
      headerName: 'UNREAL P&L',
      field: 'unrealizedPnL',
      enableCellChangeFlash: true,
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    },
    {
      headerName: 'REAL P&L',
      field: 'realizedPnL',
      enableCellChangeFlash: true,
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    },
    {
      headerName: 'TOTAL P&L',
      field: 'totalPnL',
      enableCellChangeFlash: true,
      valueFormatter: (params: any) => params.value != null ? params.value.toFixed(2) : ''
    }
  ];

  constructor(private pnlService: PnlService,
    private tradeFeed: TradeFeedService) { }

  ngOnChanges(change: SimpleChanges) {
    if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
      const accountId = change.account.currentValue.id;
      this.isPending = true;

      this.pnlService.getPnl(accountId).subscribe((entries: PnlEntry[]) => {
        this.pnlEntries = entries;
        this.processPendingUpdates();
      }, () => {
        this.isPending = false;
      });

      this.socketUnSubscribeFn?.();
      this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/pnl`, (data: PnlEntry) => {
        console.log('PnL blotter feed...', data);
        this.updatePnlEntry(data);
      });
    }
  }

  processPendingUpdates() {
    this.pendingUpdates.forEach((entry) => this.update(entry));
    this.pendingUpdates = [];
    this.isPending = false;
  }

  updatePnlEntry(data: PnlEntry) {
    if (this.isPending) {
      this.pendingUpdates.push(data);
    } else {
      this.update(data);
    }
  }

  update(data: PnlEntry) {
    const row = this.gridApi.getRowNode(data.security);
    let pnlData;
    if (row) {
      pnlData = {
        update: [Object.assign(row.data, {
          netQuantity: data.netQuantity,
          avgCostBasis: data.avgCostBasis,
          currentPrice: data.currentPrice,
          marketValue: data.marketValue,
          unrealizedPnL: data.unrealizedPnL,
          realizedPnL: data.realizedPnL,
          totalPnL: data.totalPnL
        })]
      };
    } else {
      pnlData = {
        add: [data],
        addIndex: 0
      };
    }
    this.gridApi.applyTransaction(pnlData);
  }

  onGridReady(params: GridReadyEvent) {
    console.log('PnL blotter is ready...');
    this.gridApi = params.api;
  }

  getRowId(params: GetRowIdParams<any>): string {
    return `PnL-${params.data.security}`;
  }

  ngOnDestroy() {
    this.socketUnSubscribeFn?.();
  }
}
