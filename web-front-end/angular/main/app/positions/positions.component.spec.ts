import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { PositionsComponent } from './positions.component';
import { AccountService } from '../service/account.service';
import { PositionService } from '../service/position.service';
import { TradeFeedService } from '../service/trade-feed.service';
import { MockAccountService, MockTradeFeedService, MockTradeService, accounts, positions } from '../test-utils/mocks.service';
import { DropdownModule } from '../dropdown/dropdown.module';
import { Position } from '../model/trade.model';

describe('PositionsComponent', () => {
    let component: PositionsComponent;
    let fixture: ComponentFixture<PositionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                PositionsComponent
            ],
            imports: [
                DropdownModule
            ],
            providers: [
                {
                    provide: AccountService,
                    useClass: MockAccountService
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
        fixture = TestBed.createComponent(PositionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load accounts and select the first one on init', () => {
        expect(component.accounts.length).toEqual(5);
        expect(component.accountModel).toEqual(accounts[0]);
    });

    it('should get the positions of the selected account and subscribe to its positions feed', () => {
        spyOn((component as any).positionService, 'getPositions').and.callThrough();
        spyOn((component as any).tradeFeed, 'subscribe').and.callThrough();
        component.onAccountChange(accounts[1]);
        expect((component as any).positionService.getPositions).toHaveBeenCalledWith(accounts[1].id);
        expect((component as any).tradeFeed.subscribe)
            .toHaveBeenCalledWith(`/accounts/${accounts[1].id}/positions`, jasmine.any(Function));
        expect(component.positions).toEqual(positions);
        expect(component.positionSnapshot).toEqual(positions);
    });

    it('should update a known security and prepend an unknown one from the feed', () => {
        let feedCallback: (data: Position) => void = () => undefined;
        spyOn((component as any).tradeFeed, 'subscribe').and.callFake((topic: string, callback: (data: Position) => void) => {
            feedCallback = callback;
            return () => undefined;
        });
        component.onAccountChange(accounts[1]);

        feedCallback({ ...positions[0], quantity: 999 });
        expect(component.positions.length).toEqual(2);
        expect(component.positions.find((position) => position.security === positions[0].security)?.quantity).toEqual(999);

        feedCallback({ ...positions[0], security: 'NEWCO', quantity: 7 });
        expect(component.positions.length).toEqual(3);
        expect(component.positions[0].security).toEqual('NEWCO');
    });

    it('should tear down the feed subscription on account change and on destroy', () => {
        const unSubscribe = jasmine.createSpy('unSubscribe');
        spyOn((component as any).tradeFeed, 'subscribe').and.returnValue(unSubscribe);
        component.onAccountChange(accounts[1]);
        expect(unSubscribe).not.toHaveBeenCalled();

        component.onAccountChange(accounts[2]);
        expect(unSubscribe).toHaveBeenCalledTimes(1);

        component.ngOnDestroy();
        expect(unSubscribe).toHaveBeenCalledTimes(2);
    });

    it('should render the account name in the heading', () => {
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('#positionsHeading').innerText)
            .toEqual(`Positions (${accounts[0].displayName})`);
    });
});
