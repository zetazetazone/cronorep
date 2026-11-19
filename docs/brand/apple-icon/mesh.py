"""Pause Barbell on mesh-style gradients (layered soft colour glows). Run: python mesh.py"""
GLYPH = ('<rect x="40" y="116" width="50" height="24" rx="12"/><rect x="166" y="116" width="50" height="24" rx="12"/>'
         '<rect x="76" y="70" width="38" height="116" rx="19"/><rect x="142" y="70" width="38" height="116" rx="19"/>')
# name: base colour, then glows as (cx, cy, radius, colour), all in 0..1 of the canvas.
OPTIONS = {
    "1-volt":   ("#0F766E", [(0.15, 0.10, 0.75, "#A3E635"), (0.95, 0.95, 0.70, "#FB923C"), (0.90, 0.10, 0.55, "#22D3EE")]),
    "2-ultra":  ("#3730A3", [(0.10, 0.10, 0.75, "#22D3EE"), (0.95, 0.90, 0.75, "#EC4899"), (0.85, 0.15, 0.55, "#8B5CF6")]),
    "3-blaze":  ("#9D174D", [(0.15, 0.05, 0.75, "#FB923C"), (0.95, 0.95, 0.70, "#7C3AED"), (0.90, 0.20, 0.50, "#F43F5E")]),
}
for name, (base, glows) in OPTIONS.items():
    defs = "".join(
        f'<radialGradient id="g{i}" cx="{cx}" cy="{cy}" r="{r}">'
        f'<stop offset="0" stop-color="{c}"/><stop offset="1" stop-color="{c}" stop-opacity="0"/></radialGradient>'
        for i, (cx, cy, r, c) in enumerate(glows))
    layers = "".join(f'<rect width="256" height="256" fill="url(#g{i})"/>' for i in range(len(glows)))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">'
           f'<defs>{defs}</defs><rect width="256" height="256" fill="{base}"/>{layers}'
           f'<g fill="#FFFFFF">{GLYPH}</g></svg>\n')
    open(f"a-mesh-{name}.svg", "w").write(svg)
