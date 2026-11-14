// Renders the App Store "Header" and "Search Results" assets from banners.html.
// Usage: NODE_PATH=cook/harness/node_modules node docs/store/apple-watch/render-banners.mjs [--guide]
// Output: docs/store/assets/appstore-header-3840x1646.png and appstore-search-3840x2560.png
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const here = path.dirname(fileURLToPath(import.meta.url));
const src = pathToFileURL(path.join(here, 'banners.html')).href;
const guide = process.argv.includes('--guide');
const out = guide ? path.join(here, '..', '..', '..', 'cook') : path.join(here, '..', 'assets');

const assets = [
  { a: 'header', w: 3840, h: 1646 },
  { a: 'search', w: 3840, h: 2560 },
];

const browser = await chromium.launch();
for (const { a, w, h } of assets) {
  const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
  await page.goto(`${src}?a=${a}${guide ? '&guide' : ''}`, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  const file = path.join(out, `appstore-${a}-${w}x${h}${guide ? '-guide' : ''}.png`);
  await page.screenshot({ path: file });
  console.log('wrote', file);
  await page.close();
}
await browser.close();
