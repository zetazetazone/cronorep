"""Pause Barbell on gradient backgrounds. Run: python gradients.py"""
GLYPH = ('<rect x="40" y="116" width="50" height="24" rx="12"/><rect x="166" y="116" width="50" height="24" rx="12"/>'
         '<rect x="76" y="70" width="38" height="116" rx="19"/><rect x="142" y="70" width="38" height="116" rx="19"/>')
# name: (stops top-left -> bottom-right, glyph colour)
OPTIONS = {
    "1-rest-to-go": (["#4ADE80", "#FACC15", "#FB923C"], "#FFFFFF"),   # the app's REST green into GO orange
    "2-electric":   (["#A3E635", "#10B981", "#0E7490"], "#FFFFFF"),   # lime to deep teal
    "3-aurora":     (["#22D3EE", "#6366F1", "#A855F7"], "#FFFFFF"),   # cyan to violet
}
for name, (stops, fg) in OPTIONS.items():
    n = len(stops) - 1
    st = "".join(f'<stop offset="{i/n:.2f}" stop-color="{c}"/>' for i, c in enumerate(stops))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">'
           f'<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">{st}</linearGradient></defs>'
           f'<rect width="256" height="256" fill="url(#g)"/><g fill="{fg}">{GLYPH}</g></svg>\n')
    open(f"a-gradient-{name}.svg", "w").write(svg)
