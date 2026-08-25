import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgGridModule } from 'ag-grid-angular';
import { TradeBlotterComponent } from './trade-blotter.component';
import { PositionService } from 'main/app/service/position.service';
import {
    MockTradeService,
    MockTradeFeedService,
    MockHangingPositionService,
    accounts as dummyAccounts,
    trades
} from 'main/app/test-utils/mocks.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';
import { State, Side, Trade } from 'main/app/model/trade.model';
import { gridRowTexts, settle } from 'main/app/test-utils/utils';

describe('TradeBlotterComponent', () => {
    let component: TradeBlotterComponent;
    let fixture: ComponentFixture<TradeBlotterComponent>;
    let feed: MockTradeFeedService;

    const topicFor = (accountId: number) => `/accounts/${accountId}/trades`;

    const aTrade = (overrides: Partial<Trade> = {}): Trade => ({
        accountid: dummyAccounts[0].id,
        created: new Date(),
        id: 'trade-1',
        quantity: 100,
        security: 'AAPL',
        side: Side.Buy,
        state: State.New,
        updated: new Date(),
        ...overrides
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TradeBlotterComponent],
            imports: [
                AgGridModule
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

    beforeEach(async () => {
        fixture = TestBed.createComponent(TradeBlotterComponent);
        component = fixture.componentInstance;
        feed = TestBed.inject(TradeFeedService) as unknown as MockTradeFeedService;
        await settle(fixture);
    });

    /** Drive the @Input() change the same way the parent template does. */
    const selectAccount = async (accountId: number) => {
        component.ngOnChanges({
            account: { currentValue: { id: accountId, displayName: `acct-${accountId}` }, previousValue: component.account }
        } as any);
        component.account = { id: accountId, displayName: `acct-${accountId}` };
        await settle(fixture);
    };

    /** An account whose trade list comes back empty, so the grid starts clean. */
    const selectAccountWithNoTrades = async (accountId: number) => {
        (component as any).tradeService.getTrades = () => ({ subscribe: (cb: any) => cb([]) });
        await selectAccount(accountId);
    };

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show given trades columns in the grid', () => {
        const columns = fixture.nativeElement.querySelectorAll('.ag-header-cell');
        const rows = fixture.nativeElement.querySelectorAll('.ag-center-cols-container .ag-row');
        expect(columns.length).toEqual(4);
        expect(rows.length).toEqual(0);
    });

    it('should call getTrades on changes and set trades', async () => {
        expect(component.account).not.toBeDefined();
        await selectAccount(dummyAccounts[0].id);

        expect(component.trades.length).toEqual(2);
        expect(gridRowTexts(fixture).length).toEqual(2);
        expect(component.pendingTrades.length).toEqual(0);
    });

    it('should call getTrades and subscribe to trade feed service for given account', () => {
        spyOn((component as any).tradeService, 'getTrades').and.callThrough();
        spyOn((component as any).tradeFeed, 'subscribe').and.callThrough();
        const testAccount = dummyAccounts[0];
        component.ngOnChanges({ account: { currentValue: testAccount } } as any);
        expect((component as any).tradeService.getTrades).toHaveBeenCalledWith(testAccount.id);
        expect((component as any).tradeFeed.subscribe).toHaveBeenCalled();
    });

    it('getRowId should return a prefixed id from trade data', () => {
        expect(component.getRowId({ data: trades[0] } as any)).toEqual(`Trade-${trades[0].id}`);
    });

    // ---------------------------------------------------------------------
    // UI-10..UI-17 edge / corner cases
    // ---------------------------------------------------------------------

    // UI-10
    it('should render an empty grid without error when the account has no trades', async () => {
        (component as any).tradeService.getTrades = () => ({ subscribe: (cb: any) => cb([]) });

        await expectAsync(selectAccount(1)).toBeResolved();

        expect(component.trades).toEqual([]);
        expect(gridRowTexts(fixture).length).toEqual(0);
        expect(fixture.nativeElement.querySelectorAll('.ag-header-cell').length).toEqual(4);
    });

    // UI-11
    xit('LATENT BUG: should ignore a trade-feed payload belonging to a different accountId', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'foreign-1', accountid: 999, security: 'FOREIGN' }));
        await settle(fixture);

        expect(gridRowTexts(fixture).length).toEqual(0);
    });

    // UI-11b - the behaviour actually observed today
    it('should add a trade-feed payload to the grid without checking its accountId', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'foreign-1', accountid: 999, security: 'FOREIGN' }));
        await settle(fixture);

        const texts = gridRowTexts(fixture);
        expect(texts.length).toEqual(1);
        expect(texts[0]).toContain('FOREIGN');
    });

    // UI-12
    xit('LATENT BUG: should update an existing trade row in place when a later state arrives', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'dup-1', state: State.New }));
        await settle(fixture);
        feed.emit(topicFor(1), aTrade({ id: 'dup-1', state: State.Settled }));
        await settle(fixture);

        const texts = gridRowTexts(fixture);
        expect(texts.length).toEqual(1);
        expect(texts[0]).toContain('Settled');
    });

    // UI-12b - the behaviour actually observed today
    it('should never match an existing row because getRowNode is queried without the row id prefix', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'dup-1', state: State.New }));
        await settle(fixture);

        expect(component.gridApi.getRowNode('dup-1')).toBeUndefined();
        expect(component.gridApi.getRowNode('Trade-dup-1')).toBeDefined();
    });

    // UI-12c
    xit('LATENT BUG: should not regress to an earlier state when an out-of-order update arrives', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'ooo-1', state: State.Settled }));
        await settle(fixture);
        feed.emit(topicFor(1), aTrade({ id: 'ooo-1', state: State.New }));
        await settle(fixture);

        expect(component.gridApi.getRowNode('Trade-ooo-1')?.data.state).toEqual(State.Settled);
    });

    // UI-12f - the behaviour actually observed today
    it('should add a duplicate row and show the stale state when an out-of-order update arrives', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'ooo-1', state: State.Settled }));
        await settle(fixture);
        feed.emit(topicFor(1), aTrade({ id: 'ooo-1', state: State.New }));
        await settle(fixture);

        expect(component.gridApi.getRowNode('Trade-ooo-1')?.data.state).toEqual(State.New);
        expect(gridRowTexts(fixture).length).toEqual(2);
    });

    // UI-13a
    it('should not crash on a partial trade payload with no quantity or state', async () => {
        await selectAccountWithNoTrades(1);

        expect(() => feed.emit(topicFor(1), { id: 'partial-1', security: 'PART', accountid: 1 } as any)).not.toThrow();
        await settle(fixture);

        const node = component.gridApi.getRowNode('Trade-partial-1');
        expect(node?.data.quantity).toBeUndefined();
        expect(node?.data.state).toBeUndefined();
    });

    // UI-13b
    it('should add a row with an undefined id when the payload has no id', async () => {
        await selectAccountWithNoTrades(1);

        expect(() => feed.emit(topicFor(1), { security: 'NOID', quantity: 1 } as any)).not.toThrow();
        await settle(fixture);

        expect(component.gridApi.getRowNode('Trade-undefined')).toBeDefined();
    });

    // UI-13c
    xit('LATENT BUG: should ignore a null trade-feed payload instead of throwing', async () => {
        await selectAccountWithNoTrades(1);

        expect(() => feed.emit(topicFor(1), null)).not.toThrow();
    });

    // UI-13d - the behaviour actually observed today
    it('should throw when a null trade-feed payload is delivered', async () => {
        await selectAccountWithNoTrades(1);

        expect(() => feed.emit(topicFor(1), null)).toThrowError(TypeError);
    });

    // UI-15
    it('should tear down the previous account subscription when the account changes', async () => {
        await selectAccountWithNoTrades(1);
        expect(feed.isSubscribed(topicFor(1))).toBeTrue();

        await selectAccount(2);

        expect(feed.isSubscribed(topicFor(1))).toBeFalse();
        expect(feed.isSubscribed(topicFor(2))).toBeTrue();
        expect(feed.unSubscribedTopics).toContain(topicFor(1));
    });

    // UI-15b
    it('should not leak the subscription when the component is destroyed', async () => {
        await selectAccountWithNoTrades(1);

        fixture.destroy();

        expect(feed.isSubscribed(topicFor(1))).toBeFalse();
    });

    // UI-15c
    it('should not react to a feed message for the account that was switched away from', async () => {
        await selectAccountWithNoTrades(1);
        await selectAccount(2);
        const before = gridRowTexts(fixture).length;

        feed.emit(topicFor(1), aTrade({ id: 'stale-1', security: 'STALE' }));
        await settle(fixture);

        expect(gridRowTexts(fixture).length).toEqual(before);
    });

    // UI-17a
    it('should render a unicode and a very long ticker verbatim', async () => {
        await selectAccountWithNoTrades(1);
        const longTicker = 'A'.repeat(256);

        feed.emit(topicFor(1), aTrade({ id: 'uni-1', security: '株式会社-Ünïcødé-🚀' }));
        feed.emit(topicFor(1), aTrade({ id: 'long-1', security: longTicker }));
        await settle(fixture);

        expect(component.gridApi.getRowNode('Trade-uni-1')?.data.security).toEqual('株式会社-Ünïcødé-🚀');
        expect(component.gridApi.getRowNode('Trade-long-1')?.data.security).toEqual(longTicker);
        expect(gridRowTexts(fixture).flat().join(' ')).toContain('株式会社-Ünïcødé-🚀');
    });

    // UI-17b
    it('should escape HTML delivered in a security name rather than rendering it', async () => {
        await selectAccountWithNoTrades(1);

        feed.emit(topicFor(1), aTrade({ id: 'xss-1', security: '<img src=x onerror="window.__xss=1">' }));
        await settle(fixture);

        expect(fixture.nativeElement.querySelector('.ag-center-cols-container img')).toBeNull();
        expect((window as any).__xss).toBeUndefined();
        expect(gridRowTexts(fixture).flat().join(' ')).toContain('<img src=x');
    });
});

