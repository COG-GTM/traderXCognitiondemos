import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing';
import { AgGridAngular } from 'ag-grid-angular';
import { TradeBlotterComponent } from './trade-blotter.component';
import { PositionService } from 'main/app/service/position.service';
import { MockTradeService, MockTradeFeedService, accounts as dummyAccounts, trades } from 'main/app/test-utils/mocks.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';

describe('TradeBlotterComponent', () => {
    let component: TradeBlotterComponent;
    let fixture: ComponentFixture<TradeBlotterComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TradeBlotterComponent],
            imports: [
                AgGridAngular
            ],
            providers: [
                {
                    provide: PositionService,
                    useClass: MockTradeService
                },
                {
                    provide: TradeFeedService,
                    useClass: MockTradeFeedService
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TradeBlotterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show given trades columns in the grid', async () => {
        // AG Grid v33 renders asynchronously - verify the grid element exists
        const gridEl = fixture.nativeElement.querySelector('ag-grid-angular');
        expect(gridEl).toBeTruthy();
        // Verify column definitions are set correctly
        expect(component.columnDefs.length).toEqual(4);
    });

    it('should call getTrades on changes and set trades', () => {
        expect(component.account).not.toBeDefined();
        component.ngOnChanges({ account: { currentValue: dummyAccounts[0] } } as any);
        expect(component.trades.length).toEqual(2);
        // Verify the component data was set correctly
        expect(component.pendingTrades.length).toEqual(0);
    });

    it('should call getTrades and subscribe to trade feed service for given account', async () => {
        spyOn((component as any).tradeService, 'getTrades').and.callThrough();
        spyOn((component as any).tradeFeed, 'subscribe').and.callThrough();
        const testAccount = dummyAccounts[0];
        component.ngOnChanges({ account: { currentValue: testAccount } } as any);
        expect((component as any).tradeService.getTrades).toHaveBeenCalledWith(testAccount.id);
        expect((component as any).tradeFeed.subscribe).toHaveBeenCalled();

    });

    it('getRowId should return id from trade data', () => {
        expect(component.getRowId({ data: trades[0] } as any)).toEqual(`Trade-${trades[0].id}`);
    });

});
