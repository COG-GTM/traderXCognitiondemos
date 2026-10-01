import http from 'node:http';
import { readFileSync } from 'node:fs';
import { loadQuotes } from './feed/client.js';
import { valuePortfolio } from './valuation/valuation.js';

const positions = JSON.parse(readFileSync('fixtures/positions.json', 'utf8'));
const port = Number(process.env.PORT ?? 3200);

const server = http.createServer((req, res) => {
  const started = Date.now();
  try {
    if (req.url === '/api/portfolio/valuation') {
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
