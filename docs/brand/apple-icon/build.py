"""Builds the CronoRep icon concepts. Run: python build.py"""
import math
def pt(cx,cy,r,a):
    a=math.radians(a); return cx+r*math.cos(a), cy+r*math.sin(a)
def f(v): return f"{v:.2f}"
def svg(body, bg=None, fg="#000"):
    b = f'<rect width="256" height="256" fill="{bg}"/>' if bg else ''
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">{b}<g fill="{fg}" color="{fg}">{body}</g></svg>\n'

# A — Pause barbell: a barbell whose two plates are a pause sign.
def barbell():
    # Only the bar ends show, so the middle stays an open pause sign (not an H).
    bar = '<rect x="40" y="116" width="50" height="24" rx="12"/><rect x="166" y="116" width="50" height="24" rx="12"/>'
    plates = '<rect x="76" y="70" width="38" height="116" rx="19"/><rect x="142" y="70" width="38" height="116" rx="19"/>'
    return bar + plates

# B — Rest slice: the time disc with the rest slice lifted out of it.
def slice_():
    cx,cy,r=118,142,66; a0,a1=-90,-30
    x0,y0=pt(cx,cy,r,a1); x1,y1=pt(cx,cy,r,a0)
    disc=f'<path d="M{cx},{cy} L{f(x0)},{f(y0)} A{r},{r} 0 1 1 {f(x1)},{f(y1)} Z"/>'
    off=40; ox,oy=pt(0,0,off,(a0+a1)/2)
    sx0,sy0=pt(cx+ox,cy+oy,r,a0); sx1,sy1=pt(cx+ox,cy+oy,r,a1)
    sl=f'<path d="M{f(cx+ox)},{f(cy+oy)} L{f(sx0)},{f(sy0)} A{r},{r} 0 0 1 {f(sx1)},{f(sy1)} Z"/>'
    return f'<g stroke="currentColor" stroke-width="14" stroke-linejoin="round">{disc}{sl}</g>'

# C — Crown C: a heavy C that is also a stopwatch, crown on top.
def crown_c():
    cx,cy,ro,ri=134,140,76,38; a0,a1=-38,38   # opening on the right
    p=lambda r,a: pt(cx,cy,r,a)
    o0=p(ro,a1); o1=p(ro,a0); i0=p(ri,a0); i1=p(ri,a1)
    capr=(ro-ri)/2
    c=(f'<path d="M{f(o0[0])},{f(o0[1])} A{ro},{ro} 0 1 1 {f(o1[0])},{f(o1[1])} '
       f'A{capr},{capr} 0 0 1 {f(i0[0])},{f(i0[1])} '
       f'A{ri},{ri} 0 1 0 {f(i1[0])},{f(i1[1])} '
       f'A{capr},{capr} 0 0 1 {f(o0[0])},{f(o0[1])} Z"/>')
    crown='<rect x="116" y="40" width="36" height="20" rx="10"/><rect x="125" y="52" width="18" height="18"/>'
    return c+crown

for name,fn in [("a-pause-barbell",barbell),("b-rest-slice",slice_),("c-crown-c",crown_c)]:
    open(f"{name}.svg","w").write(svg(fn()))
    open(f"{name}-icon.svg","w").write(svg(fn(),bg="#4ADE80",fg="#0B0F14"))