describe('TradeBlotterComponent slow / failing trades request', () => {
    let component: TradeBlotterComponent;
    let fixture: ComponentFixture<TradeBlotterComponent>;
    let feed: MockTradeFeedService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TradeBlotterComponent],
            imports: [AgGridModule],
            providers: [
                { provide: PositionService, useClass: MockHangingPositionService },
                { provide: TradeFeedService, useClass: MockTradeFeedService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TradeBlotterComponent);
        component = fixture.componentInstance;
        feed = TestBed.inject(TradeFeedService) as unknown as MockTradeFeedService;
        await settle(fixture);
    });

    // UI-16a
    it('should queue live updates instead of crashing while the trades request has not answered', async () => {
        component.ngOnChanges({ account: { currentValue: { id: 7, displayName: 'x' } } } as any);
        await settle(fixture);

        expect(() => feed.emit('/accounts/7/trades', {
            accountid: 7, created: new Date(), id: 'queued', quantity: 1,
            security: 'REC', side: Side.Buy, state: State.New, updated: new Date()
        })).not.toThrow();

        expect(component.isPending).toBeTrue();
        expect(component.pendingTrades.length).toEqual(1);
        expect(gridRowTexts(fixture).length).toEqual(0);
    });

    // UI-16b
    xit('LATENT BUG: should stop showing the blotter as pending when the trades request never answers or fails', async () => {
        component.ngOnChanges({ account: { currentValue: { id: 7, displayName: 'x' } } } as any);
        await settle(fixture);

        expect(component.isPending).toBeFalse();
    });

    // UI-16c - the behaviour actually observed today
    it('should stay stuck in the pending state when the trades request never answers', async () => {
        component.ngOnChanges({ account: { currentValue: { id: 7, displayName: 'x' } } } as any);
        await settle(fixture);

        expect(component.isPending).toBeTrue();
    });
});
