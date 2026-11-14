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
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { MeshoptDecoder } from 'three/examples/jsm/libs/meshopt_decoder.module.js';
import { Box3, BufferAttribute } from 'three';

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
  constructor(w, h, round, nextGif, startDelay, noGif = false) {
    this.w = w; this.h = h; this.round = round; this.nextGif = nextGif; this.noGif = noGif;
    this.canvas = document.createElement('canvas');
    this.canvas.width = w; this.canvas.height = h;
    this.ctx = this.canvas.getContext('2d');
    this.texture = new CanvasTexture(this.canvas);
    this.texture.colorSpace = SRGBColorSpace;
    this.texture.anisotropy = 4;
    // same canvas for glTF screens (their UVs are not flipped)
    this.glTexture = new CanvasTexture(this.canvas);
    this.glTexture.colorSpace = SRGBColorSpace;
    this.glTexture.anisotropy = 4;
    this.glTexture.flipY = false;
    this.video = null;
    this.mode = 'idle';
    this.start = performance.now() + startDelay;
    this.lastTick = -1;
    this.onBuzz = null;
    this.onTick = null;
    this.swapGif();
  }
  swapGif() {
    if (this.noGif) return; // the Apple Watch app has no GIF background
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
    this.glTexture.needsUpdate = true;
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

// ---------- real models ----------
const MODELS = {
  galaxy: { url: 'models/galaxy.glb', screenMat: 'screen', screenSize: 1.71, hideMats: ['glass', 'mat_gray_dark'], hideMeshes: [] },
  apple: { url: 'models/applewatch.glb', screenMat: 'Material.001', screenSize: 1.42, hideMats: ['Material.004'], hideMeshes: ['Glass', 'Cap'] },
};
const glass = () => glassMat();
function styleGalaxy(m) {
  const n = m.name;
  const metal = (color, rough) => new MeshStandardMaterial({ color, metalness: 1, roughness: rough });
  if (n === 'metal_black' || n.startsWith('metal_sapphire_blue') || n === 'chrome_sapphire_blue') return metal(0x3a3e45, 0.32);
  if (n === 'chrome' || n === 'chrome_flash') return metal(0xc9cdd3, 0.18);
  if (n === 'silicon_sapphire_blue') return new MeshPhysicalMaterial({ color: 0x166a3a, roughness: 0.55, clearcoat: 0.3, clearcoatRoughness: 0.5, envMapIntensity: 0.6 });
  if (n === 'glass') return glass();
  if (n === 'text_gray') return new MeshStandardMaterial({ color: 0x9aa0a8, roughness: 0.5, metalness: 0.2 });
  if (n === 'mat_gray_dark') return new MeshStandardMaterial({ color: 0x15171a, roughness: 0.35, metalness: 0.6 });
  if (n === 'mat_black' || n === 'metal_black_screw') return new MeshStandardMaterial({ color: 0x0b0c0e, roughness: 0.6 });
  return null;
}
function styleApple(m) {
  const n = m.name;
  if (n === 'Material.008') return new MeshStandardMaterial({ color: 0x2a3038, metalness: 0.9, roughness: 0.32 });
  if (n === 'Material.003') return new MeshPhysicalMaterial({ color: 0x0e0f11, roughness: 0.6, clearcoat: 0.25, clearcoatRoughness: 0.5, envMapIntensity: 0.35 });
  if (n === 'Material.004') return glass();
  if (n === 'Black Glossy' || n === 'Black Glossy2') return new MeshStandardMaterial({ color: 0x030304, roughness: 0.12, metalness: 0 });
  return null;
}
// The screen meshes have no usable UVs. They are flat in local XY, so map x,y straight to u,v.
function planarUV(geo) {
  const p = geo.attributes.position;
  let x0 = Infinity, x1 = -Infinity, y0 = Infinity, y1 = -Infinity;
  for (let i = 0; i < p.count; i++) { const x = p.getX(i), y = p.getY(i); x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
  const uv = new Float32Array(p.count * 2);
  for (let i = 0; i < p.count; i++) { uv[i * 2] = (p.getX(i) - x0) / (x1 - x0); uv[i * 2 + 1] = (p.getY(i) - y0) / (y1 - y0); }
  geo.setAttribute('uv', new BufferAttribute(uv, 2));
}
async function loadModels(sRound, sSquare) {
  const loader = new GLTFLoader().setMeshoptDecoder(MeshoptDecoder);
  const prep = async (cfg, screen, style) => {
    const gltf = await loader.loadAsync(cfg.url);
    const root = gltf.scene;
    let scr = null;
    root.traverse((o) => {
      if (!o.isMesh) return;
      // the models' own front glass and covers would hide the screen; the screen draws its own glass stripe
      if (cfg.hideMats.includes(o.material.name) || cfg.hideMeshes.includes(o.name)) { o.visible = false; return; }
      if (o.material.name === cfg.screenMat) {
        planarUV(o.geometry);
        o.material = new MeshBasicMaterial({ map: screen.texture, toneMapped: false });
        scr = o;
      } else {
        const mm = style(o.material);
        if (mm) o.material = mm;
        o.material.side = DoubleSide; // the FBX import has mirrored faces
      }
    });
    if (!scr) throw new Error('no screen in ' + cfg.url);
    root.updateMatrixWorld(true);
    const box = new Box3().setFromObject(scr);
    const size = Math.max(box.max.x - box.min.x, box.max.y - box.min.y);
    const k = cfg.screenSize / size;
    const g = new Group();
    root.scale.setScalar(k);
    const c = box.getCenter(new Vector3());
    root.position.set(-c.x * k, -c.y * k, -c.z * k + 0.25);
    g.add(root);
    return g;
  };
  return Promise.all([prep(MODELS.galaxy, sRound, styleGalaxy), prep(MODELS.apple, sSquare, styleApple)]);
}

// ---------- scene ----------
export function startHero({ canvas, stage, gifIds, onTap }) {
  let renderer;
  try {
    renderer = new WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: 'high-performance' });
  } catch (e) {
    return false;
  }
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
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
  const sSquare = new Screen(432, 528, false, nextGif, 4600, true);
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Real watch models (CC BY, see page footer). The code-made watches are the fallback.
  const wRound = new Group(), wSquare = new Group();
  let modelsIn = false;
  const fallback = setTimeout(() => {
    if (modelsIn) return;
    wRound.add(roundWatch(sRound)); wSquare.add(squareWatch(sSquare));
  }, 6000);
  loadModels(sRound, sSquare).then(([g1, g2]) => {
    modelsIn = true; clearTimeout(fallback);
    wRound.clear(); wSquare.clear();
    wRound.add(g1); wSquare.add(g2);
  }).catch((e) => { console.warn('watch models', e); });
  if (location.hostname === 'localhost') window.__heroScene = scene; // test harness only
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

  // The canvas covers the whole top area, so a tilted watch never hits an edge.
  // The scene is framed on the .stage3d box with a view offset.
  function resize() {
    const r = canvas.getBoundingClientRect();
    const wpx = Math.max(1, r.width), hpx = Math.max(1, r.height);
    renderer.setSize(wpx, hpx, false);
    camera.aspect = wpx / hpx;
    const t = Math.tan(MathUtils.degToRad(camera.fov / 2));
    const sr = stage.getBoundingClientRect();
    const sw = Math.max(1, sr.width), sh = Math.max(1, sr.height);
    const scx = sr.left + sw / 2 - r.left, scy = sr.top + sh / 2 - r.top;
    const small = sw < 600;
    const needW = small ? 4.7 : 4.0, needH = small ? 4.0 : 3.4;
    const d = Math.max(needW * hpx / (2 * t * sw), needH * hpx / (2 * t * sh)) + 0.4;
    camera.position.set(0, 0, d);
    camera.lookAt(0, 0, 0);
    camera.setViewOffset(wpx, hpx, wpx / 2 - scx, hpx / 2 - scy, wpx, hpx);
    camera.updateProjectionMatrix();
  }
  const ro = new ResizeObserver(resize); ro.observe(canvas); ro.observe(stage);
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
    if (e.pointerType === 'mouse') document.body.style.cursor = (!e.target.closest('a,button,input,label') && pick(e)) ? 'pointer' : '';
  }, { passive: true });
  // the canvas lets clicks through to the page; a click that lands on a watch taps it
  window.addEventListener('click', (e) => {
    if (e.target.closest('a,button,input,label,dialog')) return;
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
