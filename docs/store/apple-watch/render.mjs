// Renders the Apple Watch App Store screenshots from screens.html.
// Usage: NODE_PATH=cook/harness/node_modules node docs/store/apple-watch/render.mjs
// Output: docs/store/assets/apple-watch-<n>.png at 422x514 (Apple Watch Ultra 3 size).
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const here = path.dirname(fileURLToPath(import.meta.url));
const src = pathToFileURL(path.join(here, 'screens.html')).href;
const out = path.join(here, '..', 'assets');
// 6 = paywall: the App Review screenshot for the lifetime_unlock purchase.
const names = ['rest', 'ticks', 'go', 'idle', 'settings', 'paywall'];
// Optional: pass shot numbers to render only those, e.g. `render.mjs 6`.
const only = process.argv.slice(2).map(Number);

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 422, height: 514 }, deviceScaleFactor: 1 });
for (let i = 0; i < names.length; i++) {
  if (only.length && !only.includes(i + 1)) continue;
  await page.goto(`${src}?s=${i + 1}`, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  const file = path.join(out, `apple-watch-${i + 1}-${names[i]}.png`);
  await page.screenshot({ path: file, omitBackground: false });
  console.log('wrote', file);
}
await browser.close();
