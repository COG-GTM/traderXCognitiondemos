import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TradeComponent } from './trade.component';
import { FormsModule } from '@angular/forms';
import { AlertModule } from 'ngx-bootstrap/alert';
import { ModalModule } from 'ngx-bootstrap/modal';
import { AccountService } from '../service/account.service';
import {
    MockAccountService,
    MockSymbolService,
    MockTradeFeedService,
    MockTradeService,
    accounts,
    positions,
    trades
} from '../test-utils/mocks.service';
import { SymbolService } from '../service/symbols.service';
import { PositionService } from '../service/position.service';
import { TradeFeedService } from '../service/trade-feed.service';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { TradeTicket, Side, State } from '../model/trade.model';
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
                },
                {
                    provide: PositionService,
                    useClass: MockTradeService
                },
                {
                    provide: TradeFeedService,
                    useClass: MockTradeFeedService
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

    it('should get accounts and stocks on init', () => {
        spyOn((component as any).accountService, 'getAccounts').and.callThrough();
        spyOn((component as any).symbolService, 'getStocks').and.callThrough();
        component.ngOnInit();
        expect((component as any).accountService.getAccounts).toHaveBeenCalled();
        expect(component.accounts.length).toEqual(5);
        expect((component as any).symbolService.getStocks).toHaveBeenCalled();
        expect(component.stocks.length).toEqual(5);
    });

    it('should load trades and positions for the selected account', () => {
        spyOn((component as any).positionService, 'getTrades').and.callThrough();
        spyOn((component as any).positionService, 'getPositions').and.callThrough();
        component.onAccountChange(accounts[0]);
        expect((component as any).positionService.getTrades).toHaveBeenCalledWith(accounts[0].id);
        expect((component as any).positionService.getPositions).toHaveBeenCalledWith(accounts[0].id);
        expect(component.trades.length).toEqual(trades.length);
        expect(component.positions.length).toEqual(positions.length);
    });

    it('should subscribe to the trade and position feeds of the selected account', () => {
        spyOn((component as any).tradeFeed, 'subscribe').and.callThrough();
        component.onAccountChange(accounts[1]);
        expect((component as any).tradeFeed.subscribe)
            .toHaveBeenCalledWith(`/accounts/${accounts[1].id}/trades`, jasmine.any(Function));
        expect((component as any).tradeFeed.subscribe)
            .toHaveBeenCalledWith(`/accounts/${accounts[1].id}/positions`, jasmine.any(Function));
    });

    it('should update a known trade and prepend an unknown one on a feed message', () => {
        component.onAccountChange(accounts[0]);
        const known = { ...component.trades[0], state: State.Settled };
        (component as any).updateTrade(known);
        expect(component.trades[0].state).toEqual(State.Settled);
        expect(component.trades.length).toEqual(trades.length);

        (component as any).updateTrade({ ...known, id: 'new-trade', state: State.New });
        expect(component.trades.length).toEqual(trades.length + 1);
        expect(component.trades[0].id).toEqual('new-trade');
    });

    it('should update a known position and prepend an unknown one on a feed message', () => {
        component.onAccountChange(accounts[0]);
        const known = { ...component.positions[0], quantity: 42 };
        (component as any).updatePosition(known);
        expect(component.positions[0].quantity).toEqual(42);
        expect(component.positions.length).toEqual(positions.length);

        (component as any).updatePosition({ ...known, security: 'ZZZZ', quantity: 7 });
        expect(component.positions.length).toEqual(positions.length + 1);
        expect(component.positions[0].security).toEqual('ZZZZ');
    });

    it('should keep the state and security filters passed to the trade blotter', () => {
        component.onStateFilterChange(State.Pending);
        component.onSecurityFilterChange('aapl');
        expect(component.stateFilter).toEqual(State.Pending);
        expect(component.securityFilter).toEqual('aapl');
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
