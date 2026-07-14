const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');

// Regression guard for CVE-2024-43796 (XSS via Express response.redirect(),
// fixed in express@4.20.0) and the transitive advisories bundled with the
// pre-4.20 dependency tree (path-to-regexp ReDoS CVE-2024-45296,
// send/serve-static XSS CVE-2024-43799/CVE-2024-43800, body-parser DoS
// CVE-2024-45590). The declared floor must never resolve below 4.20.0.
const MIN_SAFE = [4, 20, 0];

function parseVersion(v) {
  return v.split('-')[0].split('.').map(Number);
}

function gte(actual, minimum) {
  for (let i = 0; i < minimum.length; i++) {
    if ((actual[i] || 0) > minimum[i]) return true;
    if ((actual[i] || 0) < minimum[i]) return false;
  }
  return true;
}

test('resolved express version is patched (>= 4.20.0)', () => {
  const { version } = require('express/package.json');
  assert.ok(
    gte(parseVersion(version), MIN_SAFE),
    `express ${version} is below the patched floor ${MIN_SAFE.join('.')}; ` +
      'it is vulnerable to CVE-2024-43796 and transitive 2024 advisories.'
  );
});

test('server does not expose an unsanitized redirect sink', () => {
  // CVE-2024-43796 is only exploitable through user-controlled input reaching
  // response.redirect()/response.location(). Assert the trade-feed server keeps
  // that sink absent so the finding stays unreachable.
  const source = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');
  assert.ok(!/\.redirect\s*\(/.test(source), 'unexpected res.redirect() usage');
  assert.ok(!/\.location\s*\(/.test(source), 'unexpected res.location() usage');
});
