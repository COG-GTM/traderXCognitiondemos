import { Component, OnInit, TemplateRef } from '@angular/core';
import { Subject } from 'rxjs';
import { TradeTicket } from '../model/trade.model';
import { Account } from '../model/account.model';
import { AccountService } from '../service/account.service';
import { Stock } from '../model/symbol.model';
import { SymbolService } from '../service/symbols.service';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { FormsModule } from '@angular/forms';
import { AlertModule } from 'ngx-bootstrap/alert';
import { ModalModule } from 'ngx-bootstrap/modal';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { TradeBlotterComponent } from './trade-blotter/trade-blotter.component';
import { PositionBlotterComponent } from './position-blotter/position-blotter.component';
import { TradeTicketComponent } from './trade-ticket/trade-ticket.component';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

@Component({
    selector: 'app-trade',
    templateUrl: './trade.component.html',
    styleUrls: ['./trade.component.scss'],
    standalone: true,
    imports: [FormsModule, CommonModule, HttpClientModule, DropdownComponent, ModalModule, AlertModule, TradeBlotterComponent, PositionBlotterComponent, TradeTicketComponent]
})
export class TradeComponent implements OnInit {
    accounts: Account[] = [];
    accountModel?: Account = undefined;
    stocks: Stock[] = [];
    modalRef?: BsModalRef;
    createTicketResponse: any;
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

    private setAccount(account: Account) {
        this.accountModel = account;
        this.account.next(account);
    }
}
