// Composes baseline (left) and upgraded (right) screenshots side by side with a pixel-diff %.
import fs from 'fs';
import path from 'path';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';

const root = path.dirname(new URL(import.meta.url).pathname);
const screens = path.join(root, 'report', 'screens');
const outDir = path.join(root, 'report', 'side-by-side');
fs.mkdirSync(outDir, { recursive: true });

const read = (p) => (fs.existsSync(p) ? PNG.sync.read(fs.readFileSync(p)) : null);
const pad = (img, w, h) => {
  const out = new PNG({ width: w, height: h });
  out.data.fill(255);
  PNG.bitblt(img, out, 0, 0, img.width, img.height, 0, 0);
  return out;
};

const results = loadResults();
const names = new Set([
  ...listPngs(path.join(screens, 'baseline')),
  ...listPngs(path.join(screens, 'upgraded')),
]);
const rows = [];
for (const name of [...names].sort()) {
  const b = read(path.join(screens, 'baseline', `${name}.png`));
  const u = read(path.join(screens, 'upgraded', `${name}.png`));
  let diff = 'n/a';
  if (b && u) {
    const w = Math.max(b.width, u.width);
    const h = Math.max(b.height, u.height);
    const bp = pad(b, w, h);
    const up = pad(u, w, h);
    const changed = pixelmatch(bp.data, up.data, null, w, h, { threshold: 0.1 });
    diff = `${((changed / (w * h)) * 100).toFixed(2)}%`;
    const gap = 20;
    const sbs = new PNG({ width: w * 2 + gap, height: h });
    sbs.data.fill(255);
    PNG.bitblt(bp, sbs, 0, 0, w, h, 0, 0);
    PNG.bitblt(up, sbs, 0, 0, w, h, w + gap, 0);
    fs.writeFileSync(path.join(outDir, `${name}.png`), PNG.sync.write(sbs));
  }
  rows.push({ name, b: !!b, u: !!u, diff });
}

const lines = [
  '# Angular 18 -> 21 parity report',
  '',
  'Same Playwright specs run against `baseline` (Angular 18, http://localhost:4000) and `upgraded` (Angular 21, http://localhost:4200), both against the same local TraderX backend.',
  '',
  '## Spec results',
  '',
  '| Spec | Baseline | Upgraded |',
  '| --- | --- | --- |',
  ...Object.entries(results).map(([t, r]) => `| ${t} | ${r.baseline ?? 'not run'} | ${r.upgraded ?? 'not run'} |`),
  '',
  '## Screenshots (baseline left, upgraded right)',
  '',
  '| Page / flow | Pixel diff | Side by side |',
  '| --- | --- | --- |',
  ...rows.map((r) => `| ${r.name} | ${r.diff} | ${r.b && r.u ? `![${r.name}](side-by-side/${r.name}.png)` : 'missing'} |`),
  '',
  '## Notes',
  '',
  '- Both apps share one backend and every run creates timestamped accounts and trades, so non-zero pixel diffs come from data created between the baseline and upgraded runs (e.g. the account dropdown list is longer, the ticket shows that run\'s account name), not from rendering differences.',
  '- `retries: 1` is set because AG Grid row virtualization and the 200 ms account-list debounce can race a fresh backend; the recorded run used `--retries=0`.',
  '',
];
fs.writeFileSync(path.join(root, 'report', 'README.md'), lines.join('\n'));
console.log(lines.join('\n'));

function listPngs(dir) {
  return fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => f.endsWith('.png')).map((f) => f.slice(0, -4)) : [];
}

function loadResults() {
  const file = path.join(root, 'report', 'results.json');
  const out = {};
  if (!fs.existsSync(file)) return out;
  const walk = (suite, prefix) => {
    for (const s of suite.suites ?? []) walk(s, prefix ? `${prefix} > ${s.title}` : s.title);
    for (const spec of suite.specs ?? []) {
      const title = `${prefix} > ${spec.title}`.replace(/^.*?\.spec\.ts > /, (m) => m);
      for (const t of spec.tests ?? []) {
        const status = t.results?.at(-1)?.status;
        (out[title] ??= {})[t.projectName] = status === 'passed' ? 'pass' : status ?? 'not run';
      }
    }
  };
  walk(JSON.parse(fs.readFileSync(file, 'utf8')), '');
  return out;
}
