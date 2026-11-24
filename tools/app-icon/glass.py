"""Zenith Plus icon renderer: a liquid-glass tile with an iridescent glass symbol.

Everything is drawn with numpy at a supersampled size and scaled down, so the
edges stay clean. Shapes are signed distance fields (negative inside, pixels).
"""
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

# ---------------------------------------------------------------- helpers

def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def normalize3(x, y, z):
    length = np.sqrt(x * x + y * y + z * z) + 1e-12
    return x / length, y / length, z / length


def smin(a, b, k):
    """Polynomial smooth minimum: fuses two shapes with a fillet of size k."""
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0.0, 1.0)
    return b * (1 - h) + a * h - k * h * (1 - h)


def sdf_round_box(X, Y, hw, hh, r):
    qx = np.abs(X) - hw + r
    qy = np.abs(Y) - hh + r
    outside = np.sqrt(np.maximum(qx, 0) ** 2 + np.maximum(qy, 0) ** 2)
    inside = np.minimum(np.maximum(qx, qy), 0)
    return outside + inside - r


def sdf_capsule(X, Y, ax, ay, bx, by, r):
    pax, pay = X - ax, Y - ay
    bax, bay = bx - ax, by - ay
    h = np.clip((pax * bax + pay * bay) / (bax * bax + bay * bay), 0, 1)
    dx, dy = pax - bax * h, pay - bay * h
    return np.sqrt(dx * dx + dy * dy) - r


def sdf_from_mask(mask, smooth=1.2):
    """Signed distance (px) from a boolean mask, for shapes with no formula."""
    inside = ndi.distance_transform_edt(mask)
    outside = ndi.distance_transform_edt(~mask)
    d = outside - inside
    d = np.where(mask, d + 0.5, d - 0.5)
    if smooth:
        d = ndi.gaussian_filter(d, smooth)
    return d


def blur(a, sigma):
    if a.ndim == 3:
        return np.stack([ndi.gaussian_filter(a[..., c], sigma) for c in range(a.shape[2])], -1)
    return ndi.gaussian_filter(a, sigma)


def shift(a, dy, dx):
    return ndi.shift(a, (dy, dx), order=1, mode="constant", cval=0.0)


def sample(img, yy, xx):
    """Bilinear sample of an HxWx3 image at float coordinates."""
    out = np.empty(yy.shape + (img.shape[2],), dtype=np.float64)
    for c in range(img.shape[2]):
        out[..., c] = ndi.map_coordinates(img[..., c], [yy, xx], order=1, mode="nearest")
    return out


def coverage(d, aa=1.0):
    """Anti-aliased coverage from a signed distance in px."""
    return np.clip(0.5 - d / aa, 0.0, 1.0)


def height_rounded(d, radius, lift=1.0):
    """Height of a glass piece whose edge is a quarter circle of `radius` px."""
    t = np.clip(-d, 0, None)
    tt = np.minimum(t, radius) / radius
    return radius * lift * np.sqrt(np.clip(1 - (1 - tt) ** 2, 0, 1))


def normals_from_height(h):
    gy, gx = np.gradient(h)
    return normalize3(-gx, -gy, np.ones_like(h))


def thin_film(cos_i, thickness_nm, n_film=1.38):
    """Soap-bubble style interference colour (RGB around 1.0)."""
    sin_i = np.sqrt(np.clip(1 - cos_i ** 2, 0, 1))
    cos_t = np.sqrt(np.clip(1 - (sin_i / n_film) ** 2, 0, 1))
    out = []
    for lam in (630.0, 545.0, 455.0):
        delta = 4 * np.pi * n_film * thickness_nm * cos_t / lam
        out.append(0.5 - 0.5 * np.cos(delta))
    rgb = np.stack(out, -1)
    return rgb / (rgb.mean(-1, keepdims=True) + 0.25)


def smooth_noise(shape, cells, seed):
    rng = np.random.default_rng(seed)
    small = rng.random((cells, cells))
    big = ndi.zoom(small, (shape[0] / cells, shape[1] / cells), order=3)
    big = big[: shape[0], : shape[1]]
    big = (big - big.min()) / (big.max() - big.min() + 1e-9)
    return big


