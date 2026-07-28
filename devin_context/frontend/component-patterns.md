# Component patterns

Skeletons extracted from the golden files. Copy them.

## Container / child split

Every feature page is a **container**: it injects the services the page needs, owns the page-level
selection, opens modals and performs **all writes**. `trade.component.ts` is the reference.

Children come in two flavours, and the split is real — don't blur it:

1. **Presentational** — `@Input()` in, `@Output()` out, **injects nothing**: `trade-ticket`,
   `dropdown`, `button-renderer`. A form child emits its payload; the container calls the service.
2. **Self-fetching blotter** — takes the account as an `@Input()` and injects **read-only** data
   sources (`PositionService`, `TradeFeedService`) to load its own snapshot and subscribe to its
   own feed topic: `trade-blotter`, `position-blotter`. This is deliberate: a blotter owns a live
   stream keyed to its own topic, and pushing every tick through the container would serialise
   two independent feeds.

The hard line is **writes**: no child posts, creates or updates. `accounts/edit` and
`accounts/user/assign-user` currently break that (they inject `AccountService` and post) — they're
legacy, not a pattern to copy.

```ts
// container
export class TradeComponent implements OnInit {
    accounts: Account[] = [];
    accountModel?: Account = undefined;

    constructor(private accountService: AccountService,
        private symbolService: SymbolService,
        private modalService: BsModalService) { }

    ngOnInit(): void {
        this.accountService.getAccounts().subscribe((accounts) => {
            this.accounts = accounts;
            this.setAccount(this.accounts[5]);
        });
    }
}
```

```ts
// presentational child
export class TradeTicketComponent implements OnInit {
    @Input() stocks: Stock[];
    @Input() account: Account | undefined;
    @Output() create = new EventEmitter<TradeTicket>();
    @Output() cancel = new EventEmitter();
}
```

A presentational child never calls a service and never navigates. It emits; the container decides.
A blotter child may read and subscribe, but still never writes and never navigates.

## Feature module

```ts
@NgModule({
  declarations: [TradeComponent, TradeTicketComponent, TradeBlotterComponent, PositionBlotterComponent],
  imports: [CommonModule, AgGridModule, FormsModule, ModalModule.forRoot(), AlertModule.forRoot(), DropdownModule],
  exports: [TradeComponent]
})
export class TradeModule { }
```

Import only the ngx-bootstrap modules the feature actually uses. Then add the route in
`main/app/routing.ts` **and** the tab in `header/header.component.html` — a route without a tab is
unreachable:

```ts
{ path: 'blotter', component: BlotterComponent },
```

```html
<li class="nav-item">
    <a class="nav-link" routerLink="/blotter" routerLinkActive="active">Blotter</a>
</li>
```

## AG Grid blotter

```ts
columnDefs: ColDef[] = [
    { headerName: 'SECURITY', field: 'security' },
    { headerName: 'QUANTITY', field: 'quantity' },
    { headerName: 'SIDE',     field: 'side' },
    { headerName: 'STATE',    field: 'state', enableCellChangeFlash: true }
];

onGridReady(params: GridReadyEvent) {
    this.gridApi = params.api;
    this.gridApi.sizeColumnsToFit();
}

getRowId(params: GetRowIdParams<any>): string {
    return `Trade-${params.data.id}`;
}
```

```html
<h5>Trades</h5>
<ag-grid-angular style="width: 100%; height: 350px;" class="ag-theme-alpine"
    [columnDefs]="columnDefs" [rowData]="trades"
    (gridReady)="onGridReady($event)" [getRowId]="getRowId">
</ag-grid-angular>
```

- `columnDefs` is a typed `ColDef[]` field, declared in the class, never built in the template.
- Row data arrives either as a bound array (`[rowData]="trades"`) or via the `async` pipe
  (`[rowData]="accounts$ | async"`). Both are in use; pick the one that matches how the data is
  fetched (see `data-and-state.md`).
- **Incremental updates go through `applyTransaction`, never through reassigning the array** —
  and look the row up with the **same `getRowId` you gave the grid**, not the raw domain id, or
  every update silently becomes an insert:

```ts
const row = this.gridApi.getRowNode(this.getRowId({ data } as GetRowIdParams<Trade>));
this.gridApi.applyTransaction(row
    ? { update: [Object.assign(row.data, { state: data.state })] }
    : { add: [{ ...data }], addIndex: 0 });
```

- If the grid can receive feed messages before its snapshot resolves, buffer them — the
  `isPending` / `pendingTrades` pair in `trade-blotter.component.ts` is the pattern, and it exists
  because dropping that race loses trades.

## Grid cell renderer

A component implementing `ICellRendererAngularComp`, with an inline one-line template, wired by
name:

```ts
type ICellParams = ICellRendererParams & { clicked: (val: any) => void };

@Component({
  selector: 'app-btn-cell-renderer',
  template: `<button class="btn btn-sm btn-info" (click)="clickHandler()">Update</button>`
})
export class ButtonCellRendererComponent implements ICellRendererAngularComp {
  agInit(params: ICellParams): void { this.params = params; }
  clickHandler() { this.params.clicked(this.params.data); }
  refresh(params: ICellParams) { return false; }
}
```

```ts
frameworkComponents = { btnCellRenderer: ButtonCellRendererComponent };
// ColDef: { headerName: 'Update', cellRenderer: 'btnCellRenderer', cellRendererParams: { clicked: (a) => … } }
```

## Modal (ngx-bootstrap)

Modals are `TemplateRef`s opened by the container — no routed modals, no bespoke overlay:

```ts
openTicket(template: TemplateRef<any>) { this.modalRef = this.modalService.show(template); }
closeTicket() { this.modalRef?.hide(); }
```

```html
<button type="button" id="createTicketBtn" class="btn btn-sm btn-primary mb-2" (click)="openTicket(ticketComponent)">
    Create Trade Ticket
</button>
<ng-template #ticketComponent>
    <app-trade-ticket [stocks]="stocks" [account]="accountModel"
        (create)="createTradeTicket($event)" (cancel)="closeTicket()"></app-trade-ticket>
</ng-template>
```

## Feedback

Success/failure feedback is an ngx-bootstrap `<alert>` bound to a response field, self-dismissing:

```html
<alert *ngIf="createTicketResponse" [type]="createTicketResponse.success ? 'success' : 'danger'"
       [dismissible]="true" [dismissOnTimeout]="2000" (onClosed)="onCloseAlert()">
    {{ createTicketResponse | json }}
</alert>
```

No `window.alert`, no toast library, no snackbar.

## Selects

Use the shared `app-ngx-dropdown` (`dropdown/dropdown.component.ts`) rather than a raw `<select>`:

```html
<app-ngx-dropdown name="account" [items]="accounts" itemKey="displayName"
    [selectedItem]="accountModel" placeholder="Select Account"
    (selectedItemChange)="onAccountChange($event)"></app-ngx-dropdown>
```

For free-text search over a list, use the ngx-bootstrap typeahead as in
`trade-ticket.component.html` (`[typeahead]`, `typeaheadOptionField`, `(typeaheadOnSelect)`), and
clear the bound model on blur when nothing was picked.

## Forms

Template-driven with `[(ngModel)]` and `FormsModule`. **Reactive forms are not used in this app** —
don't introduce them for one screen. Validate in the handler and bail early, the way
`onCreate()` does:

```ts
onCreate() {
    if (!this.ticket.security || !this.ticket.quantity) { return; }
    this.create.emit(this.ticket);
}
```
