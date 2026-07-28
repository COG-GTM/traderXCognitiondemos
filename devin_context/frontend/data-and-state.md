# Data and state

## Services are the only network boundary

One service per backend service, `@Injectable({ providedIn: 'root' })`, URLs from `environment`.
**No component ever calls `HttpClient`, builds a URL, or opens a socket.**

```ts
@Injectable({ providedIn: 'root' })
export class AccountService {
  private baseUrl = environment.accountUrl;

  constructor(private http: HttpClient) { }

  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/account/`, this.httpOptions).pipe(
      retry(2),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    console.error(error);
    return throwError(() => error);
  }
}
```

Rules, all visible in `account.service.ts` / `position.service.ts` / `symbols.service.ts`:

- Every method returns a **typed** `Observable<T>` off a model in `main/app/model/`.
- Every method ends with `catchError(this.handleError)`; `handleError` logs and re-throws with
  `throwError(() => error)`. Never swallow.
- Idempotent GETs that the page can't function without get `retry(2)` (accounts, stocks). Writes
  never retry.
- URLs come from `main/environments/environment.ts` — add a new service URL there (and in
  `.local.ts` / `.prod.ts`) rather than hard-coding a host or port.

Existing endpoints, so you don't add a service that already exists:

| Service | Methods |
| --- | --- |
| `AccountService` | `getAccounts()`, `addAccount()`, `getAccountUsers()`, `addAccountUser()` |
| `PositionService` | `getTrades(accountId)`, `getPositions(accountId)` |
| `SymbolService` | `getStocks()`, `createTicket(ticket)` |
| `UserService` | user lookup for account assignment |
| `TradeFeedService` | `subscribe(topic, cb)` / `unSubscribe(topic, cb)` |

## Component state

Local fields on the container. There is **no store** — no NgRx, no service-held global state
beyond `ThemeService`. Two accepted shapes:

**1. Imperative `subscribe` into a field** — when the value feeds a grid you then mutate, or when
you need it in TypeScript:

```ts
this.tradeService.getTrades(accountId).subscribe((trades) => { this.trades = trades; });
```

**2. `Observable$` + `async` pipe** — when the template can consume it directly. Refresh by
pushing into a `BehaviorSubject` and `switchMap`ping, exactly as `account.component.ts` does:

```ts
accountBehaviorSubject = new BehaviorSubject(0);
accountAddAction$ = this.accountBehaviorSubject.asObservable();

this.accounts$ = this.accountAddAction$.pipe(
    debounceTime(200),
    switchMap(() => this.accountService.getAccounts())
);
```

After a mutation, **re-trigger the subject** (`this.accountBehaviorSubject.next(id)`) instead of
patching the local array by hand.

Prefer (2) for read-only lists; (1) is required for the blotters because AG Grid owns the rows
after the first paint.

## Reacting to an input change

Blotters take the selected account as an `@Input()` and refetch in `ngOnChanges`, guarding against
the no-op change:

```ts
ngOnChanges(change: SimpleChanges) {
    if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
        // refetch + resubscribe
    }
}
```

## The trade feed

`TradeFeedService` wraps one socket.io connection. Topics are path-shaped:
`/accounts/${accountId}/trades`, `/accounts/${accountId}/positions`.

`subscribe()` **returns its own teardown function** — keep it and call it both when the input
changes and in `ngOnDestroy`. Leaking a subscription means the previous account's trades keep
landing in the grid:

```ts
this.socketUnSubscribeFn?.();
this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/trades`, (data: Trade) => {
    this.updateTrades(data);
});

ngOnDestroy() { this.socketUnSubscribeFn?.(); }
```

Messages from `System` are filtered inside the service; your callback only sees payloads for its
topic.

## Error handling

Services log and re-throw. Components either let it surface (blotters) or bind the response into
an `<alert>` (trade ticket). Don't add a global `HttpInterceptor`, an error page, or a retry loop
in a component.

## What the API does *not* give you

Worth knowing before you promise a number in a design:

- Trades and positions carry `security`, `quantity`, `side`, `state`, `accountid`, timestamps —
  **no price, no notional, no P&L**. Nothing in the UI can compute market value or P&L today.
- Reference data is ticker + company name only; no sector, no exchange, no last price.
- There is no per-user or historical/time-series endpoint.

If a design needs one of these, don't fabricate it — say so in the PR and leave the element out.
