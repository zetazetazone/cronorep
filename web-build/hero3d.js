// CronoRep hero: two watches made in code, each running the real timer with a GIF behind it.
// Build: npm run build  ->  ../web/js/hero3d.js
import {
  WebGLRenderer, Scene, PerspectiveCamera, Group, Mesh, CanvasTexture, SRGBColorSpace, ACESFilmicToneMapping,
  PMREMGenerator, MeshStandardMaterial, MeshBasicMaterial, LatheGeometry, CircleGeometry, CylinderGeometry,
  BoxGeometry, ExtrudeGeometry, ShapeGeometry, Shape, Vector2, Vector3, Color, DirectionalLight, PointLight,
  AdditiveBlending, Raycaster, MathUtils, DoubleSide, MeshPhysicalMaterial,
} from 'three';
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js';
import { RoundedBoxGeometry } from 'three/examples/jsm/geometries/RoundedBoxGeometry.js';

const GREEN = '#4ade80';
const ORANGE = '#fb923c';
const REST = 10; // demo rest, seconds
const GO_HOLD = 7; // seconds of GO before the demo taps again by itself

// Full-size GIPHY video (480 px) for the screens, small one on slow or data-saver connections.
const conn = navigator.connection || {};
const LOW_DATA = !!conn.saveData || /(^|-)2g|3g/.test(conn.effectiveType || '');
const gifUrl = (id, small) => `https://media.giphy.com/media/${id}/${LOW_DATA || small ? '200' : 'giphy'}.mp4`;
const fmt = (s) => Math.floor(s / 60) + ':' + String(Math.floor(s % 60)).padStart(2, '0');

function makeVideo(id, small) {
  const v = document.createElement('video');
  v.crossOrigin = 'anonymous';
  v.muted = true;
  v.loop = true;
  v.playsInline = true;
  v.setAttribute('playsinline', '');
  v.preload = 'auto';
  v.src = gifUrl(id, small);
  v.play().catch(() => {});
  return v;
}

