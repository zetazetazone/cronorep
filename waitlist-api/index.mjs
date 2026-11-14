// CronoRep waitlist API (Neon Function).
// POST / {email, platform: "android"|"ios"|"both", lang?, company?}  -> 200 {ok:true}
// "company" is a honeypot field: real people leave it empty.
// No dependencies: SQL goes over Neon's HTTP endpoint, so the bundle stays tiny.
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

const ALLOWED = (process.env.ALLOWED_ORIGINS ||
  'https://enautomatico.online,http://localhost:5173,http://127.0.0.1:5173')
  .split(',').map((s) => s.trim());
const PLATFORMS = new Set(['android', 'ios', 'both']);
const EMAIL_RE = /^[^\s@]{1,64}@[^\s@]{1,190}\.[a-z]{2,24}$/i;

// Small in-memory limit per IP: 8 posts per 10 minutes.
const hits = new Map();
function limited(ip) {
  const now = Date.now();
  const list = (hits.get(ip) || []).filter((t) => now - t < 600_000);
  list.push(now);
  hits.set(ip, list);
  if (hits.size > 5000) hits.clear();
  return list.length > 8;
}

function cors(origin) {
  const h = {
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Max-Age': '86400',
    Vary: 'Origin',
  };
  if (origin && ALLOWED.includes(origin)) h['Access-Control-Allow-Origin'] = origin;
  return h;
}

function json(status, body, origin) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...cors(origin) },
  });
}

export default async function handler(request) {
  const origin = request.headers.get('origin');
  if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors(origin) });
  if (request.method === 'GET') return json(200, { ok: true, service: 'cronorep-waitlist' }, origin);
  if (request.method !== 'POST') return json(405, { ok: false, error: 'method' }, origin);
  if (origin && !ALLOWED.includes(origin)) return json(403, { ok: false, error: 'origin' }, origin);

  const ip = (request.headers.get('x-forwarded-for') || '').split(',')[0].trim() || 'unknown';
  if (limited(ip)) return json(429, { ok: false, error: 'rate' }, origin);

  let data;
  try {
    data = await request.json();
  } catch {
    return json(400, { ok: false, error: 'json' }, origin);
  }

  // Honeypot: pretend success, store nothing.
  if (data && typeof data.company === 'string' && data.company.trim() !== '') return json(200, { ok: true }, origin);

  const email = String(data?.email || '').trim().toLowerCase();
  const platform = String(data?.platform || '').trim().toLowerCase();
  const lang = String(data?.lang || '').slice(0, 12) || null;
  if (!EMAIL_RE.test(email) || email.length > 254) return json(400, { ok: false, error: 'email' }, origin);
  if (!PLATFORMS.has(platform)) return json(400, { ok: false, error: 'platform' }, origin);

  try {
    await sql(
      `INSERT INTO waitlist (email, platform, lang, referrer)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (email, platform) DO NOTHING`,
      [email, platform, lang, origin],
    );
  } catch (err) {
    console.error('insert failed', err?.message);
    return json(500, { ok: false, error: 'server' }, origin);
  }
  return json(200, { ok: true }, origin);
}
