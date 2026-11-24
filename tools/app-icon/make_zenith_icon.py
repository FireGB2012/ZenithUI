"""Renders the new Zenith app icon options, their Android layers and preview sheets.

    python3 make_zenith_icon.py OUT_DIR            # all four options + both sheets
    python3 make_zenith_icon.py OUT_DIR planet     # one option, no sheets
    S=900 python3 make_zenith_icon.py OUT_DIR      # quicker, softer draft

Needs numpy, scipy and Pillow. glass.py (the Zenith Plus icon's glass), adaptive.py,
shapes.py, looks.py and brand_bg.png sit next to this file. The home screen sheet
also reads a Zenith home screen shot (HOME_SHOT, from the theme rig); without one it is skipped.

Each option is drawn on the 108 dp adaptive canvas at S px, then split into the
layers Android wants: background (the face), foreground (the glass symbol, its
shadow and light, with alpha), monochrome (a flat silhouette for themed icons).
"""
import os
import sys
import time

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from adaptive import (VIS, Piece, coverage, mask_circle, mask_squircle, render,  # noqa: E402
                      resize_rgb, resize_rgba, split_foreground, to_image)
from looks import LOOKS  # noqa: E402
import shapes as sh  # noqa: E402

S = int(os.environ.get("S", "2048"))
LAYER = 432                      # 108 dp at xxxhdpi, what drawable-nodpi holds today
HOME_SHOT = os.environ.get(
    "HOME_SHOT", "/mnt/project-files/zenith-glass-themes/shots/after-home-frost-dark.png")


# ------------------------------------------------------------------ symbols

def glass_z(r=0.068, a=0.195):
    z = sh.polyline([(-a, -a), (a, -a), (-a, a), (a, a)], r, 0.02)
    return [Piece(z, r)], z


def rising_z(r=0.056, a=0.205, tick=0.205):
    z = sh.polyline([(-a, -a), (a, -a), (-a, a), (a, a)], r, 0.018)
    t = sh.polyline([(a, -a), (a, -a + tick)], r)

    def f(X, Y, T):
        return sh.smin(z(X, Y, T), t(X, Y, T), 0.006 * T)
    return [Piece(f, r)], f


PLANET = dict(cy=0.012, rot=-16.0, A=0.37, B=0.090, rr=0.033, Rs=0.178)


def planet_pieces(glow, aura):
    p = PLANET
    ring = Piece(sh.ellipse_ring(0, p["cy"], p["A"], p["B"], p["rot"], p["rr"]), p["rr"], melt=0.2)
    ball = Piece(sh.circle(0, p["cy"], p["Rs"]), p["Rs"], lift=0.85, melt=0, glow=glow, glow_amt=0.18,
                 aura=aura, aura_amt=0.5)
    return ring, ball


def render_planet(S, look, face_uv, glow, aura):
    """Ring, then the ball over it, then the ring's near half again over the ball."""
    p = PLANET
    ring, ball = planet_pieces(glow, aura)
    p1 = render(S, look, [ring], face_uv=face_uv)
    p2 = render(S, look, [ball], face_uv=face_uv, under=p1["rgb"])
    p3 = render(S, look, [ring], face_uv=face_uv, under=p2["rgb"])
    T = S * VIS
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
    X, Y = xx - c, yy - c
    disc = np.clip(0.5 - (np.hypot(X, Y - p["cy"] * T) - (p["Rs"] * T + 1.5)) / 1.5, 0, 1)
    front = np.clip(sh.ellipse_front(0, p["cy"], p["rot"])(X, Y, T) / 0.02 + 0.5, 0, 1)
    w = (disc * front)[..., None]
    rgb = p2["rgb"] * (1 - w) + p3["rgb"] * w
    mask = np.maximum(p1["mask"], p2["mask"])
    return dict(face=p1["face"], rgb=rgb, mask=mask)


def planet_mono(S):
    """Flat planet for themed icons: the near half of the ring cuts a gap through the ball."""
    p = PLANET
    T = S * VIS
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
    X, Y = xx - c, yy - c
    ring_d = sh.ellipse_ring(0, p["cy"], p["A"], p["B"], p["rot"], p["rr"])(X, Y, T)
    ball_d = sh.circle(0, p["cy"], p["Rs"])(X, Y, T)
    front = np.clip(sh.ellipse_front(0, p["cy"], p["rot"])(X, Y, T) / 0.02 + 0.5, 0, 1)
    gap = 0.020 * T
    cut = coverage(ring_d - gap) * front
    ball = coverage(ball_d) * (1 - cut)
    behind = coverage(ball_d - gap) * (1 - front)            # far half hides behind the ball
    ring = coverage(ring_d) * (1 - behind)
    return np.maximum(ball, ring)