// ---------- the screen: same drawing as the app (GIF, 55% black, ring, text) ----------
class Screen {
  constructor(w, h, round, nextGif, startDelay) {
    this.w = w; this.h = h; this.round = round; this.nextGif = nextGif;
    this.canvas = document.createElement('canvas');
    this.canvas.width = w; this.canvas.height = h;
    this.ctx = this.canvas.getContext('2d');
    this.texture = new CanvasTexture(this.canvas);
    this.texture.colorSpace = SRGBColorSpace;
    this.texture.anisotropy = 4;
    this.video = null;
    this.mode = 'idle';
    this.start = performance.now() + startDelay;
    this.lastTick = -1;
    this.onBuzz = null;
    this.onTick = null;
    this.swapGif();
  }
  swapGif() {
    const old = this.video;
    // first GIF small so the screen fills at once; later GIFs full size
    const v = makeVideo(this.nextGif(), !old);
    v.addEventListener('loadeddata', () => { this.video = v; if (old) { old.pause(); old.removeAttribute('src'); old.load(); } }, { once: true });
    if (!old) this.video = v;
  }
  tap(now) {
    this.start = now; this.lastTick = -1; this.mode = 'rest';
    this.swapGif();
  }
  accent() { return this.mode === 'go' ? ORANGE : GREEN; }
  draw(now) {
    const { ctx, w, h } = this;
    const S = Math.min(w, h);
    const el = (now - this.start) / 1000;
    ctx.fillStyle = '#000';
    ctx.fillRect(0, 0, w, h);
    const v = this.video;
    if (v && v.readyState >= 2 && v.videoWidth) {
      const r = Math.max(w / v.videoWidth, h / v.videoHeight);
      const dw = v.videoWidth * r, dh = v.videoHeight * r;
      ctx.drawImage(v, (w - dw) / 2, (h - dh) / 2, dw, dh);
      ctx.fillStyle = 'rgba(0,0,0,0.55)';
      ctx.fillRect(0, 0, w, h);
    }
    let label, time, sub, frac = 1;
    if (el < 0) { this.mode = 'idle'; label = 'TAP TO START'; time = fmt(REST); sub = 'Hold to set time'; }
    else if (el < REST) {
      this.mode = 'rest';
      const left = REST - el;
      frac = left / REST;
      label = 'REST'; time = fmt(Math.ceil(left)); sub = 'total ' + fmt(el);
      const sec = Math.ceil(left);
      if (sec <= 3 && sec !== this.lastTick) { this.lastTick = sec; this.onTick && this.onTick(); }
    } else {
      if (this.mode !== 'go') { this.mode = 'go'; this.onBuzz && this.onBuzz(); }
      label = 'GO'; time = '+' + fmt(el - REST); sub = 'total ' + fmt(el);
      if (el - REST > GO_HOLD) this.tap(now);
    }
    const cx = w / 2, cy = h / 2;
    const sc = ctx.createRadialGradient(cx, cy, 0, cx, cy, S * 0.42);
    sc.addColorStop(0, 'rgba(0,0,0,0.45)'); sc.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = sc; ctx.fillRect(0, 0, w, h);
    const rad = this.round ? S / 2 - S * 0.035 : S / 2 - S * 0.07;
    const lw = S * 0.032;
    ctx.lineCap = 'round';
    ctx.lineWidth = lw;
    if (this.mode !== 'idle') {
      ctx.strokeStyle = 'rgba(255,255,255,0.12)';
      ctx.beginPath(); ctx.arc(cx, cy, rad, 0, Math.PI * 2); ctx.stroke();
      ctx.strokeStyle = this.accent();
      ctx.beginPath(); ctx.arc(cx, cy, rad, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * frac); ctx.stroke();
    }
    ctx.textAlign = 'center';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = this.accent();
    ctx.font = `700 ${Math.round(S * (this.mode === 'idle' ? 0.075 : 0.085))}px Roboto, system-ui, sans-serif`;
    ctx.fillText(label, cx, cy - S * 0.15);
    ctx.fillStyle = this.mode === 'go' ? ORANGE : '#fff';
    ctx.font = `300 ${Math.round(S * 0.26)}px Roboto, system-ui, sans-serif`;
    ctx.fillText(time, cx, cy + S * 0.09);
    ctx.fillStyle = '#c9ced5';
    ctx.font = `400 ${Math.round(S * 0.064)}px Roboto, system-ui, sans-serif`;
    ctx.fillText(sub, cx, cy + S * 0.2);
    // glass: one soft reflection stripe
    const gl = ctx.createLinearGradient(0, 0, w, h * 0.8);
    gl.addColorStop(0.18, 'rgba(255,255,255,0)'); gl.addColorStop(0.3, 'rgba(255,255,255,0.08)'); gl.addColorStop(0.42, 'rgba(255,255,255,0)');
    ctx.fillStyle = gl; ctx.fillRect(0, 0, w, h);
    this.texture.needsUpdate = true;
  }
}

// ---------- shared parts ----------
// A band that leaves the lug and curves back around the wrist.
function bandGeometry(width, thick, length, radius) {
  const g = new BoxGeometry(width, length, thick, 6, 64, 2);
  g.translate(0, length / 2, 0);
  const p = g.attributes.position;
  for (let i = 0; i < p.count; i++) {
    const y = p.getY(i), z = p.getZ(i);
    const u = y / length;
    const tip = 0.32;
    let sx = 1 - 0.14 * u;
    if (y > length - tip) { const k = (y - (length - tip)) / tip; sx *= Math.max(0.18, Math.sqrt(1 - k * k)); }
    p.setX(i, p.getX(i) * sx);
    const t = y / radius;
    p.setY(i, (radius + z) * Math.sin(t));
    p.setZ(i, -radius + (radius + z) * Math.cos(t));
    // soften the long edges a little
    const x = p.getX(i);
    const edge = Math.abs(x) / (width / 2);
    if (edge > 0.99) p.setX(i, x * 0.985);
  }
  g.computeVertexNormals();
  return g;
}

// A loop around the band at arc length a (on a band built by bandGeometry).
function keeper(width, thick, radius, a, mat) {
  const t = a / radius;
  const m = new Mesh(new RoundedBoxGeometry(width * 0.93 + 0.07, 0.11, thick + 0.07, 3, 0.035), mat);
  m.position.set(0, radius * Math.sin(t), -radius + radius * Math.cos(t));
  m.rotation.x = -t;
  return m;
}

