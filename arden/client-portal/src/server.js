import http from 'node:http';
import { readFileSync } from 'node:fs';
import { loadQuotes } from './feed/client.js';
import { valuePortfolio } from './valuation/valuation.js';

const positions = JSON.parse(readFileSync('fixtures/positions.json', 'utf8'));
const port = Number(process.env.PORT ?? 3200);
const page = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Portfolio valuation</title>
  <style>
    :root { color-scheme: light; font: 16px/1.5 system-ui, sans-serif; color: #182433; background: #f4f6f8; }
    body { max-width: 1080px; margin: 0 auto; padding: 40px 24px; }
    header { border-bottom: 1px solid #d8dee6; margin-bottom: 28px; padding-bottom: 18px; }
    .brand { color: #586879; font-size: .85rem; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
    h1 { margin: 8px 0 0; font-size: 2rem; }
    #status { border-radius: 8px; margin: 18px 0; padding: 14px 16px; }
    #status.error { background: #fff0f0; border: 1px solid #df7777; color: #8a2020; }
    #status.ok { background: #edf8f1; border: 1px solid #80b891; color: #215d35; }
    #asof { color: #586879; margin: 12px 0; }
    table { background: white; border-collapse: collapse; width: 100%; }
    th, td { border-bottom: 1px solid #d8dee6; padding: 12px 14px; text-align: left; }
    th { color: #586879; font-size: .82rem; text-transform: uppercase; }
    td.number { font-variant-numeric: tabular-nums; text-align: right; }
  </style>
</head>
<body>
  <header><div class="brand">Arden Capital Markets</div><h1>Portfolio valuation</h1></header>
  <div id="status" role="status" aria-live="polite">Loading valuation…</div>
  <p id="asof"></p>
  <div id="results"></div>
  <script>
    const status = document.querySelector('#status');
    const results = document.querySelector('#results');
    const asof = document.querySelector('#asof');
    const formatNumber = (value, digits = 2) => Number(value).toLocaleString('en-GB', {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    });
    const cell = (row, value, numeric = false) => {
      const td = document.createElement('td');
      td.textContent = value;
      if (numeric) td.className = 'number';
      row.append(td);
    };
    fetch('/api/portfolio/valuation')
      .then(async (response) => {
        const body = await response.json();
        if (!response.ok) {
          status.className = 'error';
          status.textContent = \`Valuation unavailable (HTTP \${response.status}: \${body.error || response.statusText})\`;
          const detail = document.createElement('p');
          detail.textContent = 'The feed batch could not be valued. Please retry after the feed issue is resolved.';
          status.append(detail);
          return;
        }
        status.className = 'ok';
        status.textContent = 'Portfolio valuation available';
        asof.textContent = \`As of \${body.asof || 'unknown'}\`;
        const table = document.createElement('table');
        const head = document.createElement('tr');
        ['ISIN', 'Quantity', 'Price', 'Currency', 'Market value', 'As of'].forEach((label) => {
          const th = document.createElement('th');
          th.textContent = label;
          head.append(th);
        });
        const thead = document.createElement('thead');
        thead.append(head);
        const tbody = document.createElement('tbody');
        body.rows.forEach((item) => {
          const row = document.createElement('tr');
          cell(row, item.isin);
          cell(row, formatNumber(item.quantity, 0), true);
          cell(row, formatNumber(item.price, 4), true);
          cell(row, item.ccy);
          cell(row, \`\${item.ccy} \${formatNumber(item.marketValue)}\`, true);
          cell(row, item.asof);
          tbody.append(row);
        });
        table.append(thead, tbody);
        results.replaceChildren(table);
      })
      .catch(() => {
        status.className = 'error';
        status.textContent = 'Valuation unavailable (network error)';
        const detail = document.createElement('p');
        detail.textContent = 'The feed batch could not be valued. Please retry after the feed issue is resolved.';
        status.append(detail);
      });
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  const started = Date.now();
  try {
    if (req.url === '/') {
      res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
      res.end(page);
    } else if (req.url === '/api/portfolio/valuation') {
      const rows = valuePortfolio(positions, loadQuotes());
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ rows, asof: rows[0]?.asof }));
    } else if (req.url === '/api/settlement/summary') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ message: 'Trades settle 2 business days after execution (T+2). US equities settle T+1.' }));
    } else if (req.url === '/health') {
      res.writeHead(200); res.end('ok');
    } else {
      res.writeHead(404); res.end();
    }
  } catch (err) {
    console.error(JSON.stringify({ level: 'error', service: 'client-portal', route: req.url, error: err.message, stack: err.stack }));
    res.writeHead(500, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ error: 'valuation_failed' }));
  } finally {
    console.log(JSON.stringify({ level: 'info', service: 'client-portal', route: req.url, status: res.statusCode, ms: Date.now() - started }));
  }
});

server.listen(port, () => console.log(`client-portal listening on ${port}`));