# ------------------------------------------------------------ the scene

class Look:
    """Colours and light for one icon."""

    def __init__(self, face, env_top, env_bottom, overhead, shadow_tint, rim_tint,
                 glass_white=0.14, shadow_strength=0.30, caustic=0.30, irid=0.75,
                 spec=1.0, edge_tint=(0.36, 0.34, 0.72), symbol_glow=None,
                 tile_rim_light=0.55, film_base=340.0, film_span=260.0,
                 body_light=0.12, rb_center=0.38, rb_width=0.16, inner_glow=0.2,
                 round_frac=1.0, k1_tilt=0.2, k1_broad=0.0, holo=0.12, aura=None, k1=1.0,
                 rim_shade=0.08, rim_glow=0.32, groove=0.05,
                 body_tint=(1.0, 1.0, 1.0)):
        self.face = face                  # function(U, V) -> HxWx3, U,V in 0..1 over the tile
        self.env_top = np.array(env_top)
        self.env_bottom = np.array(env_bottom)
        self.overhead = np.array(overhead)
        self.shadow_tint = np.array(shadow_tint)
        self.rim_tint = np.array(rim_tint)
        self.glass_white = glass_white
        self.shadow_strength = shadow_strength
        self.caustic = caustic
        self.irid = irid
        self.spec = spec
        self.edge_tint = np.array(edge_tint)
        self.symbol_glow = symbol_glow
        self.tile_rim_light = tile_rim_light
        self.film_base = film_base
        self.film_span = film_span
        self.body_light = body_light
        self.rb_center = rb_center
        self.rb_width = rb_width
        self.inner_glow = inner_glow
        self.round_frac = round_frac
        self.k1_tilt = k1_tilt
        self.k1_broad = k1_broad
        self.holo = holo
        self.aura = aura
        self.k1 = k1
        self.rim_shade = rim_shade
        self.rim_glow = rim_glow
        self.groove = groove
        self.body_tint = body_tint


KEY = normalize3(np.array(-0.52), np.array(-0.66), np.array(0.54))   # upper left
FILL = normalize3(np.array(0.62), np.array(0.58), np.array(0.53))    # lower right, weak


def environment(look, rx, ry, rz, floor_rgb):
    """What a reflection sees: a soft pastel studio with a big light up-left."""
    up = smoothstep(0.6, -0.6, ry)[..., None]          # -y is up on screen
    walls = look.env_bottom * (1 - up) + look.env_top * up
    above = smoothstep(0.15, 0.95, rz)[..., None]
    env = walls * (1 - above) + look.overhead * above
    below = smoothstep(0.0, -0.5, rz)[..., None]
    env = env * (1 - below) + floor_rgb * 0.85 * below
    kx, ky, kz = [float(v) for v in KEY]
    box = smoothstep(0.86, 0.97, rx * kx + ry * ky + rz * kz)[..., None]
    env = env + box * 1.5
    fx, fy, fz = [float(v) for v in FILL]
    strip = smoothstep(0.90, 0.985, rx * fx + ry * fy + rz * fz)[..., None]
    env = env + strip * look.rim_tint * 0.55
    return env


def hue_rgb(h):
    """Smooth rainbow: hue 0..1 -> RGB 0..1 (pastel friendly)."""
    h = np.mod(h, 1.0)[..., None]
    k = np.array([0.0, 2.0 / 3.0, 1.0 / 3.0])
    return 0.5 + 0.5 * np.cos(2 * np.pi * (h + k))