function roundedRectShape(w, h, r) {
  const s = new Shape();
  const x = -w / 2, y = -h / 2;
  s.moveTo(x + r, y);
  s.lineTo(x + w - r, y); s.quadraticCurveTo(x + w, y, x + w, y + r);
  s.lineTo(x + w, y + h - r); s.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  s.lineTo(x + r, y + h); s.quadraticCurveTo(x, y + h, x, y + h - r);
  s.lineTo(x, y + r); s.quadraticCurveTo(x, y, x + r, y);
  return s;
}

function fitUV(geo, w, h) {
  const p = geo.attributes.position, uv = geo.attributes.uv;
  for (let i = 0; i < p.count; i++) uv.setXY(i, (p.getX(i) + w / 2) / w, (p.getY(i) + h / 2) / h);
  uv.needsUpdate = true;
  return geo;
}

const glassMat = () => new MeshStandardMaterial({
  color: 0x000000, roughness: 0.04, metalness: 0, transparent: true, opacity: 1,
  blending: AdditiveBlending, depthWrite: false, envMapIntensity: 2.2,
});

// ---------- round watch (Galaxy Watch style) ----------
function roundWatch(screen) {
  const g = new Group();
  const caseMat = new MeshStandardMaterial({ color: 0xc3c7ce, metalness: 1, roughness: 0.26, side: DoubleSide });
  const bezelMat = new MeshStandardMaterial({ color: 0x2a2e35, metalness: 0.85, roughness: 0.32, side: DoubleSide });
  const tickMat = new MeshStandardMaterial({ color: 0xdfe3e8, metalness: 0.6, roughness: 0.35 });
  const bandMat = new MeshPhysicalMaterial({ color: 0x1b1e23, metalness: 0, roughness: 0.55, clearcoat: 0.3, clearcoatRoughness: 0.5, envMapIntensity: 0.6 });

  const prof = [[0, -0.24], [0.72, -0.24], [0.9, -0.2], [1.0, -0.11], [1.05, 0.0], [1.05, 0.1], [1.02, 0.17], [0.99, 0.19]];
  const caseGeo = new LatheGeometry(prof.map(([x, y]) => new Vector2(x, y)), 96);
  caseGeo.rotateX(Math.PI / 2);
  g.add(new Mesh(caseGeo, caseMat));

  const bprof = [[0.99, 0.18], [1.02, 0.22], [1.0, 0.29], [0.93, 0.31], [0.88, 0.3], [0.86, 0.27]];
  const bezGeo = new LatheGeometry(bprof.map(([x, y]) => new Vector2(x, y)), 96);
  bezGeo.rotateX(Math.PI / 2);
  g.add(new Mesh(bezGeo, bezelMat));
  for (let i = 0; i < 60; i++) {
    const major = i % 5 === 0;
    const t = new Mesh(new BoxGeometry(0.012, major ? 0.07 : 0.035, 0.01), tickMat);
    const a = (i / 60) * Math.PI * 2;
    const r = major ? 0.935 : 0.95;
    t.position.set(Math.sin(a) * r, Math.cos(a) * r, 0.307);
    t.rotation.z = -a;
    g.add(t);
  }

  const scr = new Mesh(new CircleGeometry(0.855, 96), new MeshBasicMaterial({ map: screen.texture, toneMapped: false }));
  scr.position.z = 0.262;
  g.add(scr);
  const glass = new Mesh(new CircleGeometry(0.87, 96), glassMat());
  glass.position.z = 0.27;
  g.add(glass);

  for (const a of [0.42, -0.42]) {
    const b = new Mesh(new CylinderGeometry(0.075, 0.075, 0.16, 32), caseMat);
    b.rotation.z = Math.PI / 2 - a;
    b.position.set(Math.cos(a) * 1.08, Math.sin(a) * 1.08, 0.0);
    g.add(b);
  }
  // lugs
  for (const s of [1, -1]) {
    const lug = new Mesh(new RoundedBoxGeometry(1.0, 0.36, 0.34, 4, 0.12), caseMat);
    lug.position.set(0, s * 0.92, -0.04);
    g.add(lug);
  }
  const band = bandGeometry(0.86, 0.12, 2.5, 1.05);
  const top = new Mesh(band, bandMat); top.position.set(0, 1.0, -0.06); g.add(top);
  const bot = new Mesh(band, bandMat); bot.rotation.z = Math.PI; bot.position.set(0, -1.0, -0.06); g.add(bot);
  return g;
}

