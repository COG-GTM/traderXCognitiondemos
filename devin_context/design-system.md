# TraderX design system

The UI is Bootstrap 5.3 with a small trading-desk overlay. Everything below already exists in the
app (`main/styles.scss`, the header and blotter components) — treat it as the source of truth for
any new screen or widget.

## Colour tokens

| Token | Hex | Use |
| --- | --- | --- |
| Primary / brand | `#044fbd` | Primary buttons, active accents, default card accent bar |
| Primary hover | `#0d6efd` | Hover state for primary and `.btn-info` |
| Long / positive | `#198754` | Long positions, buy side, gains |
| Short / negative | `#dc3545` | Short positions, sell side, losses |
| Text default | `#212529` | Values, headings |
| Text muted | `#6c757d` | Labels, captions |
| Border | `#dee2e6` | Card and grid borders |
| Surface | `#ffffff` | Cards, panels, grid body |
| Surface alt | `#f8f9fa` | Grid headers, subtle fills |

Never introduce a hex value outside this table; prefer the Bootstrap class that maps to it
(`text-muted`, `border`, `bg-body-tertiary`, `text-success`, `text-danger`).

## Typography

| Role | Size / weight | Notes |
| --- | --- | --- |
| Panel heading (`<h5>`) | 1.05rem / 600 | "Trades", "Positions" |
| Metric label | 0.6875rem / 600, uppercase, `letter-spacing: 0.08em` | muted colour |
| Metric value | 1.875rem / 600, `line-height: 1` | tabular numbers, thousands separators |
| Metric caption | 0.75rem / 400 | muted colour, one short sentence |
| Body / grid cell | 0.8125–0.875rem / 400 | AG Grid defaults |

## Spacing

4px base scale, expressed with Bootstrap utilities: `gap-3` (16px) between sibling cards,
`mb-4` (24px) below a card strip, `p-5` page padding (existing `.trade-container`).

## Components

### Metric card

The standard KPI tile used above a blotter.

- White surface, `1px solid #dee2e6`, `border-radius: 6px`, `box-shadow: 0 1px 2px rgba(16,24,40,.06)`.
- **4px left accent bar** carrying the semantic colour: brand `#044fbd` for neutral metrics,
  `#198754` for long/positive, `#dc3545` for short/negative.
- Vertical order inside the card: uppercase label → large value → muted caption.
- The value inherits the accent colour for long/short cards; neutral cards keep default text.
- Cards in a strip are equal width (`flex: 1`) and wrap on narrow viewports.

### Blotter panel

AG Grid `ag-theme-alpine`, `height: 350px`, `<h5>` title above the grid, uppercase column headers,
trades panel 60% width and positions panel 40% (`.trade-blotter` in `trade.component.scss`).

### Buttons

`btn btn-sm btn-primary` for primary actions; buy = green (`#buyButton`), sell = red
(`#sellButton`) as defined globally.

## Layout rules

- The trade page is a single column: ops row (account selector + ticket button) → metric strip →
  blotters.
- Metric strips sit directly above the data they summarise and never scroll horizontally.
- Numbers are right-aligned in grids, left-aligned in metric cards, and always formatted with the
  `number` pipe (`{{ value | number }}`).
