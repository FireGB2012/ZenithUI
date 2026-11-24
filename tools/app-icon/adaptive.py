"""Zenith app icon renderer: glass symbols on a full-bleed face, split into
Android adaptive icon layers (background, foreground, monochrome).

The canvas is the 108 dp adaptive layer. T is the 72 dp part a mask shows, and
symbols are sized in T. Android only promises the middle 66 dp circle
(0.458 T from the centre) survives every mask, so symbols stay inside it.
Built on glass.py from the Zenith Plus icon (same light, same glass).
"""
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

from glass import (KEY, coverage, environment, height_rounded, hue_rgb, normalize3,
                   normals_from_height, sample, shift, smooth_noise, smoothstep, blur)

VIS = 72.0 / 108.0
SAFE = 33.0 / 72.0          # safe-zone radius in T


class Piece:
    """One glass part of a symbol.

    sdf(X, Y, T) -> signed distance in px (negative inside).
    r    the half-thickness of the part in T, the radius of its rounded edge.
    elev how far above the face it floats (1 = resting on it), for its shadow.
    glow an RGB light inside the glass (a lit core), or None.
    aura an RGB bloom the part throws onto the face around it, or None.
    """

    def __init__(self, sdf, r, lift=0.9, elev=1.0, glow=None, glow_amt=0.0, aura=None,
                 aura_amt=1.0, melt=0.30, tint=None, white=None):
        self.sdf = sdf
        self.r = r
        self.lift = lift
        self.elev = elev
        self.glow = None if glow is None else np.array(glow, dtype=np.float64)
        self.glow_amt = glow_amt
        self.aura = None if aura is None else np.array(aura, dtype=np.float64)
        self.aura_amt = aura_amt
        self.melt = melt
        self.tint = None if tint is None else np.array(tint, dtype=np.float64)
        self.white = white


