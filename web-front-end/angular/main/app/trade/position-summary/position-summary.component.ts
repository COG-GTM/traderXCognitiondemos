import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Account } from 'main/app/model/account.model';
import { Position } from 'main/app/model/trade.model';
import { PositionService } from 'main/app/service/position.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';

@Component({
    selector: 'app-position-summary',
    templateUrl: './position-summary.component.html',
    styleUrls: ['./position-summary.component.scss']
})
export class PositionSummaryComponent implements OnChanges, OnDestroy {
    @Input() account?: Account;

    securitiesHeld = 0;
    netQuantity = 0;
    longPositions = 0;
    longQuantity = 0;
    shortPositions = 0;
    shortQuantity = 0;

    private positions = new Map<string, number>();
    private socketUnSubscribeFn: Function;

    constructor(private positionService: PositionService,
        private tradeFeed: TradeFeedService) { }

    ngOnChanges(change: SimpleChanges) {
        if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
            const accountId = change.account.currentValue.id;

            this.positionService.getPositions(accountId).subscribe((positions: Position[]) => {
                this.positions = new Map(positions.map((position) => [position.security, position.quantity]));
                this.recalculate();
            }, () => {
                this.positions.clear();
                this.recalculate();
            });

            this.socketUnSubscribeFn?.();
            this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/positions`, (data: Position) => {
                this.positions.set(data.security, data.quantity);
                this.recalculate();
            });
        }
    }

    ngOnDestroy() {
        this.socketUnSubscribeFn?.();
    }

    private recalculate() {
        const quantities = Array.from(this.positions.values()).filter((quantity) => quantity !== 0);

        this.securitiesHeld = quantities.length;
        this.netQuantity = quantities.reduce((total, quantity) => total + quantity, 0);

        const longs = quantities.filter((quantity) => quantity > 0);
        this.longPositions = longs.length;
        this.longQuantity = longs.reduce((total, quantity) => total + quantity, 0);

        const shorts = quantities.filter((quantity) => quantity < 0);
        this.shortPositions = shorts.length;
        this.shortQuantity = Math.abs(shorts.reduce((total, quantity) => total + quantity, 0));
    }
}
