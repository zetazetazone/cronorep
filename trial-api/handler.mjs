// CronoRep trial API logic. index.mjs gives it the real database.
// POST / {id: 64 hex, platform: "wear"|"phone", usedMs: int} -> 200 {ok:true, usedMs}
// The stored value only goes up, so a client cannot lower it.
export const LIMIT_MS = 10 * 60 * 60 * 1000;

const ID_RE = /^[0-9a-f]{64}$/;
const PLATFORMS = new Set(['wear', 'phone']);

/** In-memory limit per IP: `max` posts per `windowMs`. */
export function makeLimiter(max = 30, windowMs = 600_000) {
  const hits = new Map();
  return function limited(ip) {
    const now = Date.now();
    const list = (hits.get(ip) || []).filter((t) => now - t < windowMs);
    list.push(now);
    hits.set(ip, list);
    if (hits.size > 5000) hits.clear();
    return list.length > max;
  };
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export function makeHandler({ sql, limited }) {
  return async function handler(request) {
    if (request.method === 'GET') return json(200, { ok: true, service: 'cronorep-trial' });
    if (request.method !== 'POST') return json(405, { ok: false, error: 'method' });

    const ip = (request.headers.get('x-forwarded-for') || '').split(',')[0].trim() || 'unknown';
    if (limited(ip)) return json(429, { ok: false, error: 'rate' });

    let data;
    try {
      data = await request.json();
    } catch {
      return json(400, { ok: false, error: 'json' });
    }

    const id = data?.id;
    const platform = data?.platform;
    const usedMs = data?.usedMs;
    if (typeof id !== 'string' || !ID_RE.test(id)) return json(400, { ok: false, error: 'id' });
    if (!PLATFORMS.has(platform)) return json(400, { ok: false, error: 'platform' });
    if (!Number.isInteger(usedMs) || usedMs < 0) return json(400, { ok: false, error: 'usedMs' });

    try {
      const result = await sql(
        `INSERT INTO trial_usage (trial_id, platform, used_ms) VALUES ($1, $2, $3)
         ON CONFLICT (trial_id) DO UPDATE
           SET used_ms = GREATEST(trial_usage.used_ms, EXCLUDED.used_ms), updated_at = now()
         RETURNING used_ms`,
        [id, platform, Math.min(usedMs, LIMIT_MS)],
      );
      return json(200, { ok: true, usedMs: Number(result.rows[0].used_ms) });
    } catch (err) {
      console.error('upsert failed', err?.message);
      return json(500, { ok: false, error: 'server' });
    }
  };
}
