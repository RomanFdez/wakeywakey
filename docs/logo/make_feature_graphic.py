#!/usr/bin/env python3
"""
Genera el feature graphic de WakeyWakey para Google Play Store (1024×500).

Composición:
  Izquierda — logo WW + título + tagline + 4 pills de features
  Derecha   — mockup de teléfono con la pantalla de alerta real

Uso:   python make_feature_graphic.py
Salida: feature_graphic.png (sobreescribe el existente en docs/logo/)
"""
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

HERE     = Path(__file__).parent
FONT     = HERE / "../../apps/windows/dmg-assets/Nunito.ttf"
LOGO     = HERE / "logo_ww_rockwell_square.png"
ALERT    = HERE / "../capturas/shared image (1).png"
OUT      = HERE / "feature_graphic.png"

# ── Paleta ────────────────────────────────────────────────────────────────────
NAVY        = (26,  26,  46)
NAVY_DARK   = (14,  14,  28)
NAVY_PHONE  = (12,  12,  26)
YELLOW      = (255, 224,  58)
WHITE       = (255, 255, 255)
GRAY        = (170, 170, 195)
PILL_BG     = ( 38,  38,  68)
PILL_BORDER = ( 90,  90, 140)

W, H = 1024, 500


def font(size: int, weight: int = 700):
    f = ImageFont.truetype(str(FONT), size)
    try:
        f.set_variation_by_axes([weight])
    except Exception:
        pass
    return f


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0]-1, size[1]-1], radius=radius, fill=255)
    return mask


def draw_pill(d: ImageDraw.ImageDraw, x, y, label, f):
    # label tiene formato "•  Texto" — pintamos el bullet en amarillo
    dot, _, text = label.partition("  ")
    dot_bbox  = d.textbbox((0, 0), dot,  font=f)
    text_bbox = d.textbbox((0, 0), text, font=f)
    dot_w  = dot_bbox[2]  - dot_bbox[0]
    text_w = text_bbox[2] - text_bbox[0]
    th     = text_bbox[3] - text_bbox[1]
    gap    = 6
    ph     = 34
    pw     = 14 + dot_w + gap + text_w + 14
    d.rounded_rectangle([x, y, x+pw, y+ph], radius=17,
                        fill=PILL_BG, outline=PILL_BORDER, width=1)
    ty = y + (ph - th) // 2 - 1
    d.text((x+14, ty),              dot,  font=f, fill=YELLOW)
    d.text((x+14+dot_w+gap, ty),    text, font=f, fill=WHITE)
    return pw


def make():
    img = Image.new("RGB", (W, H), NAVY)
    d   = ImageDraw.Draw(img)

    # ── Fondo: degradado suave izq→der ────────────────────────────────
    for x in range(W):
        t = x / W
        r = int(NAVY[0] * (1-t*0.35))
        g = int(NAVY[1] * (1-t*0.35))
        b = int(NAVY[2] * (1-t*0.30))
        d.line([(x, 0), (x, H)], fill=(r, g, b))

    # ── Halo amarillo izquierda ────────────────────────────────────────
    glow = Image.new("RGBA", (W, H), (0,0,0,0))
    gd   = ImageDraw.Draw(glow)
    gd.ellipse([-60, H//2-190, 340, H//2+190], fill=(*YELLOW, 14))
    gd.ellipse([-20, H//2-140, 280, H//2+140], fill=(*YELLOW,  8))
    img = Image.alpha_composite(img.convert("RGBA"), glow).convert("RGB")
    d   = ImageDraw.Draw(img)

    # ── Logo ──────────────────────────────────────────────────────────
    logo     = Image.open(LOGO).convert("RGBA")
    logo_sz  = 82
    logo     = logo.resize((logo_sz, logo_sz), Image.LANCZOS)
    LPAD     = 48
    logo_y   = 52
    img.paste(logo, (LPAD, logo_y), logo.split()[3])

    # ── Título ────────────────────────────────────────────────────────
    f_title = font(60, 800)
    title_y = logo_y + logo_sz + 12
    d.text((LPAD, title_y), "WakeyWakey", font=f_title, fill=YELLOW)

    # ── Tagline ───────────────────────────────────────────────────────
    f_tag   = font(22, 500)
    tag_y   = title_y + 72
    d.text((LPAD, tag_y),
           "Never miss the start of a meeting again.",
           font=f_tag, fill=GRAY)

    # ── Pills (2 filas × 2) ───────────────────────────────────────────
    f_pill  = font(17, 600)
    pills   = [
        ("•  Full-screen alert",   "•  Configurable timing"),
        ("•  Google Calendar",     "•  Snooze & dismiss"),
    ]
    py = tag_y + 46
    for row in pills:
        px = LPAD
        for label in row:
            pw = draw_pill(d, px, py, label, f_pill)
            px += pw + 10
        py += 44

    # ── Mockup de teléfono ────────────────────────────────────────────
    PX, PY   = 618, 14          # esquina superior izquierda del marco
    PW, PH   = 340, 472         # tamaño del marco (ratio ~0.72 → más teléfono)
    CORNER   = 40
    BORDER   = 9
    NOTCH_H  = 26               # barra de estado encima del screenshot
    BTN_R    = 11               # radio del botón home

    # Marco exterior (phone body)
    d.rounded_rectangle(
        [PX, PY, PX+PW, PY+PH],
        radius=CORNER, fill=NAVY_PHONE,
        outline=(55, 55, 90), width=2
    )

    # Área de pantalla
    SX = PX + BORDER
    SY = PY + BORDER
    SW = PW - BORDER*2
    SH = PH - BORDER*2

    # Screenshot: cargar → recortar la alerta (parte superior) → escalar
    shot  = Image.open(ALERT).convert("RGB")
    sw, sh = shot.size                       # 1080 × 2400
    crop_h = int(sw * SH / SW)              # height que mantiene el aspect ratio de la pantalla
    crop_h = min(crop_h, int(sh * 0.68))    # no bajar del reloj y los botones
    crop   = shot.crop((0, 0, sw, crop_h)).resize((SW, SH), Image.LANCZOS)

    # Pegar el screenshot con esquinas redondeadas
    inner_r = CORNER - BORDER
    mask    = rounded_mask((SW, SH), inner_r)
    img.paste(crop, (SX, SY), mask)

    # Barra de estado encima (oscurece el status bar del screenshot)
    d.rectangle([SX, SY, SX+SW, SY+NOTCH_H], fill=NAVY_PHONE)

    # Botón home abajo (decorativo)
    bcx = PX + PW//2
    bcy = PY + PH - 15
    d.ellipse([bcx-BTN_R, bcy-BTN_R, bcx+BTN_R, bcy+BTN_R],
              outline=(70, 70, 110), width=2)

    # Líneas de volumen (decorativas, lado izquierdo del teléfono)
    for vy in [PY+80, PY+110, PY+140]:
        d.rounded_rectangle([PX-5, vy, PX+1, vy+22], radius=3, fill=(50, 50, 85))

    # ── Guardar ───────────────────────────────────────────────────────
    img.save(str(OUT), "PNG")
    print(f"✓ {OUT}  ({W}×{H})")


if __name__ == "__main__":
    make()