// ---------- square watch (Apple Watch style) ----------
function squareWatch(screen) {
  const g = new Group();
  const caseMat = new MeshStandardMaterial({ color: 0x343a44, metalness: 0.95, roughness: 0.3 });
  const blackMat = new MeshStandardMaterial({ color: 0x030304, metalness: 0, roughness: 0.15 });
  const bandMat = new MeshPhysicalMaterial({ color: 0x166a3a, metalness: 0, roughness: 0.55, clearcoat: 0.3, clearcoatRoughness: 0.5, envMapIntensity: 0.55 });

  const W = 1.62, H = 1.98;
  const caseGeo = new ExtrudeGeometry(roundedRectShape(W - 0.2, H - 0.2, 0.34), {
    depth: 0.22, bevelEnabled: true, bevelThickness: 0.11, bevelSize: 0.1, bevelSegments: 10, curveSegments: 28,
  });
  caseGeo.translate(0, 0, -0.11);
  g.add(new Mesh(caseGeo, caseMat));

  const face = new Mesh(new ShapeGeometry(roundedRectShape(W - 0.1, H - 0.1, 0.4), 28), blackMat);
  face.position.z = 0.222;
  g.add(face);
  const sw = W - 0.3, sh = H - 0.3;
  const scr = new Mesh(fitUV(new ShapeGeometry(roundedRectShape(sw, sh, 0.3), 28), sw, sh),
    new MeshBasicMaterial({ map: screen.texture, toneMapped: false }));
  scr.position.z = 0.226;
  g.add(scr);
  const glass = new Mesh(new ShapeGeometry(roundedRectShape(W - 0.1, H - 0.1, 0.4), 28), glassMat());
  glass.position.z = 0.232;
  g.add(glass);

  const crown = new Mesh(new CylinderGeometry(0.13, 0.13, 0.15, 40, 1), new MeshStandardMaterial({ color: 0x3b414c, metalness: 0.95, roughness: 0.38, flatShading: true }));
  crown.rotation.z = Math.PI / 2;
  crown.position.set(W / 2 + 0.06, 0.38, 0);
  g.add(crown);
  const btn = new Mesh(new RoundedBoxGeometry(0.06, 0.46, 0.13, 3, 0.03), caseMat);
  btn.position.set(W / 2 + 0.015, -0.22, 0);
  g.add(btn);

  const band = bandGeometry(1.2, 0.11, 2.6, 1.1);
  const top = new Mesh(band, bandMat); top.position.set(0, H / 2 - 0.08, -0.08); g.add(top);
  const bot = new Mesh(band, bandMat); bot.rotation.z = Math.PI; bot.position.set(0, -(H / 2 - 0.08), -0.08); g.add(bot);
  return g;
}

