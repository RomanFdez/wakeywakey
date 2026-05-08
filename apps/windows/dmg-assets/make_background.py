#!/usr/bin/env python3
"""
Genera el fondo del DMG de WakeyWakey en 1x (540x380) y 2x (1080x760).
Paleta: amarillo limón #FFE03A · azul noche #1A1A2E. Fuente: Nunito ExtraBold (variable, weight=800).

Salida:
- background.png (1x)
- background@2x.png (2x)
- background.tiff (multi-resolution para Retina, generado luego con tiffutil)
"""
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

HERE = Path(__file__).parent
FONT = HERE / "Nunito.ttf"

YELLOW = (255, 224, 58, 255)   # #FFE03A
NAVY   = (26, 26, 46, 255)     # #1A1A2E
NAVY_SOFT = (26, 26, 46, 90)   # navy con alpha para hint sutil

# Posiciones lógicas (1x). El icono de la app y el shortcut a Applications
# se colocan luego con create-dmg en estas mismas coordenadas.
APP_X, APP_Y = 140, 195
APPS_X, APPS_Y = 400, 195
ICON_R = 64  # radio aproximado de los iconos (Finder los muestra ~96-128)

def render(scale: int, out: Path):
    W, H = 540 * scale, 380 * scale
    img = Image.new("RGBA", (W, H), YELLOW)
    d = ImageDraw.Draw(img)

    f_title = ImageFont.truetype(str(FONT), 26 * scale); f_title.set_variation_by_axes([800])
    f_hint  = ImageFont.truetype(str(FONT), 20 * scale); f_hint.set_variation_by_axes([700])

    # Título sutil arriba: "WakeyWakey" — recordatorio de marca.
    txt = "WakeyWakey"
    bbox = d.textbbox((0, 0), txt, font=f_title)
    tw = bbox[2] - bbox[0]
    d.text(((W - tw) / 2, 28 * scale), txt, font=f_title, fill=NAVY)

    # Halo sutil debajo de los iconos (donde Finder los pinta).
    for cx, cy in [(APP_X * scale, APP_Y * scale), (APPS_X * scale, APPS_Y * scale)]:
        d.ellipse(
            [cx - ICON_R * scale, cy - ICON_R * scale,
             cx + ICON_R * scale, cy + ICON_R * scale],
            fill=(255, 255, 255, 110),
        )

    # Flecha curva de izquierda a derecha (estilo arco) — sugiere arrastrar.
    arrow_y = APP_Y * scale
    start_x = (APP_X + ICON_R + 10) * scale
    end_x   = (APPS_X - ICON_R - 10) * scale
    arc_h   = 14 * scale

    # Arco bezier aproximado con polilínea
    import math
    steps = 40
    pts = []
    for i in range(steps + 1):
        t = i / steps
        x = start_x + (end_x - start_x) * t
        y = arrow_y - math.sin(t * math.pi) * arc_h
        pts.append((x, y))

    d.line(pts, fill=NAVY, width=4 * scale, joint="curve")

    # Cabeza de flecha
    ax, ay = pts[-1]
    bx, by = pts[-3]
    import math
    angle = math.atan2(ay - by, ax - bx)
    head_len = 14 * scale
    head_w   = 9 * scale
    p1 = (ax, ay)
    p2 = (ax - head_len * math.cos(angle - math.pi / 7),
          ay - head_len * math.sin(angle - math.pi / 7))
    p3 = (ax - head_len * math.cos(angle + math.pi / 7),
          ay - head_len * math.sin(angle + math.pi / 7))
    d.polygon([p1, p2, p3], fill=NAVY)

    # Texto inferior: "Drag to Applications"
    hint = "Drag to Applications"
    bbox = d.textbbox((0, 0), hint, font=f_hint)
    hw = bbox[2] - bbox[0]
    d.text(((W - hw) / 2, H - 60 * scale), hint, font=f_hint, fill=NAVY)

    img.save(out, "PNG")
    print(f"→ {out}  ({W}x{H})")

if __name__ == "__main__":
    render(1, HERE / "background.png")
    render(2, HERE / "background@2x.png")