def render_tile(S, look, symbol_sdf, tile_frac=0.94, corner=0.225, bevel=0.07,
                sym_bevel=None, symbol_lift=0.9, shadow_off=(0.022, 0.032),
                caustic_off=(0.040, 0.055), refract=0.9, rainbow=0.42,
                seed=7, extra=None):
    """Draw the tile plus a glass symbol. Returns float RGBA (S x S x 4), 0..1.

    symbol_sdf(X, Y, T) gives the symbol's signed distance in px, where X, Y are
    pixel offsets from the tile centre and T is the tile size in px.
    """
    T = S * tile_frac
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
    X, Y = xx - c, yy - c
    kx, ky, kz = [float(v) for v in KEY]
    kdir = np.array([kx, ky]) / np.hypot(kx, ky)

    # ---- tile slab
    d_tile = sdf_round_box(X, Y, T / 2, T / 2, T * corner)
    a_tile = coverage(d_tile)
    w = T * bevel
    h_tile = height_rounded(d_tile, w, 1.0)
    tnx, tny, tnz = normals_from_height(h_tile)

    U = (X / T + 0.5)
    V = (Y / T + 0.5)
    face = look.face(np.clip(U, 0, 1), np.clip(V, 0, 1))

    # ---- symbol geometry
    d_sym = symbol_sdf(X, Y, T)
    m_sym = coverage(d_sym)
    t_sym = np.clip(-d_sym, 0, None)
    half = max(2.0, float(np.max(t_sym)))
    rb = half * look.round_frac if sym_bevel is None else sym_bevel * T
    # blur the distance field before shaping: near the edge it is a straight
    # ramp and stays put, but the crease down the middle of a stroke melts
    d_shape = ndi.gaussian_filter(d_sym, half * 0.30)
    t_sym = np.clip(-d_shape, 0, None)
    half = max(2.0, float(np.max(t_sym)))
    rb = half * look.round_frac if sym_bevel is None else sym_bevel * T
    h_sym = height_rounded(d_shape, rb, symbol_lift)
    h_sym = ndi.gaussian_filter(h_sym, S / 1400)
    snx, sny, snz = normals_from_height(h_sym)
    tilt = 1 - np.clip(snz, 0, 1)
    nxy = np.sqrt(snx ** 2 + sny ** 2) + 1e-9
    to_key = (snx * kdir[0] + sny * kdir[1]) / np.maximum(nxy, 0.12)   # 1 = faces the light

    # ---- what the symbol does to the face: halo, shadow, caustic
    sdx, sdy = shadow_off[0] * T, shadow_off[1] * T
    shadow = blur(shift(m_sym, sdy, sdx), T * 0.030)
    halo = blur(m_sym, T * 0.014)
    sh = np.clip(shadow * 0.80 + halo * 0.45, 0, 1) * (1 - m_sym)
    sh = np.maximum(sh, blur(m_sym, T * 0.03) * 0.25)
    face = face * (1 - look.shadow_strength * sh[..., None] * (1 - look.shadow_tint))
    if look.aura is not None:
        aura = blur(m_sym, T * 0.06) * 0.8 + blur(m_sym, T * 0.018) * 0.5
        face = face + np.array(look.aura) * np.clip(aura, 0, 1)[..., None] * (1 - m_sym[..., None])
    core = smoothstep(0.35, 0.95, t_sym / half) * m_sym
    cdx, cdy = caustic_off[0] * T, caustic_off[1] * T
    caustic = blur(shift(core, cdy, cdx), T * 0.016) * (1 - m_sym)
    film_n = smooth_noise((S, S), 6, seed)
    caus_col = 0.75 + 0.25 * hue_rgb(0.55 + 0.5 * film_n)
    face = face + look.caustic * caustic[..., None] * caus_col

    # ---- tile rim: a clear glass edge that catches light all the way round
    t_in = np.clip(-d_tile, 0, None)
    s_t = np.clip(t_in / w, 0, 1)
    tilt_t = 1 - np.clip(tnz, 0, 1)
    nxy_t = np.sqrt(tnx ** 2 + tny ** 2)
    tk = (tnx * kdir[0] + tny * kdir[1]) / np.maximum(nxy_t, 0.12)
    lam = (tnx * kx + tny * ky + tnz * kz) / kz
    tile = face * (1 + look.rim_shade * (lam - 1))[..., None]
    key_side = np.clip(tk, 0, 1)[..., None]
    far_side = np.clip(-tk, 0, 1)[..., None]
    rim_col = key_side * np.array([1.0, 1.0, 1.0]) + far_side * look.rim_tint \
        + (1 - key_side - far_side) * np.array([0.88, 0.94, 1.0])
    rim_amt = (look.rim_glow * (1 - s_t) ** 0.8 * (s_t < 1))[..., None]
    tile = tile * (1 - rim_amt) + rim_col * rim_amt
    fres_t = (1 - tnz) ** 2.0
    rxt, ryt, rzt = 2 * tnz * tnx, 2 * tnz * tny, 2 * tnz * tnz - 1
    env_t = environment(look, rxt, ryt, rzt, face)
    tile = tile * (1 - 0.35 * fres_t[..., None]) + env_t * (0.35 * fres_t[..., None])
    streak_t = np.exp(-((tilt_t - 0.30) / 0.10) ** 2) * np.clip(tk, 0, 1) ** 2.5
    tile = tile + (0.50 * streak_t)[..., None]
    streak_t2 = np.exp(-((tilt_t - 0.42) / 0.12) ** 2) * np.clip(-tk, 0, 1) ** 2.0
    tile = tile + (0.30 * streak_t2)[..., None] * look.rim_tint
    edge = np.exp(-t_in / (S * 0.0018))
    edge_w = 0.35 + 0.65 * np.abs(tk)
    tile = tile + (edge * edge_w * look.tile_rim_light)[..., None]
    # where the rim meets the face: a faint groove, then a hairline of light
    groove = np.exp(-((t_in - w * 0.98) / (w * 0.07)) ** 2)
    tile = tile * (1 - look.groove * groove[..., None] * (1 - look.shadow_tint * 0.6))
    ridge = np.exp(-((t_in - w * 1.12) / (w * 0.07)) ** 2)
    tile = tile + (0.06 * ridge * (0.3 + 0.7 * np.clip(tk, 0, 1)))[..., None]

    # ---- the glass symbol
    # refraction: bend the straight-down view ray at the top surface (Snell,
    # glass 1.5) and follow it to the tile under the glass
    eta = 1 / 1.5
    cosi = np.clip(snz, 0, 1)
    k = np.clip(1 - eta * eta * (1 - cosi * cosi), 0, None)
    coef = eta * cosi - np.sqrt(k)
    tx_, ty_, tz_ = coef * snx, coef * sny, -eta + coef * snz
    depth = h_sym * refract + rb * 0.15
    offx = tx_ * depth / np.clip(-tz_, 0.25, None)
    offy = ty_ * depth / np.clip(-tz_, 0.25, None)
    behind = sample(tile, yy + offy, xx + offx)
    behind = behind * (1 - look.glass_white) + look.glass_white
    behind = behind * np.array(look.body_tint)
    fres = 0.04 + 0.96 * tilt ** 2.6
    rx, ry, rz = 2 * snz * snx, 2 * snz * sny, 2 * snz * snz - 1
    env = environment(look, rx, ry, rz, behind)
    glass = behind * (1 - fres[..., None]) + env * fres[..., None]

    # body light: lit side a touch brighter, far side a touch deeper
    lam_s = np.clip(snx * kx + sny * ky + snz * kz, -1, 1) - kz
    glass = glass * (1 + look.body_light * lam_s)[..., None]

    # rainbow dispersion on the rounded sides
    noise = smooth_noise((S, S), 5, seed + 3)
    band = np.exp(-((tilt - look.rb_center) / look.rb_width) ** 2) * m_sym
    rb_col = hue_rgb(0.05 + 1.6 * tilt + 0.6 * noise + 0.2 * to_key)
    rb_amt = (rainbow * band * (0.45 + 0.55 * np.clip(-to_key * 0.7 + 0.5, 0, 1)))[..., None]
    glass = glass * (1 - rb_amt) + (glass * 0.40 + rb_col * 0.80) * rb_amt

    # a faint holographic sheen over the whole body
    holo = hue_rgb(0.35 + 0.9 * noise + 0.5 * tilt)
    glass = glass * (1 - look.holo) + glass * (0.55 + 0.9 * holo) * look.holo

    # dark hairline at the very edge (deeper on the far side), bright line inside it
    far = np.clip(-to_key, 0, 1)
    dark = np.exp(-t_sym / (rb * (0.09 + 0.10 * far)))
    glass = glass * (1 - (0.45 + 0.30 * far)[..., None] * dark[..., None] * (1 - look.edge_tint))
    lit = np.exp(-((t_sym - rb * 0.22) / (rb * 0.07)) ** 2)
    glass = glass + (0.42 * lit * (0.35 + 0.65 * np.clip(to_key, 0, 1)))[..., None]

    # light streaks from a big ring light: key up-left, pink fill opposite
    k1 = np.exp(-((tilt - look.k1_tilt) / 0.05) ** 2) * np.clip(to_key, 0, 1) ** 2
    k1s = np.exp(-((tilt - 0.10) / 0.10) ** 2) * np.clip(to_key, 0, 1) ** 1.5
    k2 = np.exp(-((tilt - 0.26) / 0.08) ** 2) * np.clip(-to_key, 0, 1) ** 2
    glass = glass + look.spec * (0.95 * look.k1 * k1 + look.k1_broad * k1s)[..., None]
    glass = glass + look.spec * (0.38 * k2)[..., None] * look.rim_tint
    # light that went through and gathered on the far inside wall
    inner = np.exp(-((tilt - 0.07) / 0.06) ** 2) * np.clip(-to_key, 0, 1) ** 1.5
    glass = glass + (look.inner_glow * inner)[..., None] * look.rim_tint
    hx, hy, hz = normalize3(np.array(kx), np.array(ky), np.array(kz + 1.0))
    spec = np.clip(snx * float(hx) + sny * float(hy) + snz * float(hz), 0, 1)
    glass = glass + look.spec * (1.0 * spec ** 300)[..., None]
    if look.symbol_glow is not None:
        glass = glass + np.array(look.symbol_glow) * (core[..., None] * 0.22)

    rgb = tile * (1 - m_sym[..., None]) + glass * m_sym[..., None]
    if extra is not None:
        rgb = extra(rgb, X, Y, T)
    rgb = np.clip(rgb, 0, 1)
    return np.dstack([rgb, a_tile])