def render(S, look, pieces, refract=1.0, shadow_off=(0.016, 0.034), caustic_off=(0.030, 0.050),
           rainbow=0.85, seed=7, face_uv="visible", face_fn=None, under=None):
    """Returns dict(face, rgb, mask) as float arrays (S x S x 3, S x S x 3, S x S)."""
    T = S * VIS
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
    X, Y = xx - c, yy - c
    kx, ky, kz = [float(v) for v in KEY]
    kdir = np.array([kx, ky]) / np.hypot(kx, ky)

    if face_uv == "canvas":
        U, V = xx / (S - 1), yy / (S - 1)
    else:
        U, V = X / T + 0.5, Y / T + 0.5
    face = look.face(U, V)
    if face_fn is not None:
        face = face_fn(face, U, V, X, Y, T)
    face0 = face.copy()
    if under is not None:            # glass laid over an earlier pass: refract that instead
        face = under.copy()

    # ---- the parts
    ds, ms, hs, rbs = [], [], [], []
    for p in pieces:
        d = p.sdf(X, Y, T)
        rb = p.r * T
        d_shape = ndi.gaussian_filter(d, rb * p.melt) if p.melt else d
        h = height_rounded(d_shape, rb, p.lift)
        ds.append(d)
        ms.append(coverage(d))
        hs.append(h)
        rbs.append(rb)
    D = np.stack(ds)
    owner = np.argmin(D, axis=0)
    d_sym = D.min(axis=0)
    m_sym = np.max(np.stack(ms), axis=0)
    h_sym = np.max(np.stack(hs), axis=0)
    h_sym = ndi.gaussian_filter(h_sym, S / 1400)
    rb_map = np.array(rbs)[owner]
    t_sym = np.clip(-d_sym, 0, None)

    snx, sny, snz = normals_from_height(h_sym)
    tilt = 1 - np.clip(snz, 0, 1)
    nxy = np.sqrt(snx ** 2 + sny ** 2) + 1e-9
    to_key = (snx * kdir[0] + sny * kdir[1]) / np.maximum(nxy, 0.12)

    # ---- what the parts do to the face: shadow, halo, bloom, caustic
    shade = np.zeros((S, S))
    for p, m in zip(pieces, ms):
        e = p.elev
        sdx, sdy = shadow_off[0] * T * e, shadow_off[1] * T * e
        sh = blur(shift(m, sdy, sdx), T * 0.030 * e ** 0.8)
        halo = blur(m, T * 0.014)
        s = np.clip(sh * 0.80 / e ** 0.5 + halo * 0.45 / e, 0, 1)
        s = np.maximum(s, blur(m, T * 0.03) * 0.25 / e)
        shade = 1 - (1 - shade) * (1 - s)
    shade = shade * (1 - m_sym)
    face = face * (1 - look.shadow_strength * shade[..., None] * (1 - look.shadow_tint))
    for p, m in zip(pieces, ms):
        if p.aura is not None:
            bloom = blur(m, T * 0.07) * 0.8 + blur(m, T * 0.022) * 0.6
            face = face + p.aura * (p.aura_amt * np.clip(bloom, 0, 1) * (1 - m_sym))[..., None]
    core = smoothstep(0.35, 0.95, t_sym / rb_map) * m_sym
    cdx, cdy = caustic_off[0] * T, caustic_off[1] * T
    caustic = blur(shift(core, cdy, cdx), T * 0.016) * (1 - m_sym)
    film_n = smooth_noise((S, S), 6, seed)
    caus_col = 0.75 + 0.25 * hue_rgb(0.55 + 0.5 * film_n)
    face = face + look.caustic * caustic[..., None] * caus_col

    # ---- the glass
    eta = 1 / 1.5
    cosi = np.clip(snz, 0, 1)
    k = np.clip(1 - eta * eta * (1 - cosi * cosi), 0, None)
    coef = eta * cosi - np.sqrt(k)
    tx_, ty_, tz_ = coef * snx, coef * sny, -eta + coef * snz
    depth = h_sym * refract + rb_map * 0.15
    offx = tx_ * depth / np.clip(-tz_, 0.25, None)
    offy = ty_ * depth / np.clip(-tz_, 0.25, None)
    behind = sample(face, yy + offy, xx + offx)
    gw = np.full((S, S), float(look.glass_white))
    tint = np.ones((S, S, 3)) * np.array(look.body_tint)
    for i, p in enumerate(pieces):
        sel = owner == i
        if p.white is not None:
            gw[sel] = p.white
        if p.tint is not None:
            tint[sel] = p.tint
    behind = behind * (1 - gw[..., None]) + gw[..., None]
    behind = behind * tint
    fres = 0.04 + 0.96 * tilt ** 2.6
    rx, ry, rz = 2 * snz * snx, 2 * snz * sny, 2 * snz * snz - 1
    env = environment(look, rx, ry, rz, behind)
    glass = behind * (1 - fres[..., None]) + env * fres[..., None]

    lam_s = np.clip(snx * kx + sny * ky + snz * kz, -1, 1) - kz
    glass = glass * (1 + look.body_light * lam_s)[..., None]

    noise = smooth_noise((S, S), 5, seed + 3)
    band = np.exp(-((tilt - look.rb_center) / look.rb_width) ** 2) * m_sym
    rb_col = hue_rgb(0.05 + 1.6 * tilt + 0.6 * noise + 0.2 * to_key)
    rb_amt = (rainbow * band * (0.45 + 0.55 * np.clip(-to_key * 0.7 + 0.5, 0, 1)))[..., None]
    glass = glass * (1 - rb_amt) + (glass * 0.40 + rb_col * 0.80) * rb_amt

    holo = hue_rgb(0.35 + 0.9 * noise + 0.5 * tilt)
    glass = glass * (1 - look.holo) + glass * (0.55 + 0.9 * holo) * look.holo

    far = np.clip(-to_key, 0, 1)
    dark = np.exp(-t_sym / (rb_map * (0.09 + 0.10 * far)))
    glass = glass * (1 - (0.45 + 0.30 * far)[..., None] * dark[..., None] * (1 - look.edge_tint))
    lit = np.exp(-((t_sym - rb_map * 0.22) / (rb_map * 0.07)) ** 2)
    glass = glass + (0.42 * lit * (0.35 + 0.65 * np.clip(to_key, 0, 1)))[..., None]

    k1 = np.exp(-((tilt - look.k1_tilt) / 0.05) ** 2) * np.clip(to_key, 0, 1) ** 2
    k1s = np.exp(-((tilt - 0.10) / 0.10) ** 2) * np.clip(to_key, 0, 1) ** 1.5
    k2 = np.exp(-((tilt - 0.26) / 0.08) ** 2) * np.clip(-to_key, 0, 1) ** 2
    glass = glass + look.spec * (0.95 * look.k1 * k1 + look.k1_broad * k1s)[..., None]
    glass = glass + look.spec * (0.38 * k2)[..., None] * look.rim_tint
    inner = np.exp(-((tilt - 0.07) / 0.06) ** 2) * np.clip(-to_key, 0, 1) ** 1.5
    glass = glass + (look.inner_glow * inner)[..., None] * look.rim_tint
    hx, hy, hz = normalize3(np.array(kx), np.array(ky), np.array(kz + 1.0))
    spec = np.clip(snx * float(hx) + sny * float(hy) + snz * float(hz), 0, 1)
    glass = glass + look.spec * (1.0 * spec ** 300)[..., None]
    if look.symbol_glow is not None:
        glass = glass + np.array(look.symbol_glow) * (core[..., None] * 0.22)
    for i, p in enumerate(pieces):
        if p.glow is not None:
            mine = (owner == i) * m_sym
            ci = smoothstep(0.0, 1.0, t_sym / rbs[i]) * mine
            glass = glass + p.glow * (p.glow_amt * ci ** 1.5)[..., None]

    rgb = face * (1 - m_sym[..., None]) + glass * m_sym[..., None]
    return dict(face=np.clip(face0, 0, 1), rgb=np.clip(rgb, 0, 1), mask=m_sym)


