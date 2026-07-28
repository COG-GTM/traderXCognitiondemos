import { Component, Input, OnChanges } from '@angular/core';
import { Position, State, Trade } from '../../model/trade.model';

@Component({
    selector: 'app-blotter-summary',
    templateUrl: './blotter-summary.component.html'
})
export class BlotterSummaryComponent implements OnChanges {
    @Input() trades: Trade[] = [];
    @Input() positions: Position[] = [];

    tradesToday = 0;
    working = 0;
    settled = 0;
    securitiesHeld = 0;

    private readonly workingStates: State[] = [State.New, State.Processing, State.Pending];

    ngOnChanges() {
        this.tradesToday = this.trades.length;
        this.working = this.trades.filter((trade) => this.workingStates.includes(trade.state)).length;
        this.settled = this.trades.filter((trade) => trade.state === State.Settled).length;
        this.securitiesHeld = new Set(this.positions
            .filter((position) => position.quantity !== 0)
            .map((position) => position.security)).size;
    }
}
