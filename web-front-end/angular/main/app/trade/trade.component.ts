import { Component, OnInit, TemplateRef, inject, signal } from '@angular/core';
import { TradeTicket } from '../model/trade.model';
import { Account } from '../model/account.model';
import { AccountService } from '../service/account.service';
import { Stock } from '../model/symbol.model';
import { SymbolService } from '../service/symbols.service';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { TradeTicketComponent } from './trade-ticket/trade-ticket.component';
import { AlertComponent } from 'ngx-bootstrap/alert';
import { TradeBlotterComponent } from './trade-blotter/trade-blotter.component';
import { PositionBlotterComponent } from './position-blotter/position-blotter.component';
import { JsonPipe } from '@angular/common';

@Component({
    selector: 'app-trade',
    templateUrl: './trade.component.html',
    styleUrls: ['./trade.component.scss'],
    imports: [DropdownComponent, TradeTicketComponent, AlertComponent, TradeBlotterComponent, PositionBlotterComponent, JsonPipe]
})
export class TradeComponent implements OnInit {
    private accountService = inject(AccountService);
    private symbolService = inject(SymbolService);
    private modalService = inject(BsModalService);

    readonly accounts = signal<Account[]>([]);
    readonly accountModel = signal<Account | undefined>(undefined);
    readonly stocks = signal<Stock[]>([]);
    modalRef?: BsModalRef;
    readonly createTicketResponse = signal<any>(undefined);

    ngOnInit(): void {
        this.accountService.getAccounts().subscribe((accounts) => {
            this.accounts.set(accounts);
            this.setAccount(accounts[5]);
            console.log(accounts);
        });
        this.symbolService.getStocks().subscribe((stocks) => this.stocks.set(stocks));
    }

    onAccountChange(account: Account) {
        console.log('onAccountChange', arguments);
        account && this.setAccount(account);
    }

    getAccountName(item: Account) {
        return item.displayName;
    }

    openTicket(template: TemplateRef<any>) {
        this.modalRef = this.modalService.show(template);
    }

    createTradeTicket(ticket: TradeTicket) {
        console.log('createTradeTicket', ticket);
        this.symbolService.createTicket(ticket).subscribe((response) => {
            console.log(response);
            this.createTicketResponse.set(response);
        });
        this.closeTicket();
    }

    closeTicket() {
        this.modalRef?.hide();
    }

    onCloseAlert() {
        this.createTicketResponse.set(undefined);
    }

    private setAccount(account: Account) {
        this.accountModel.set(account);
    }
}
