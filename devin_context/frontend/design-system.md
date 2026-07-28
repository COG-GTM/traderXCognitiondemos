# Design system

There is no bespoke design system here, and that's deliberate: **Bootstrap 5 is the design system**,
AG Grid owns anything tabular, and `main/styles.scss` holds the handful of brand overrides. Compose
utilities; don't write CSS.

## Source of truth

`main/styles.scss` — all 32 lines of it:

```scss
@import 'bootstrap/dist/css/bootstrap.css';
@import 'ag-grid-community/styles/ag-grid.css';
@import 'ag-grid-community/styles/ag-theme-alpine.css';
```

plus these overrides, which are the entire brand palette:

| Rule | Value | Meaning |
| --- | --- | --- |
| `.btn-primary` | `#044fbd` (hover `#0d6efd`) | TraderX blue — primary actions |
| `.btn-info` | `#0d6efd` (hover `#3583f9`) | lighter blue — secondary/inline actions |
| `#buyButton` | `green` | buy side |
| `#sellButton` | `red` | sell side |
| `.drpbtn3` | `margin-left: 5px` | one-off dropdown spacing |

**Never introduce a hex value in a component.** Buy/sell are the only semantic colours in the app
and they are already spoken for; everything else is a Bootstrap contextual class.

## Themes — know what's real

`ThemeService` toggles `document.documentElement.className` between `professional-dark` and
`professional-light` and swaps a `#theme-tag` stylesheet. **None of that is currently wired up**:
`index.html` has no `#theme-tag`, no `professional-*.css` ship with the app, and
`header.component.html` never emits the `switchTheme` output the shell listens for. The app always
renders the default Bootstrap light appearance; the only dark surface is the navbar
(`text-bg-primary` + `data-bs-theme="dark"`).

So: **build for default Bootstrap**, and use contextual classes (`bg-body-tertiary`,
`text-bg-secondary`, `btn-secondary`) rather than fixed colours, so the day someone finishes the
theme work your screen comes along for free. Don't "fix" the theme switcher as a side effect of a
feature — it's its own ticket.

## Button semantics

Consistency here is more visible than anything else in the UI:

| Class | Use | Seen in |
| --- | --- | --- |
| `btn btn-sm btn-primary` | the primary action of a screen or form | `Create Trade Ticket`, `Create`, dropdown toggle |
| `btn btn-sm btn-info` | secondary/inline action, including grid row actions | `Update` cell renderer |
| `btn btn-sm btn-secondary` | cancel / dismiss, and the unselected state of a toggle | `Close`, unselected Buy/Sell |
| `btn btn-warning btn-sm` | the selected *Sell* toggle | trade ticket |

`btn-sm` is effectively mandatory — there are no default-size buttons in the app. One
`btn-primary` per screen section.

## Layout and spacing

Bootstrap utilities only. The vocabulary actually in use:

- Page padding: `p-5` (`trade`), `p-5 pt-3` (`accounts`).
- Vertical rhythm: `mb-2` / `mb-3` / `mb-4`, `mt-3` / `mt-4`.
- Horizontal gap: `me-2` / `me-3`.
- Flex: `d-flex`, `flex-column`, `justify-content-*`.
- Forms: `mb-3 row` + `col-sm-2 col-form-label` + `col-sm-8` + `form-control` — that exact
  three-part row is the form idiom, see `trade-ticket.component.html`.
- Nav: `navbar` + `nav nav-tabs` with `routerLinkActive="active"` in `header.component.html`.
- Headings inside a page are `<h5>` / `<h6>` (the `<h1>`-sized page title is the navbar brand).

## When SCSS is allowed

Only for layout a utility can't express — proportional split panes, mostly. The one real example is
`trade.component.scss`: a nested, component-scoped block with flex-basis sizing. If you add SCSS:

- put it in the component's own `.scss` (view-encapsulated), never in `styles.scss`;
- nest under a single feature root class (`.trade-container { … }`);
- use `flex-basis` / `min-width` for split panes, matching `60% / 40%` blotters;
- no colours, no font sizes, no new spacing scale.

Inline `style="…"` in a template is accepted for **AG Grid box sizing only** (`style="width: 100%;
height: 350px;"`), because the grid needs an explicit height. Nowhere else.

## AG Grid

- Theme class is always `ag-theme-alpine`. Both light and dark themes keep it.
- Size with `flex` in the `ColDef` (`accounts`) or `sizeColumnsToFit()` on ready (`blotters`) —
  don't hard-code column widths.
- `headerName` is UPPERCASE in the trade blotter (`SECURITY`, `QUANTITY`, `SIDE`, `STATE`);
  where a `ColDef` omits `headerName`, AG Grid title-cases the field (`Account Id`). Match the
  grid you're extending rather than mixing both in one table.
- Live-updating columns get `enableCellChangeFlash: true` — that flash is the app's only
  animation, and it's how a trader sees a state change.

## Badges and status

The app currently renders `state` as plain text in a grid cell. If a design calls for a pill, use
Bootstrap badges with these tones — don't invent colours:

| State | Class |
| --- | --- |
| `New` | `badge text-bg-secondary` |
| `Processing` | `badge text-bg-info` |
| `Pending` | `badge text-bg-warning` |
| `Settled` | `badge text-bg-success` |
| `Buy` / `Sell` side | `text-success` / `text-danger` |
