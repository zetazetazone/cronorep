"""Pause Barbell, dark and shiny: deep colour, a soft light glow, a chrome shape. Run: python dark.py
The darkest edge stays a clear colour, never black (watchOS rule)."""
GLYPH = ('<rect x="40" y="116" width="50" height="24" rx="12"/><rect x="166" y="116" width="50" height="24" rx="12"/>'
         '<rect x="76" y="70" width="38" height="116" rx="19"/><rect x="142" y="70" width="38" height="116" rx="19"/>')
# name: (edge colour, middle colour, glow colour, chrome bottom colour)
OPTIONS = {
    "1-navy":    ("#1A2466", "#2A3FA8", "#7EA6FF", "#C7D3EE"),
    "2-plum":    ("#3A1259", "#5B1F94", "#D08BFF", "#DCCBEF"),
    "3-emerald": ("#083F3A", "#0F6B5C", "#5EF2B5", "#C6E8DD"),
}
for name, (edge, mid, glow, chrome) in OPTIONS.items():
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256"><defs>'
           f'<radialGradient id="bg" cx="0.5" cy="0.38" r="0.75">'
           f'<stop offset="0" stop-color="{mid}"/><stop offset="1" stop-color="{edge}"/></radialGradient>'
           f'<radialGradient id="glow" cx="0.5" cy="0.05" r="0.6">'
           f'<stop offset="0" stop-color="{glow}" stop-opacity="0.75"/><stop offset="1" stop-color="{glow}" stop-opacity="0"/></radialGradient>'
           f'<linearGradient id="chrome" x1="0" y1="0" x2="0" y2="1" gradientUnits="userSpaceOnUse" x2="0" y2="186">'
           f'<stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="{chrome}"/></linearGradient>'
           f'</defs><rect width="256" height="256" fill="url(#bg)"/><rect width="256" height="256" fill="url(#glow)"/>'
           f'<g fill="url(#chrome)">{GLYPH}</g></svg>\n')
    svg = svg.replace('x1="0" y1="0" x2="0" y2="1" gradientUnits="userSpaceOnUse" x2="0" y2="186"',
                      'x1="0" y1="70" x2="0" y2="186" gradientUnits="userSpaceOnUse"')
    open(f"a-dark-{name}.svg", "w").write(svg)
