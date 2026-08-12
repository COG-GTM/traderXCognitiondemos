import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TradeTicketComponent } from './trade-ticket/trade-ticket.component';
import { TradeComponent } from './trade.component';
import { TradeBlotterComponent } from './trade-blotter/trade-blotter.component';
import { AgGridModule } from 'ag-grid-angular';
import { ModalModule } from 'ngx-bootstrap/modal';
import { FormsModule } from '@angular/forms';
import { AlertModule } from 'ngx-bootstrap/alert';
import { PositionBlotterComponent } from './position-blotter/position-blotter.component';
import { DropdownModule } from '../dropdown/dropdown.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TypeaheadModule } from 'ngx-bootstrap/typeahead';
import { TabsModule } from 'ngx-bootstrap/tabs';
import { ComplianceBlotterComponent } from './compliance-blotter/compliance-blotter.component';

@NgModule({
  declarations: [TradeComponent, TradeTicketComponent, TradeBlotterComponent, PositionBlotterComponent, ComplianceBlotterComponent],
  imports: [
    CommonModule,
    AgGridModule,
    BrowserAnimationsModule,
    TypeaheadModule.forRoot(),
    ModalModule.forRoot(),
    AlertModule.forRoot(),
    TabsModule.forRoot(),
    FormsModule,
    DropdownModule
  ],
  exports: [TradeComponent, TradeTicketComponent, TradeBlotterComponent, ComplianceBlotterComponent]
})
export class TradeModule { }
