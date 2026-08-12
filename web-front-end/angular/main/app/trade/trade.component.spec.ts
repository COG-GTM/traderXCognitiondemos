import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';

import { TradeComponent } from './trade.component';
import { FormsModule } from '@angular/forms';
import { AlertModule } from 'ngx-bootstrap/alert';
import { ModalModule } from 'ngx-bootstrap/modal';
import { AccountService } from '../service/account.service';
import { MockAccountService, MockSymbolService, accounts } from '../test-utils/mocks.service';
import { SymbolService } from '../service/symbols.service';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { TradeTicket, Side } from '../model/trade.model';
import { DropdownModule } from '../dropdown/dropdown.module';

describe('TradeComponent', () => {
    let component: TradeComponent;
    let fixture: ComponentFixture<TradeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                TradeComponent
            ],
            imports: [
                FormsModule,
                DropdownModule,
                ModalModule.forRoot(),
                AlertModule.forRoot()
            ],
            providers: [
                {
                    provide: AccountService,
                    useClass: MockAccountService
                },
                {
                    provide: SymbolService,
                    useClass: MockSymbolService
                }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TradeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should call symbol service to create ticket on call', async () => {
        const ticket: TradeTicket = {
            accountId: 1,
            quantity: 10,
            security: 'abc',
            side: Side.Buy
        };
        spyOn((component as any).symbolService, 'createTicket').and.callThrough();
        spyOn(component, 'closeTicket');
        component.createTradeTicket(ticket);
        expect((component as any).symbolService.createTicket).toHaveBeenCalledWith(ticket);
        expect(component.closeTicket).toHaveBeenCalled();
    });

    it('should keep the ticket open with an inline reason on a 422 rejection', () => {
        const ticket: TradeTicket = { accountId: 1, quantity: 10, security: 'abc', side: Side.Buy };
        const rejection = new HttpErrorResponse({
            status: 422,
            error: { decision: 'REJECTED', reason: 'NOTIONAL_LIMIT_BREACH', limit: 1000000, attempted: 1250000 }
        });
        spyOn((component as any).symbolService, 'createTicket').and.returnValue(throwError(() => rejection));
        spyOn(component, 'closeTicket');

        component.createTradeTicket(ticket);

        expect(component.closeTicket).not.toHaveBeenCalled();
        expect(component.ticketRejectionMessage).toContain('$1,000,000.00');
        expect(component.ticketRejectionMessage).toContain('over by $250,000.00');
    });

    it('should fall back to the generic failure alert for a non-422 error', () => {
        const ticket: TradeTicket = { accountId: 1, quantity: 10, security: 'abc', side: Side.Buy };
        const failure = new HttpErrorResponse({ status: 404, error: 'Trade not found' });
        spyOn((component as any).symbolService, 'createTicket').and.returnValue(throwError(() => failure));
        spyOn(component, 'closeTicket');

        component.createTradeTicket(ticket);

        expect(component.ticketRejectionMessage).toBeUndefined();
        expect(component.createTicketResponse).toEqual('Trade not found');
        expect(component.closeTicket).toHaveBeenCalled();
    });

    it('should get accounts and stocks on init', () => {
        spyOn((component as any).accountService, 'getAccounts').and.callThrough();
        spyOn((component as any).symbolService, 'getStocks').and.callThrough();
        component.ngOnInit();
        expect((component as any).accountService.getAccounts).toHaveBeenCalled();
        expect(component.accounts.length).toEqual(5);
        expect((component as any).symbolService.getStocks).toHaveBeenCalled();
        expect(component.stocks.length).toEqual(5);
    });

    it('should open and close ticket on click', async () => {
        component.accountModel = accounts[0];
        spyOn(component, 'openTicket');
        spyOn(component, 'closeTicket');
        fixture.nativeElement.querySelector('#createTicketBtn').click();
        expect(component.openTicket).toHaveBeenCalled();
        // TODO: need to check how to test bootstrap modal in jasmine
        // fixture.nativeElement.click();
        // expect(component.closeTicket).toHaveBeenCalled();
    });

});