GLOW = {"pastel": (1.0, 0.95, 1.0), "midnight": (1.0, 0.92, 1.0), "aurora": (0.85, 1.0, 0.95),
        "frost": (0.95, 0.92, 1.0)}
AURA = {"pastel": (0.20, 0.16, 0.30), "midnight": (0.36, 0.28, 0.70), "aurora": (0.16, 0.46, 0.40),
        "frost": (0.10, 0.08, 0.18)}


def pastel_z():
    """Zenith's gradient, with a touch more ink in the glass so the Z stands off it."""
    lk = LOOKS["pastel"]()
    lk.glass_white = 0.08
    lk.body_tint = np.array([0.84, 0.84, 1.0])
    lk.shadow_strength = 0.62
    lk.edge_tint = np.array([0.20, 0.20, 0.62])
    return lk


OPTIONS = [
    dict(n=1, key="glass-z", label="Glass Z", note="Zenith's colours, a clean glass Z",
         look=pastel_z, look_name="pastel", symbol="glass_z", face_uv="canvas"),
    dict(n=2, key="rising-z", label="Rising Z", note="Today's arrow idea, drawn as a Z",
         look=LOOKS["midnight"], look_name="midnight", symbol="rising_z"),
    dict(n=3, key="planet", label="Planet", note="Zenith is the top of the sky",
         look=LOOKS["aurora"], look_name="aurora", symbol="planet"),
    dict(n=4, key="frost-planet", label="Frost Planet", note="The planet in white, iOS 26 light",
         look=LOOKS["frost"], look_name="frost", symbol="planet"),
]


def render_option(opt, S):
    look = opt["look"]()
    face_uv = opt.get("face_uv", "visible")
    T = S * VIS
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
    X, Y = xx - c, yy - c
    if opt["symbol"] == "planet":
        res = render_planet(S, look, face_uv, GLOW[opt["look_name"]], AURA[opt["look_name"]])
        mono = planet_mono(S)
    else:
        pieces, sdf = glass_z() if opt["symbol"] == "glass_z" else rising_z()
        res = render(S, look, pieces, face_uv=face_uv)
        mono = coverage(sdf(X, Y, T))
    res["mono"] = mono
    return res


# ------------------------------------------------------------------ outputs

def crop_visible(arr, S, margin=1.0):
    """The part a mask can show (72 dp), times margin."""
    c0 = int(round(S * (1 - VIS * margin) / 2))
    return arr[c0:S - c0, c0:S - c0]


def masked(rgb_vis, n, kind):
    rgb = resize_rgb(rgb_vis, n)
    mk = {"circle": mask_circle, "squircle": mask_squircle}[kind](n, frac=1.0)
    return np.dstack([rgb, mk])


def write_option(opt, res, out_dir):
    d = os.path.join(out_dir, f"{opt['n']}-{opt['key']}")
    os.makedirs(os.path.join(d, "android-layers"), exist_ok=True)
    rgb, face, mask, mono = res["rgb"], res["face"], res["mask"], res["mono"]
    vis = crop_visible(rgb, S)
    for kind in ("squircle", "circle"):
        to_image(masked(vis, 1024, kind)).save(os.path.join(d, f"{opt['key']}-{kind}-1024.png"), optimize=True)
    to_image(resize_rgb(vis, 512)).save(os.path.join(d, f"{opt['key']}-play-store-512.png"), optimize=True)
    fg = split_foreground(face, rgb, mask)
    lay = os.path.join(d, "android-layers")
    to_image(resize_rgb(face, LAYER)).save(os.path.join(lay, "ic_launcher_layer_background.png"), optimize=True)
    to_image(resize_rgba(fg, LAYER)).save(os.path.join(lay, "ic_launcher_layer_foreground.png"), optimize=True)
    mono_rgba = np.dstack([np.ones(mono.shape + (3,)), mono])
    to_image(resize_rgba(mono_rgba, LAYER), dither=False).save(
        os.path.join(lay, "ic_launcher_layer_monochrome.png"), optimize=True)
    return d


def check_layers(d, res):
    """Lay the saved foreground over the saved background; it must match the render."""
    lay = os.path.join(d, "android-layers")
    bg = np.asarray(Image.open(os.path.join(lay, "ic_launcher_layer_background.png")).convert("RGB")) / 255.0
    fg = np.asarray(Image.open(os.path.join(lay, "ic_launcher_layer_foreground.png")).convert("RGBA")) / 255.0
    comp = fg[..., :3] * fg[..., 3:] + bg * (1 - fg[..., 3:])
    want = resize_rgb(res["rgb"], LAYER)
    err = np.abs(comp - want)
    return float(err.max()), float(err.mean())


