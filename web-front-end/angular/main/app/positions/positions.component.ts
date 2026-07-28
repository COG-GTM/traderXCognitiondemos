import { Component, OnDestroy, OnInit } from '@angular/core';
import { Account } from '../model/account.model';
import { Position } from '../model/trade.model';
import { AccountService } from '../service/account.service';
import { PositionService } from '../service/position.service';
import { TradeFeedService } from '../service/trade-feed.service';

@Component({
    selector: 'app-positions',
    templateUrl: './positions.component.html'
})
export class PositionsComponent implements OnInit, OnDestroy {
    accounts: Account[] = [];
    accountModel?: Account = undefined;
    positions: Position[] = [];
    positionSnapshot: Position[] = [];
    positionUpdate?: Position;
    private pendingPositions: Position[] = [];
    private isPending = true;
    private socketUnSubscribeFn?: Function;

    constructor(private accountService: AccountService,
        private positionService: PositionService,
        private tradeFeed: TradeFeedService) { }

    ngOnInit(): void {
        this.accountService.getAccounts().subscribe((accounts) => {
            this.accounts = accounts;
            if (this.accounts.length) {
                this.setAccount(this.accounts[0]);
            }
        });
    }

    onAccountChange(account: Account) {
        console.log('onAccountChange', account);
        account && this.setAccount(account);
    }

    ngOnDestroy(): void {
        this.socketUnSubscribeFn?.();
    }

    private setAccount(account: Account) {
        this.accountModel = account;
        this.isPending = true;
        this.pendingPositions = [];
        this.positions = [];
        this.positionSnapshot = [];
        this.positionUpdate = undefined;

        this.positionService.getPositions(account.id).subscribe((positions: Position[]) => {
            this.positions = positions;
            this.processPendingPositions();
            this.positionSnapshot = this.positions;
        }, () => {
            this.isPending = false;
        });

        this.socketUnSubscribeFn?.();
        this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${account.id}/positions`, (data: Position) => {
            this.updatePosition(data);
        });
    }

    private updatePosition(data: Position) {
        if (this.isPending) {
            this.pendingPositions.push(data);
        } else {
            this.merge(data);
            this.positionUpdate = data;
        }
    }

    private processPendingPositions() {
        this.pendingPositions.forEach((position) => this.merge(position));
        this.pendingPositions = [];
        this.isPending = false;
    }

    private merge(data: Position) {
        const known = this.positions.some((position) => position.security === data.security);
        this.positions = known
            ? this.positions.map((position) => position.security === data.security ? { ...position, ...data } : position)
            : [data, ...this.positions];
    }
}
