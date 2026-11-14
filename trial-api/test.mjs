import { test } from 'node:test';
import assert from 'node:assert/strict';
import { makeHandler, makeLimiter, LIMIT_MS } from './handler.mjs';

const ID = 'a'.repeat(64);

function post(body, ip = '1.2.3.4') {
  return new Request('https://x/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-forwarded-for': ip },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
}

function fakeSql(storedValue) {
  const calls = [];
  const sql = async (query, params) => {
    calls.push({ query, params });
    return { rows: [{ used_ms: String(storedValue ?? params[2]) }] };
  };
  return { sql, calls };
}

const noLimit = () => false;

test('GET is a health check', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const res = await h(new Request('https://x/', { method: 'GET' }));
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true, service: 'cronorep-trial' });
});

test('stores the value and returns the stored one', async () => {
  const { sql, calls } = fakeSql(9000);
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 5000 }));
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true, usedMs: 9000 });
  assert.deepEqual(calls[0].params, [ID, 'wear', 5000]);
  assert.match(calls[0].query, /GREATEST\(trial_usage\.used_ms, EXCLUDED\.used_ms\)/);
});

test('cuts a value above the limit', async () => {
  const { sql, calls } = fakeSql();
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'phone', usedMs: LIMIT_MS + 1 }));
  assert.equal(res.status, 200);
  assert.equal(calls[0].params[2], LIMIT_MS);
});

test('rejects bad input', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const cases = [
    ['not json', 'json'],
    [{ id: 'ABC', platform: 'wear', usedMs: 1 }, 'id'],
    [{ id: 'A'.repeat(64), platform: 'wear', usedMs: 1 }, 'id'],
    [{ id: ID, platform: 'ios', usedMs: 1 }, 'platform'],
    [{ id: ID, platform: 'wear', usedMs: -1 }, 'usedMs'],
    [{ id: ID, platform: 'wear', usedMs: 1.5 }, 'usedMs'],
    [{ id: ID, platform: 'wear', usedMs: '5' }, 'usedMs'],
  ];
  for (const [body, error] of cases) {
    const res = await h(post(body));
    assert.equal(res.status, 400, `case ${JSON.stringify(body)}`);
    assert.deepEqual(await res.json(), { ok: false, error });
  }
});

test('rate limit answers 429', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: () => true });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 1 }));
  assert.equal(res.status, 429);
});

test('database error answers 500', async () => {
  const sql = async () => { throw new Error('down'); };
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 1 }));
  assert.equal(res.status, 500);
  assert.deepEqual(await res.json(), { ok: false, error: 'server' });
});

test('other methods answer 405', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const res = await h(new Request('https://x/', { method: 'PUT' }));
  assert.equal(res.status, 405);
});

test('limiter allows 30 posts per window, then blocks', () => {
  const limited = makeLimiter(30, 600_000);
  for (let i = 0; i < 30; i++) assert.equal(limited('ip'), false);
  assert.equal(limited('ip'), true);
  assert.equal(limited('other'), false);
});

test('the limiter key is the rightmost x-forwarded-for entry', async () => {
  let seen;
  const h = makeHandler({ sql: fakeSql().sql, limited: (ip) => { seen = ip; return false; } });
  await h(post({ id: ID, platform: 'wear', usedMs: 1 }, '6.6.6.6, 1.2.3.4'));
  assert.equal(seen, '1.2.3.4');
});

test('limiter evicts the oldest key and keeps recent ones', () => {
  const limited = makeLimiter(1, 600_000);
  assert.equal(limited('a'), false);
  assert.equal(limited('a'), true);
  for (let i = 1; i <= 5001; i++) limited(`k${i}`);
  assert.equal(limited('zzz-new'), false);
  assert.equal(limited('k4999'), true, 'recent keys keep their count');
  assert.equal(limited('zzz-new'), true);
  assert.equal(limited('a'), false, 'the oldest key was evicted');
});