# ------------------------------------------------------------------ sheets

def font(size, weight="SemiBold", display=False):
    fam = "InterDisplay" if display else "Inter"
    return ImageFont.truetype(f"/usr/share/fonts/opentype/inter/{fam}-{weight}.otf", size)


def backdrop(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    a = np.array([10, 11, 24]) / 255.0
    b = np.array([22, 16, 51]) / 255.0
    c = np.array([8, 17, 31]) / 255.0
    t = (xx / w * 0.55 + yy / h * 0.45)[..., None]
    col = np.where(t < 0.5, a + (b - a) * (t / 0.5), b + (c - b) * ((t - 0.5) / 0.5))
    return Image.fromarray((col * 255).astype(np.uint8), "RGB").convert("RGBA")


def with_shadow(icon, radius=18, alpha=120, dy=10):
    pad = radius * 3
    w, h = icon.size
    canvas = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    sh_ = Image.new("RGBA", icon.size, (4, 3, 14, 0))
    sh_.putalpha(icon.getchannel("A").point(lambda v: v * alpha // 255))
    canvas.alpha_composite(sh_, (pad, pad + dy))
    canvas = canvas.filter(ImageFilter.GaussianBlur(radius))
    canvas.alpha_composite(icon, (pad, pad))
    return canvas, pad


def themed(mono_vis, n, dark=True):
    """How Android 13+ draws a themed icon: the silhouette in one tone on a tinted disc."""
    m = resize_rgb(np.dstack([mono_vis] * 3), n)[..., 0]
    bg = np.array([0x22, 0x2B, 0x45]) / 255.0 if dark else np.array([0xDC, 0xE4, 0xFF]) / 255.0
    fg = np.array([0xC6, 0xD4, 0xFF]) / 255.0 if dark else np.array([0x2B, 0x3A, 0x6E]) / 255.0
    rgb = bg * (1 - m[..., None]) + fg * m[..., None]
    return np.dstack([rgb, mask_circle(n, frac=1.0)])


def options_sheet(results, out_dir):
    W, H = 2000, 1500
    img = backdrop(W, H)
    d = ImageDraw.Draw(img)
    d.text((80, 64), "New Zenith icon", font=font(68, "SemiBold", display=True), fill=(255, 255, 255, 255))
    d.text((83, 152), "Same liquid glass as the Plus icon. Big ones are the Samsung shape, then a Pixel "
           "circle and a themed icon.", font=font(28, "Regular"), fill=(200, 200, 225, 255))
    big = 400
    gap = (W - 160 - big * 4) // 3
    top = 250
    for i, (opt, res) in enumerate(results):
        x = 80 + i * (big + gap)
        vis = crop_visible(res["rgb"], S)
        sq = to_image(masked(vis, big, "squircle"))
        ic, pad = with_shadow(sq)
        img.alpha_composite(ic, (x - pad, top - pad))
        d.text((x + big // 2, top + big + 36), f"{opt['n']}  {opt['label']}", font=font(40, "SemiBold"),
               fill=(255, 255, 255, 255), anchor="mt")
        d.text((x + big // 2, top + big + 94), opt["note"], font=font(23, "Regular"),
               fill=(185, 185, 212, 255), anchor="mt")
        # pixel circle and themed icon, side by side
        sm = 170
        sgap = big - 2 * sm
        cy = top + big + 160
        circ = to_image(masked(vis, sm, "circle"))
        ic2, pad2 = with_shadow(circ, radius=10, alpha=110, dy=6)
        img.alpha_composite(ic2, (x - pad2, cy - pad2))
        mono_vis = crop_visible(res["mono"], S)
        th = to_image(themed(mono_vis, sm), dither=False)
        ic3, pad3 = with_shadow(th, radius=10, alpha=110, dy=6)
        img.alpha_composite(ic3, (x + sm + sgap - pad3, cy - pad3))
        d.text((x + sm // 2, cy + sm + 16), "Pixel", font=font(22, "Medium"), fill=(170, 170, 200, 255),
               anchor="mt")
        d.text((x + sm + sgap + sm // 2, cy + sm + 16), "Themed", font=font(22, "Medium"),
               fill=(170, 170, 200, 255), anchor="mt")
        # real phone sizes: 60 dp and 44 dp at the A55's 2.8x
        ry = cy + sm + 80
        for j, s_ in enumerate((168, 123, 84)):
            t_ = to_image(masked(vis, s_, "squircle"))
            img.alpha_composite(t_, (x + sum((168, 123, 84)[:j]) + j * 30 + (big - 168 - 123 - 84 - 60) // 2,
                                     ry + (168 - s_)))
    d.text((80, H - 110), "Bottom row: the same icon smaller, like in the app drawer and Settings.",
           font=font(26, "Regular"), fill=(160, 160, 190, 255))
    d.text((80, H - 68), "Like a symbol but not its colour? Any symbol can go on any of the four colours.",
           font=font(26, "Regular"), fill=(160, 160, 190, 255))
    path = os.path.join(out_dir, "zenith-icon-options.png")
    img.convert("RGB").save(path, optimize=True)
    return path


def home_sheet(results, out_dir):
    """Each option in the empty fifth row of a real Zenith home screen (theme rig shot)."""
    shot = Image.open(HOME_SHOT).convert("RGBA")
    W0, H0 = shot.size                       # 1233 x 2673, about 3x
    icon_px, cx, cy = 208, 180, 1942          # same size and column as the icons above it
    label_y = cy + 146
    phones = []
    for opt, res in results:
        ph = shot.copy()
        vis = crop_visible(res["rgb"], S)
        sq = to_image(masked(vis, icon_px, "squircle"))
        ic, pad = with_shadow(sq, radius=14, alpha=95, dy=8)
        ph.alpha_composite(ic, (cx - icon_px // 2 - pad, cy - icon_px // 2 - pad))
        dd = ImageDraw.Draw(ph)
        f = font(37, "Medium")
        dd.text((cx + 1, label_y + 2), "Zenith", font=f, fill=(0, 0, 0, 90), anchor="mm")
        dd.text((cx, label_y), "Zenith", font=f, fill=(255, 255, 255, 255), anchor="mm")
        phones.append(ph)
    scale = 0.5
    pw, ph_ = int(W0 * scale), int(H0 * scale)
    gap = 40
    top = 170
    W = 80 * 2 + 4 * pw + 3 * gap
    H = top + ph_ + 110
    img = backdrop(W, H)
    d = ImageDraw.Draw(img)
    d.text((80, 56), "On your home screen", font=font(60, "SemiBold", display=True), fill=(255, 255, 255, 255))
    d.text((82, 128), "In the empty row, next to your other apps. Real Zenith home screen from the theme rig.",
           font=font(26, "Regular"), fill=(200, 200, 225, 255))
    for i, ((opt, _), phone) in enumerate(zip(results, phones)):
        x = 80 + i * (pw + gap)
        small = phone.resize((pw, ph_), Image.LANCZOS)
        m = Image.new("L", (pw, ph_), 0)
        ImageDraw.Draw(m).rounded_rectangle((0, 0, pw - 1, ph_ - 1), 44, fill=255)
        small.putalpha(m)
        img.alpha_composite(small, (x, top))
        d.text((x + pw // 2, top + ph_ + 30), f"{opt['n']}  {opt['label']}", font=font(36, "SemiBold"),
               fill=(255, 255, 255, 255), anchor="mt")
    path = os.path.join(out_dir, "zenith-icon-on-home-screen.png")
    img.convert("RGB").save(path, optimize=True)
    # a close-up of the slot, full size, for judging it next to its neighbours
    crops = []
    for phone in phones:
        crops.append(phone.crop((20, 1310, 640, 2140)))
    cw, ch = crops[0].size
    big = Image.new("RGBA", (cw * 4 + 30 * 3, ch), (10, 11, 24, 255))
    for i, cimg in enumerate(crops):
        big.alpha_composite(cimg, (i * (cw + 30), 0))
    path2 = os.path.join(out_dir, "zenith-icon-on-home-screen-close-up.png")
    big.convert("RGB").save(path2, optimize=True)
    return path, path2


def main():
    out_dir = sys.argv[1]
    only = sys.argv[2:]
    os.makedirs(out_dir, exist_ok=True)
    results = []
    for opt in OPTIONS:
        if only and opt["key"] not in only:
            continue
        t0 = time.time()
        res = render_option(opt, S)
        d = write_option(opt, res, out_dir)
        err = check_layers(d, res)
        print(f"{opt['key']}: {time.time() - t0:.1f}s  layers max err {err[0]:.4f} mean {err[1]:.5f}", flush=True)
        results.append((opt, res))
    if not only:
        print(options_sheet(results, out_dir))
        if os.path.exists(HOME_SHOT):
            print(*home_sheet(results, out_dir))
        else:
            print("no home screen shot at HOME_SHOT, so no home screen sheet")


if __name__ == "__main__":
    main()
