# Testing and selectors

## Every component gets a spec

`<name>.component.spec.ts` sits beside the component. Karma + Jasmine, run with:

```bash
npm --prefix web-front-end/angular run test:ci     # ChromeHeadlessNoSandbox, single run, coverage
```

The shape (from `trade-blotter.component.spec.ts`):

```ts
describe('TradeBlotterComponent', () => {
    let component: TradeBlotterComponent;
    let fixture: ComponentFixture<TradeBlotterComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TradeBlotterComponent],
            imports: [AgGridModule],
            providers: [
                { provide: PositionService,  useClass: MockTradeService },
                { provide: TradeFeedService, useClass: MockTradeFeedService }
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
});
```

Rules:

- **Never hit the network in a spec.** Provide a mock class from
  `main/app/test-utils/mocks.service.ts` (`MockTradeService`, `MockTradeFeedService`, and the
  `accounts` / `trades` fixtures) with `{ provide: RealService, useClass: MockService }`. Extend
  that file rather than declaring ad-hoc mocks in a spec.
- Start with `should create`, then one `it` per behaviour, named as a sentence
  (`should call getTrades on changes and set trades`).
- Assert against the **rendered DOM** via `fixture.nativeElement.querySelectorAll(...)`, not just
  component fields — the existing specs count `.ag-header-cell` / `.ag-row` to prove the grid is
  wired.
- Grid rendering is async: `fakeAsync` + `tick(100)` after `fixture.detectChanges()`, as the
  blotter spec does.
- `@faker-js/faker` is available for fixture data.

## Selectors: `id`, not `data-testid`

This codebase's stable hooks are plain `id` attributes on the elements a test or a demo script
drives. Keep adding them in the same style — lowerCamelCase, or kebab-case where the existing
neighbour is kebab-case:

| Element | Existing id |
| --- | --- |
| Open trade ticket | `createTicketBtn` |
| Security typeahead | `stock-input` |
| Buy / Sell toggles | `buyButton` / `sellButton` (these ids are also style hooks — see `styles.scss`) |
| Quantity field | `quantityField` |
| Submit / cancel ticket | `createButton` / `cancelButton` |
| Account label | `accountLabel` |
| Grids | `accountgrid`, `usergrid` |
| Generated dropdowns | `drp<n>` / `drpbtn<n>` from `dropdown.component.ts` |

Give every new interactive control and every new grid an `id`. Don't introduce `data-testid`
alongside these — one convention, consistently, is worth more than the better convention applied
twice.

## Accessibility floor

Not audited today, but don't regress it:

- Every form control has a `<label class="col-sm-2 col-form-label">` in its `mb-3 row`, or an
  `aria-label` if the design has no visible label.
- Buttons carry an explicit `type` (`type="button"` unless they submit a form).
- Nav uses `routerLink` anchors inside `nav-item`/`nav-link`, with `routerLinkActive="active"`.
- Dropdowns keep the `role="menu"` / `role="menuitem"` / `aria-labelledby` wiring that
  `dropdown.component.html` already has.
- Never convey state by colour alone — the buy/sell colours are paired with the words Buy/Sell.