def save_sizes(rgba, stem, sizes=(1024, 512)):
    S = rgba.shape[0]
    # dither a hair so gradients do not band
    rng = np.random.default_rng(1)
    rgb = rgba[..., :3] + (rng.random(rgba[..., :3].shape) - 0.5) / 255.0
    arr = np.dstack([np.clip(rgb, 0, 1), rgba[..., 3]])
    big = Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA")
    out = []
    for s in sizes:
        im = big.resize((s, s), Image.LANCZOS) if s != S else big
        path = f"{stem}-{s}.png"
        im.save(path, optimize=True)
        out.append(path)
    return out


# ------------------------------------------------------------ faces

def brand_face(path):
    """The gradient behind Zenith's own logo, stretched over the tile."""
    src = np.asarray(Image.open(path).convert("RGB")).astype(np.float64) / 255.0
    src = blur(src, 6)

    def face(U, V):
        H, W = src.shape[:2]
        return sample(src, V * (H - 1), U * (W - 1))
    return face


def blob_face(points, base, power=2.2, spread=0.55):
    """A smooth gradient from colour points [(u, v, (r, g, b)), ...] in 0..1."""
    pts = [(u, v, np.array(c) / 255.0) for u, v, c in points]
    base = np.array(base) / 255.0

    def face(U, V):
        acc = np.zeros(U.shape + (3,))
        wsum = np.zeros(U.shape)
        for u, v, col in pts:
            d2 = (U - u) ** 2 + (V - v) ** 2
            w = np.exp(-d2 / (2 * (spread * 0.5) ** 2))
            acc += w[..., None] * col
            wsum += w
        wb = 0.08
        return (acc + wb * base) / (wsum + wb)[..., None]
    return face
