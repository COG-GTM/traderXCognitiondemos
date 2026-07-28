import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { of } from 'rxjs';
import { PositionSummaryComponent } from './position-summary.component';
import { PositionService } from 'main/app/service/position.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';
import { MockTradeService, MockTradeFeedService, accounts as dummyAccounts } from 'main/app/test-utils/mocks.service';
import { Position } from 'main/app/model/trade.model';

const testPositions: Position[] = [
    { accountid: 1, quantity: 4300, security: 'AAPL', updated: new Date() },
    { accountid: 1, quantity: 3100, security: 'MSFT', updated: new Date() },
    { accountid: 1, quantity: -1500, security: 'TSLA', updated: new Date() },
    { accountid: 1, quantity: 0, security: 'IBM', updated: new Date() }
];

describe('PositionSummaryComponent', () => {
    let component: PositionSummaryComponent;
    let fixture: ComponentFixture<PositionSummaryComponent>;
    let positionService: PositionService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [PositionSummaryComponent],
            imports: [CommonModule],
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
        fixture = TestBed.createComponent(PositionSummaryComponent);
        component = fixture.componentInstance;
        positionService = TestBed.inject(PositionService);
        spyOn(positionService, 'getPositions').and.returnValue(of(testPositions));
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render zeroed metrics when no account is selected', () => {
        const values = fixture.nativeElement.querySelectorAll('.position-summary-value');
        expect(values.length).toEqual(4);
        expect([...values].map((value: HTMLElement) => value.innerText.trim())).toEqual(['0', '0', '0', '0']);
    });

    it('should fetch positions and subscribe to the feed on account change', () => {
        const testAccount = dummyAccounts[0];
        spyOn((component as any).tradeFeed, 'subscribe').and.callThrough();

        component.ngOnChanges({ account: { currentValue: testAccount } } as any);

        expect(positionService.getPositions).toHaveBeenCalledWith(testAccount.id);
        expect((component as any).tradeFeed.subscribe)
            .toHaveBeenCalledWith(`/accounts/${testAccount.id}/positions`, jasmine.any(Function));
    });

    it('should aggregate long, short and net quantities, ignoring flat positions', () => {
        component.ngOnChanges({ account: { currentValue: dummyAccounts[0] } } as any);
        fixture.detectChanges();

        expect(component.securitiesHeld).toEqual(3);
        expect(component.netQuantity).toEqual(5900);
        expect(component.longPositions).toEqual(2);
        expect(component.longQuantity).toEqual(7400);
        expect(component.shortPositions).toEqual(1);
        expect(component.shortQuantity).toEqual(1500);

        const values = fixture.nativeElement.querySelectorAll('.position-summary-value');
        expect([...values].map((value: HTMLElement) => value.innerText.trim())).toEqual(['3', '5,900', '2', '1']);
    });

    it('should update metrics from the position feed', () => {
        let feedCallback: Function = () => { };
        spyOn((component as any).tradeFeed, 'subscribe').and.callFake((topic: string, callback: Function) => {
            feedCallback = callback;
            return () => { };
        });

        component.ngOnChanges({ account: { currentValue: dummyAccounts[0] } } as any);
        feedCallback({ accountid: 1, quantity: -2500, security: 'AAPL', updated: new Date() });

        expect(component.netQuantity).toEqual(-900);
        expect(component.longPositions).toEqual(1);
        expect(component.shortPositions).toEqual(2);
        expect(component.shortQuantity).toEqual(4000);
    });

    it('should unsubscribe from the feed on destroy', () => {
        const unsubscribe = jasmine.createSpy('unsubscribe');
        spyOn((component as any).tradeFeed, 'subscribe').and.returnValue(unsubscribe);

        component.ngOnChanges({ account: { currentValue: dummyAccounts[0] } } as any);
        component.ngOnDestroy();

        expect(unsubscribe).toHaveBeenCalled();
    });
});