// ---------- scene ----------
export function startHero({ canvas, gifIds, onTap }) {
  let renderer;
  try {
    renderer = new WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: 'high-performance' });
  } catch (e) {
    return false;
  }
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
  renderer.toneMapping = ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.05;
  renderer.outputColorSpace = SRGBColorSpace;

  const scene = new Scene();
  const pmrem = new PMREMGenerator(renderer);
  scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
  scene.environmentIntensity = 0.9;

  const camera = new PerspectiveCamera(26, 1, 0.1, 100);
  const key = new DirectionalLight(0xffffff, 2.2); key.position.set(-3, 4, 5); scene.add(key);
  const rimA = new PointLight(GREEN, 9, 14); rimA.position.set(-3.4, 1.4, -3); scene.add(rimA);
  const rimB = new PointLight(GREEN, 9, 14); rimB.position.set(3.4, -0.8, -3); scene.add(rimB);

  let gi = 0;
  const order = gifIds.slice().sort(() => Math.random() - 0.5);
  const nextGif = () => order[gi++ % order.length];

  const sRound = new Screen(512, 512, true, nextGif, 900);
  const sSquare = new Screen(432, 528, false, nextGif, 4600);
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;

  const wRound = roundWatch(sRound);
  const wSquare = squareWatch(sSquare);
  const rig = new Group();
  const a = new Group(); a.add(wRound); a.position.set(-0.98, 0.1, -0.35); a.rotation.set(0.05, 0.36, 0.07);
  const b = new Group(); b.add(wSquare); b.position.set(0.98, -0.1, 0.25); b.rotation.set(-0.02, -0.42, -0.05);
  rig.add(a, b);
  scene.add(rig);

  const watches = [
    { group: a, inner: wRound, screen: sRound, light: rimA, buzzT: -1 },
    { group: b, inner: wSquare, screen: sSquare, light: rimB, buzzT: -1 },
  ];
  for (const w of watches) {
    w.screen.onBuzz = () => { w.buzzT = performance.now(); };
    w.screen.onTick = () => { w.tickT = performance.now(); };
  }

  function resize() {
    const r = canvas.getBoundingClientRect();
    const wpx = Math.max(1, r.width), hpx = Math.max(1, r.height);
    renderer.setSize(wpx, hpx, false);
    camera.aspect = wpx / hpx;
    const t = Math.tan(MathUtils.degToRad(camera.fov / 2));
    const needW = 4.15, needH = camera.aspect < 1.4 ? 4.6 : 3.7;
    camera.position.set(0, 0, Math.max(needW / 2 / (t * camera.aspect), needH / 2 / t) + 0.4);
    camera.lookAt(0, 0, 0);
    camera.updateProjectionMatrix();
  }
  new ResizeObserver(resize).observe(canvas);
  resize();

  // pointer: tilt toward the cursor, tap a watch to restart its timer
  const target = new Vector2(), cur = new Vector2();
  const ray = new Raycaster(), ndc = new Vector2();
  function pick(e) {
    const r = canvas.getBoundingClientRect();
    ndc.set(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
    ray.setFromCamera(ndc, camera);
    for (const w of watches) if (ray.intersectObject(w.group, true).length) return w;
    return null;
  }
  window.addEventListener('pointermove', (e) => {
    target.set((e.clientX / innerWidth) * 2 - 1, (e.clientY / innerHeight) * 2 - 1);
    if (e.pointerType === 'mouse') canvas.style.cursor = pick(e) ? 'pointer' : '';
  }, { passive: true });
  canvas.addEventListener('click', (e) => {
    const w = pick(e);
    if (!w) return;
    w.screen.tap(performance.now());
    onTap && onTap(w === watches[0] ? 'round' : 'square');
  });

  let visible = true;
  new IntersectionObserver(([en]) => { visible = en.isIntersecting; if (visible) loop(); }).observe(canvas);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) loop(); });

  let raf = 0, lastDraw = 0;
  const col = new Color();
  function frame(now) {
    raf = 0;
    if (!visible || document.hidden) return;
    const t = now / 1000;
    if (now - lastDraw > 33) { sRound.draw(now); sSquare.draw(now); lastDraw = now; }
    cur.lerp(target, 0.05);
    rig.rotation.y = reduce ? 0 : cur.x * 0.28;
    rig.rotation.x = reduce ? 0 : cur.y * 0.14;
    for (let i = 0; i < watches.length; i++) {
      const w = watches[i];
      const float = reduce ? 0 : Math.sin(t * 0.8 + i * 1.7) * 0.06;
      w.inner.position.y = float;
      w.inner.rotation.y = reduce ? 0 : Math.sin(t * 0.45 + i * 2.1) * 0.08;
      // buzz: short shake when the rest ends; tick: tiny nudge in the last 3 s
      let shake = 0;
      if (!reduce && w.buzzT > 0 && now - w.buzzT < 700) shake = Math.sin((now - w.buzzT) * 0.12) * 0.035 * (1 - (now - w.buzzT) / 700);
      if (!reduce && w.tickT && now - w.tickT < 160) shake += Math.sin((now - w.tickT) * 0.2) * 0.008;
      w.inner.position.x = shake;
      col.set(w.screen.accent());
      w.light.color.lerp(col, 0.08);
    }
    renderer.render(scene, camera);
    loop();
  }
  function loop() { if (!raf) raf = requestAnimationFrame(frame); }
  loop();
  return true;
}
