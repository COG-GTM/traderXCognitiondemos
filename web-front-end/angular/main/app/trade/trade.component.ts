import { Component, OnInit, TemplateRef } from '@angular/core';
import { Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TradeTicket } from '../model/trade.model';
import { formatRejectionMessage, parseTradeRejection } from '../model/trade-rejection.model';
import { Account } from '../model/account.model';
import { AccountService } from '../service/account.service';
import { Stock } from '../model/symbol.model';
import { SymbolService } from '../service/symbols.service';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';

@Component({
    selector: 'app-trade',
    templateUrl: './trade.component.html',
    styleUrls: ['./trade.component.scss']
})
export class TradeComponent implements OnInit {
    accounts: Account[] = [];
    accountModel?: Account = undefined;
    stocks: Stock[] = [];
    modalRef?: BsModalRef;
    createTicketResponse: any;
    ticketRejectionMessage?: string;
    private account = new Subject<Account>();

    constructor(private accountService: AccountService,
        private symbolService: SymbolService,
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

    openTicket(template: TemplateRef<any>) {
        this.ticketRejectionMessage = undefined;
        this.modalRef = this.modalService.show(template);
    }

    createTradeTicket(ticket: TradeTicket) {
        console.log('createTradeTicket', ticket);
        this.ticketRejectionMessage = undefined;
        this.symbolService.createTicket(ticket).subscribe({
            next: (response) => {
                console.log(response);
                this.createTicketResponse = response;
                this.closeTicket();
            },
            error: (error: HttpErrorResponse) => {
                const rejection = parseTradeRejection(error.status, error.error);
                if (rejection) {
                    this.ticketRejectionMessage = formatRejectionMessage(rejection);
                    return;
                }
                this.createTicketResponse = error.error ?? { success: false };
                this.closeTicket();
            }
        });
    }

    closeTicket() {
        this.ticketRejectionMessage = undefined;
        this.modalRef?.hide();
    }

    onCloseAlert() {
        this.createTicketResponse = undefined;
    }

    private setAccount(account: Account) {
        this.accountModel = account;
        this.account.next(account);
    }
}