# ------------------------------------------------------------ layers

def split_foreground(B, C, m):
    """Foreground RGBA that gives C when laid over B. The symbol itself is opaque."""
    eps = 1e-6
    up = np.where(C > B, (C - B) / np.maximum(1 - B, eps), 0)
    dn = np.where(C < B, (B - C) / np.maximum(B, eps), 0)
    a = np.clip(np.max(np.maximum(up, dn), axis=-1), 0, 1)
    a = np.maximum(a, m)
    a = np.where(a < 0.75 / 255, 0, a)
    F = (C - (1 - a)[..., None] * B) / np.maximum(a, eps)[..., None]
    F = np.where(a[..., None] > 0, np.clip(F, 0, 1), 0)
    return np.dstack([F, a])


def resize_rgba(rgba, size):
    """Lanczos resize with premultiplied alpha, so edges never pick up dark fringes."""
    a = rgba[..., 3]
    pm = rgba[..., :3] * a[..., None]
    chans = []
    for ch in [pm[..., 0], pm[..., 1], pm[..., 2], a]:
        im = Image.fromarray(ch.astype(np.float32), "F").resize((size, size), Image.LANCZOS)
        chans.append(np.asarray(im, dtype=np.float64))
    out_a = np.clip(chans[3], 0, 1)
    rgb = np.stack(chans[:3], -1) / np.maximum(out_a, 1e-6)[..., None]
    rgb = np.where(out_a[..., None] > 1e-4, np.clip(rgb, 0, 1), 0)
    return np.dstack([rgb, out_a])


def resize_rgb(rgb, size):
    chans = []
    for i in range(3):
        im = Image.fromarray(rgb[..., i].astype(np.float32), "F").resize((size, size), Image.LANCZOS)
        chans.append(np.asarray(im, dtype=np.float64))
    return np.clip(np.stack(chans, -1), 0, 1)


def to_image(arr, dither=True, seed=1):
    arr = np.array(arr, dtype=np.float64)
    if dither:
        rng = np.random.default_rng(seed)
        n = arr.shape[-1]
        noise = (rng.random(arr.shape[:2] + (min(n, 3),)) - 0.5) / 255.0
        arr[..., :3] = arr[..., :3] + noise
    arr = np.clip(arr, 0, 1)
    mode = "RGBA" if arr.shape[-1] == 4 else "RGB"
    return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), mode)


# ------------------------------------------------------------ masks

def mask_circle(n, frac=VIS):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    c = (n - 1) / 2.0
    r = n * frac / 2.0
    d = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) - r
    return np.clip(0.5 - d, 0, 1)


def _bezier_path_mask(n, frac, path_pts, ss=4):
    from PIL import ImageDraw
    big = n * ss
    img = Image.new("L", (big, big), 0)
    off = big * (1 - frac) / 2.0
    scale = big * frac / 100.0
    pts = [(off + x * scale, off + y * scale) for x, y in path_pts]
    ImageDraw.Draw(img).polygon(pts, fill=255)
    return np.asarray(img.resize((n, n), Image.LANCZOS), dtype=np.float64) / 255.0


def _cubic(p0, p1, p2, p3, steps=64):
    out = []
    for i in range(steps):
        t = i / steps
        mt = 1 - t
        x = mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0]
        y = mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1]
        out.append((x, y))
    return out


def mask_squircle(n, frac=VIS):
    """AOSP's squircle icon mask: M50,0 C10,0 0,10 0,50 S10,100 50,100 S100,90 100,50 S90,0 50,0Z."""
    pts = []
    pts += _cubic((50, 0), (10, 0), (0, 10), (0, 50))
    pts += _cubic((0, 50), (0, 90), (10, 100), (50, 100))
    pts += _cubic((50, 100), (90, 100), (100, 90), (100, 50))
    pts += _cubic((100, 50), (100, 10), (90, 0), (50, 0))
    return _bezier_path_mask(n, frac, pts)


def mask_rounded(n, frac=VIS, radius=0.225, smooth=0.6):
    """iOS-style continuous-corner square (what Zenith's own icon shapes look like)."""
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    c = (n - 1) / 2.0
    half = n * frac / 2.0
    p = 2 + 3.0 * smooth
    x = np.abs(xx - c) / half
    y = np.abs(yy - c) / half
    r = radius * 2
    qx = np.clip(x - (1 - r), 0, None) / r
    qy = np.clip(y - (1 - r), 0, None) / r
    inside_box = np.maximum(x, y)
    corner = (qx ** p + qy ** p) ** (1 / p)
    d = np.where((qx > 0) & (qy > 0), (corner - 1) * r, inside_box - 1) * half
    return np.clip(0.5 - d, 0, 1)
