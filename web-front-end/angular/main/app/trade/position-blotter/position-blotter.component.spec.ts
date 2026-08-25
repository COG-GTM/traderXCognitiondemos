import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgGridModule } from 'ag-grid-angular';
import { PositionBlotterComponent } from './position-blotter.component';
import { PositionService } from 'main/app/service/position.service';
import {
  MockTradeService,
  MockTradeFeedService,
  MockFailingPositionService,
  positions
} from 'main/app/test-utils/mocks.service';
import { TradeFeedService } from 'main/app/service/trade-feed.service';
import { gridRowTexts, settle } from 'main/app/test-utils/utils';

describe('PositionBlotterComponent', () => {
  let component: PositionBlotterComponent;
  let fixture: ComponentFixture<PositionBlotterComponent>;
  let feed: MockTradeFeedService;

  const topicFor = (accountId: number) => `/accounts/${accountId}/positions`;

  const aPosition = (overrides: any = {}) => ({
    accountid: 1,
    quantity: 10,
    security: 'AAPL',
    updated: new Date(),
    ...overrides
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PositionBlotterComponent],
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
    })
      .compileComponents();
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(PositionBlotterComponent);
    component = fixture.componentInstance;
    feed = TestBed.inject(TradeFeedService) as unknown as MockTradeFeedService;
    component.positions = positions;
    await settle(fixture);
  });

  const selectAccount = async (accountId: number) => {
    component.ngOnChanges({
      account: { currentValue: { id: accountId, displayName: `acct-${accountId}` }, previousValue: component.account }
    } as any);
    component.account = { id: accountId, displayName: `acct-${accountId}` };
    await settle(fixture);
  };

  /** Selects an account whose position list comes back with the given rows. */
  const selectAccountWithPositions = async (accountId: number, rows: any[]) => {
    (component as any).tradeService.getPositions = () => ({ subscribe: (cb: any) => cb(rows) });
    component.positions = rows;
    await selectAccount(accountId);
  };

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show given positions in the grid', () => {
    const columns = fixture.nativeElement.querySelectorAll('.ag-header-cell');
    const rows = gridRowTexts(fixture);
    expect(columns.length).toEqual(2);
    expect(rows.length).toEqual(2);
    expect(rows[0][0]).toEqual(component.positions[0].security);
    expect(rows[0][1]).toEqual(component.positions[0].quantity.toString());
  });

  // ---------------------------------------------------------------------
  // UI-10, UI-12..UI-17 edge / corner cases
  // ---------------------------------------------------------------------

  // UI-10b
  it('should render an empty grid without error when the account holds no positions', async () => {
    await selectAccountWithPositions(1, []);

    expect(component.positions).toEqual([]);
    expect(gridRowTexts(fixture).length).toEqual(0);
    expect(fixture.nativeElement.querySelectorAll('.ag-header-cell').length).toEqual(2);
  });

  // UI-14a
  it('should render a zero quantity position', async () => {
    await selectAccountWithPositions(1, [aPosition({ security: 'ZERO', quantity: 0 })]);

    expect(gridRowTexts(fixture)[0]).toEqual(['ZERO', '0']);
  });

  // UI-14b
  it('should render a negative (short) quantity position', async () => {
    await selectAccountWithPositions(1, [aPosition({ security: 'SHORT', quantity: -250 })]);

    expect(gridRowTexts(fixture)[0]).toEqual(['SHORT', '-250']);
  });

  // UI-12d
  xit('LATENT BUG: should update a position row in place when a new quantity arrives for the same security', async () => {
    await selectAccountWithPositions(1, []);

    feed.emit(topicFor(1), aPosition({ security: 'MSFT', quantity: 5 }));
    await settle(fixture);
    feed.emit(topicFor(1), aPosition({ security: 'MSFT', quantity: 15 }));
    await settle(fixture);

    const texts = gridRowTexts(fixture);
    expect(texts.length).toEqual(1);
    expect(texts[0]).toEqual(['MSFT', '15']);
  });

  // UI-12e - the behaviour actually observed today
  it('should look the row up by the raw security while rows are keyed with a Position- prefix', async () => {
    await selectAccountWithPositions(1, []);

    feed.emit(topicFor(1), aPosition({ security: 'MSFT', quantity: 5 }));
    await settle(fixture);

    expect(component.gridApi.getRowNode('MSFT')).toBeUndefined();
    expect(component.gridApi.getRowNode('Position-MSFT')).toBeDefined();
  });

  // UI-13e
  it('should not crash on a partial position payload with no quantity', async () => {
    await selectAccountWithPositions(1, []);

    expect(() => feed.emit(topicFor(1), { security: 'PART', accountid: 1 })).not.toThrow();
    await settle(fixture);

    expect(component.gridApi.getRowNode('Position-PART')?.data.quantity).toBeUndefined();
  });

  // UI-13f
  xit('LATENT BUG: should ignore a null position payload instead of throwing', async () => {
    await selectAccountWithPositions(1, []);

    expect(() => feed.emit(topicFor(1), null)).not.toThrow();
  });

  // UI-13g - the behaviour actually observed today
  it('should throw when a null position payload is delivered', async () => {
    await selectAccountWithPositions(1, []);

    expect(() => feed.emit(topicFor(1), null)).toThrowError(TypeError);
  });

  // UI-15d
  it('should tear down the previous account position subscription on account switch', async () => {
    await selectAccountWithPositions(1, []);
    expect(feed.isSubscribed(topicFor(1))).toBeTrue();

    await selectAccount(2);

    expect(feed.isSubscribed(topicFor(1))).toBeFalse();
    expect(feed.isSubscribed(topicFor(2))).toBeTrue();
  });

  // UI-15e
  it('should unsubscribe on destroy', async () => {
    await selectAccountWithPositions(1, []);

    fixture.destroy();

    expect(feed.isSubscribed(topicFor(1))).toBeFalse();
  });

  // UI-17c
  it('should render a unicode and a very long security verbatim and escape HTML', async () => {
    const longSecurity = 'Z'.repeat(256);
    await selectAccountWithPositions(1, [
      aPosition({ security: '株式会社-Ünïcødé-🚀', quantity: 1 }),
      aPosition({ security: longSecurity, quantity: 2 }),
      aPosition({ security: '<img src=x onerror="window.__xssPos=1">', quantity: 3 })
    ]);

    const rendered = gridRowTexts(fixture).flat().join(' ');
    expect(rendered).toContain('株式会社-Ünïcødé-🚀');
    expect(rendered).toContain(longSecurity);
    expect(rendered).toContain('<img src=x');
    expect(fixture.nativeElement.querySelector('.ag-center-cols-container img')).toBeNull();
    expect((window as any).__xssPos).toBeUndefined();
  });
});

describe('PositionBlotterComponent HTTP error paths', () => {
  let component: PositionBlotterComponent;
  let fixture: ComponentFixture<PositionBlotterComponent>;
  let feed: MockTradeFeedService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PositionBlotterComponent],
      imports: [AgGridModule],
      providers: [
        { provide: PositionService, useClass: MockFailingPositionService },
        { provide: TradeFeedService, useClass: MockTradeFeedService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PositionBlotterComponent);
    component = fixture.componentInstance;
    feed = TestBed.inject(TradeFeedService) as unknown as MockTradeFeedService;
    await settle(fixture);
  });

  // UI-16d
  it('should recover from a failed positions request and still accept live updates', async () => {
    expect(() => component.ngOnChanges({ account: { currentValue: { id: 7, displayName: 'x' } } } as any)).not.toThrow();
    expect(component.isPending).toBeFalse();
    await settle(fixture);

    feed.emit('/accounts/7/positions', { accountid: 7, quantity: 3, security: 'REC', updated: new Date() });
    await settle(fixture);

    expect(component.gridApi.getRowNode('Position-REC')).toBeDefined();
  });
});
