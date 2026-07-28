import { Component, Input } from '@angular/core';
import { Position } from '../../model/trade.model';

@Component({
    selector: 'app-positions-summary',
    templateUrl: './positions-summary.component.html'
})
export class PositionsSummaryComponent {
    @Input() positions: Position[] = [];

    get securityCount(): number {
        return new Set(this.positions.map((position) => position.security)).size;
    }

    get totalQuantity(): number {
        return this.positions.reduce((total, position) => total + Math.abs(position.quantity), 0);
    }
}
