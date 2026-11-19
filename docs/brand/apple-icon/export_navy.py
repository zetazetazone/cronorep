"""Writes the chosen Navy icon into the apps. Run from the repo root: python docs/brand/apple-icon/export_navy.py"""
EDGE, MID, GLOW, CHROME = "#1A2466", "#2A3FA8", "#7EA6FF", "#C7D3EE"
# The glyph on the 256 canvas: (x, y, w, h, corner radius).
RECTS = [(40, 116, 50, 24, 12), (166, 116, 50, 24, 12), (76, 70, 38, 116, 19), (142, 70, 38, 116, 19)]

def rrect(x, y, w, h, r, s, t):
    """A rounded rectangle path, scaled by s and moved by t."""
    X, Y, W, H, R = x * s + t, y * s + t, w * s, h * s, r * s
    f = lambda v: f"{v:.3f}".rstrip("0").rstrip(".")
    return (f"M{f(X+R)},{f(Y)}h{f(W-2*R)}a{f(R)},{f(R)} 0 0 1 {f(R)},{f(R)}v{f(H-2*R)}"
            f"a{f(R)},{f(R)} 0 0 1 {f(-R)},{f(R)}h{f(-(W-2*R))}a{f(R)},{f(R)} 0 0 1 {f(-R)},{f(-R)}"
            f"v{f(-(H-2*R))}a{f(R)},{f(R)} 0 0 1 {f(R)},{f(-R)}Z")

def a(c):  # #RRGGBB -> #AARRGGBB for Android
    return "#FF" + c[1:]

def bg_paths(size, shape_path):
    return f'''    <path android:pathData="{shape_path}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial" android:centerX="{size*0.5:g}" android:centerY="{size*0.38:g}" android:gradientRadius="{size*0.75:g}"
                android:startColor="{a(MID)}" android:endColor="{a(EDGE)}" />
        </aapt:attr>
    </path>
    <path android:pathData="{shape_path}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial" android:centerX="{size*0.5:g}" android:centerY="{size*0.05:g}" android:gradientRadius="{size*0.6:g}"
                android:startColor="#BF{GLOW[1:]}" android:endColor="#00{GLOW[1:]}" />
        </aapt:attr>
    </path>
'''

def glyph_path(s, t, top, bottom):
    d = "".join(rrect(*r, s, t) for r in RECTS)
    return f'''    <path android:pathData="{d}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="0" android:startY="{top:g}" android:endX="0" android:endY="{bottom:g}"
                android:startColor="#FFFFFFFF" android:endColor="{a(CHROME)}" />
        </aapt:attr>
    </path>
'''

HEAD = '''<?xml version="1.0" encoding="utf-8"?>
<!-- {note} Source: docs/brand/apple-icon/export_navy.py -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="{size}dp" android:height="{size}dp"
    android:viewportWidth="{size}" android:viewportHeight="{size}">
'''

# Wear OS: round icon on a 48 canvas.
s = 48 / 256
circle = "M24,0a24,24 0,1 1,0 48a24,24 0,1 1,0 -48Z"
open("shared/src/main/res/drawable/ic_launcher.xml", "w", newline="\n").write(
    HEAD.format(note="CronoRep icon: Pause Barbell on Navy.", size=48) + bg_paths(48, circle)
    + glyph_path(s, 0, 70 * s, 186 * s) + "</vector>\n")

# Phone adaptive icon: full-bleed background and the glyph inside the 66 dp safe zone.
square = "M0,0h108v108h-108Z"
open("mobile/src/main/res/drawable/ic_launcher_background.xml", "w", newline="\n").write(
    HEAD.format(note="Adaptive icon background: Navy with a soft glow.", size=108) + bg_paths(108, square) + "</vector>\n")
s2 = 0.34; t2 = (108 - 256 * s2) / 2
open("mobile/src/main/res/drawable/ic_launcher_foreground.xml", "w", newline="\n").write(
    HEAD.format(note="Adaptive icon foreground: the Pause Barbell, inside the safe zone.", size=108)
    + glyph_path(s2, t2, 70 * s2 + t2, 186 * s2 + t2) + "</vector>\n")
