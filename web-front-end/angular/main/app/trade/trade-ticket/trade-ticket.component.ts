import { Component, Input, OnInit, output } from '@angular/core';
import { TradeTicket } from 'main/app/model/trade.model';
import { Stock } from 'main/app/model/symbol.model';
import { Account } from 'main/app/model/account.model';
import { TypeaheadMatch, TypeaheadDirective } from 'ngx-bootstrap/typeahead';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';

@Component({
    selector: 'app-trade-ticket',
    templateUrl: './trade-ticket.component.html',
    styleUrls: ['./trade-ticket.component.scss'],
    imports: [FormsModule, TypeaheadDirective, NgClass]
})
export class TradeTicketComponent implements OnInit {

  @Input() stocks: Stock[];
  @Input() account: Account | undefined;

  readonly create = output<TradeTicket>();
  readonly cancel = output();

  selectedCompany?: string = undefined;
  ticket: TradeTicket;
  filteredStocks: Stock[] = [];

  ngOnInit() {
    this.ticket = {
      quantity: 0,
      accountId: this.account?.id || 0,
      side: 'Buy',
      security: ''
    };

    this.filteredStocks = this.stocks;
  }


  onSelect(e: TypeaheadMatch): void {
    console.log('Selected value: ', e.value);
    this.ticket.security = e.item.ticker;
  }

  onBlur(): void {
    if (this.selectedCompany) return;
    this.ticket.security = '';
  }

  onCreate() {
    if (!this.ticket.security || !this.ticket.quantity) {
      console.warn('Either security is not selected or quanity is not set!')
      return;
    }
    console.log('create tradeTicket', this.ticket);
    this.create.emit(this.ticket);
  }

  onCancel() {
    this.cancel.emit();
  }
}
