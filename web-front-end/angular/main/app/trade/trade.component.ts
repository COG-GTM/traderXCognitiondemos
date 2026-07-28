import { Component, OnDestroy, OnInit, TemplateRef } from '@angular/core';
import { Subject } from 'rxjs';
import { Position, StateFilter, Trade, TradeTicket } from '../model/trade.model';
import { Account } from '../model/account.model';
import { AccountService } from '../service/account.service';
import { Stock } from '../model/symbol.model';
import { SymbolService } from '../service/symbols.service';
import { PositionService } from '../service/position.service';
import { TradeFeedService } from '../service/trade-feed.service';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';

@Component({
    selector: 'app-trade',
    templateUrl: './trade.component.html',
    styleUrls: ['./trade.component.scss']
})
export class TradeComponent implements OnInit, OnDestroy {
    accounts: Account[] = [];
    accountModel?: Account = undefined;
    stocks: Stock[] = [];
    trades: Trade[] = [];
    positions: Position[] = [];
    stateFilter: StateFilter = 'All';
    securityFilter = '';
    modalRef?: BsModalRef;
    createTicketResponse: any;
    private account = new Subject<Account>();
    private tradesUnSubscribeFn?: Function;
    private positionsUnSubscribeFn?: Function;

    constructor(private accountService: AccountService,
        private symbolService: SymbolService,
        private positionService: PositionService,
        private tradeFeed: TradeFeedService,
        private modalService: BsModalService) { }

    ngOnInit(): void {
        this.accountService.getAccounts().subscribe((accounts) => {
            this.accounts = accounts;
            this.setAccount(this.accounts[5]);
            console.log(this.accounts);
        });
        this.symbolService.getStocks().subscribe((stocks) => this.stocks = stocks);
    }

    onAccountChange(account: Account) {
        console.log('onAccountChange', arguments);
        account && this.setAccount(account);
    }

    getAccountName(item: Account) {
        return item.displayName;
    }

    onStateFilterChange(state: StateFilter) {
        this.stateFilter = state;
    }

    onSecurityFilterChange(security: string) {
        this.securityFilter = security;
    }

    openTicket(template: TemplateRef<any>) {
        this.modalRef = this.modalService.show(template);
    }

    createTradeTicket(ticket: TradeTicket) {
        console.log('createTradeTicket', ticket);
        this.symbolService.createTicket(ticket).subscribe((response) => {
            console.log(response);
            this.createTicketResponse = response;
        });
        this.closeTicket();
    }

    closeTicket() {
        this.modalRef?.hide();
    }

    onCloseAlert() {
        this.createTicketResponse = undefined;
    }

    ngOnDestroy() {
        this.tradesUnSubscribeFn?.();
        this.positionsUnSubscribeFn?.();
    }

    private setAccount(account: Account) {
        this.accountModel = account;
        this.account.next(account);
        if (account) {
            this.loadSummary(account.id);
        }
    }

    private loadSummary(accountId: number) {
        this.trades = [];
        this.positions = [];
        this.positionService.getTrades(accountId).subscribe((trades) => this.trades = trades);
        this.positionService.getPositions(accountId).subscribe((positions) => this.positions = positions);

        this.tradesUnSubscribeFn?.();
        this.tradesUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/trades`,
            (trade: Trade) => this.updateTrade(trade));

        this.positionsUnSubscribeFn?.();
        this.positionsUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/positions`,
            (position: Position) => this.updatePosition(position));
    }

    private updateTrade(trade: Trade) {
        const known = this.trades.find((item) => item.id === trade.id);
        this.trades = known
            ? this.trades.map((item) => (item.id === trade.id ? { ...item, state: trade.state } : item))
            : [trade, ...this.trades];
    }

    private updatePosition(position: Position) {
        const known = this.positions.find((item) => item.security === position.security);
        this.positions = known
            ? this.positions.map((item) => (item.security === position.security
                ? { ...item, quantity: position.quantity } : item))
            : [position, ...this.positions];
    }
}
