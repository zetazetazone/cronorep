// CronoRep trial API (Neon Function). Logic is in handler.mjs.
// No dependencies: SQL goes over Neon's HTTP endpoint, so the bundle stays tiny.
import { makeHandler, makeLimiter } from './handler.mjs';

async function sql(query, params) {
  const url = process.env.DATABASE_URL;
  const host = new URL(url).hostname;
  const res = await fetch(`https://${host}/sql`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Neon-Connection-String': url },
    body: JSON.stringify({ query, params }),
  });
  if (!res.ok) throw new Error(`sql ${res.status}: ${(await res.text()).slice(0, 200)}`);
  return res.json();
}

export default makeHandler({ sql, limited: makeLimiter(30, 600_000) });
