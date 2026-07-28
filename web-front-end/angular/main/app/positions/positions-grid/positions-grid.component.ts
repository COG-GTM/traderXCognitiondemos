import { ColDef, GetRowIdParams, GridApi, GridReadyEvent, ValueFormatterParams } from 'ag-grid-community';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Position } from '../../model/trade.model';

@Component({
    selector: 'app-positions-grid',
    templateUrl: './positions-grid.component.html'
})
export class PositionsGridComponent implements OnChanges {
    @Input() positions: Position[] = [];
    @Input() positionUpdate?: Position;
    gridApi?: GridApi<Position>;
    columnDefs: ColDef[] = [
        {
            headerName: 'SECURITY',
            field: 'security'
        },
        {
            headerName: 'QUANTITY',
            field: 'quantity',
            enableCellChangeFlash: true
        },
        {
            headerName: 'UPDATED',
            field: 'updated',
            enableCellChangeFlash: true,
            valueFormatter: (params: ValueFormatterParams<Position, Date>) =>
                params.value ? new Date(params.value).toLocaleString() : ''
        }
    ];

    ngOnChanges(change: SimpleChanges) {
        if (change.positionUpdate?.currentValue) {
            this.update(change.positionUpdate.currentValue);
        }
    }

    onGridReady(params: GridReadyEvent<Position>) {
        console.log('positions grid is ready...');
        this.gridApi = params.api;
        this.gridApi.sizeColumnsToFit();
    }

    getRowId(params: GetRowIdParams<Position>): string {
        return `Position-${params.data.security}`;
    }

    private update(data: Position) {
        const gridApi = this.gridApi;
        if (!gridApi) {
            return;
        }
        const row = gridApi.getRowNode(`Position-${data.security}`);
        gridApi.applyTransaction(row?.data
            ? { update: [Object.assign(row.data, { quantity: data.quantity, updated: data.updated })] }
            : { add: [{ ...data }], addIndex: 0 });
    }
}
