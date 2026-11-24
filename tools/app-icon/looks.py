"""Colour and light sets for the Zenith icon (the Plus icon's three plus Aurora)."""
import os

from glass import Look, blob_face, brand_face

HERE = os.path.dirname(os.path.abspath(__file__))


def pastel():
    # Zenith's own gradient, the background of today's icon
    return Look(
        face=brand_face(os.path.join(HERE, "brand_bg.png")),
        env_top=(0.80, 0.92, 1.00), env_bottom=(0.86, 0.72, 1.00), overhead=(0.93, 0.94, 1.00),
        shadow_tint=(0.50, 0.46, 0.85), rim_tint=(1.00, 0.78, 0.95),
        glass_white=0.22, shadow_strength=0.48, caustic=0.6,
        edge_tint=(0.30, 0.28, 0.70), k1=1.15, rb_center=0.36, rb_width=0.2,
    )


def midnight():
    return Look(
        face=blob_face([(0.06, 0.05, (78, 96, 190)), (0.5, 0.5, (30, 27, 80)), (0.96, 0.96, (122, 48, 168)),
                        (0.92, 0.1, (38, 40, 112)), (0.1, 0.92, (34, 42, 116))], base=(34, 30, 84), spread=0.72),
        env_top=(0.55, 0.62, 0.95), env_bottom=(0.62, 0.42, 0.85), overhead=(0.62, 0.62, 0.86),
        shadow_tint=(0.25, 0.2, 0.45), rim_tint=(1.00, 0.70, 0.95),
        glass_white=0.30, shadow_strength=0.55, caustic=0.35, tile_rim_light=0.5,
        edge_tint=(0.35, 0.30, 0.75), k1=1.2, rb_center=0.36, rb_width=0.2, holo=0.18,
        symbol_glow=(0.42, 0.36, 0.70), rim_glow=0.18,
    )


def aurora():
    # Zenith's Aurora theme: deep teal night, mint light at the lit edge, violet at the far one
    return Look(
        face=blob_face([(0.04, 0.04, (22, 92, 98)), (0.5, 0.5, (14, 34, 58)), (0.97, 0.97, (70, 40, 132)),
                        (0.94, 0.08, (18, 52, 86)), (0.08, 0.94, (16, 48, 74))], base=(14, 32, 54), spread=0.74),
        env_top=(0.45, 0.95, 0.80), env_bottom=(0.62, 0.50, 0.95), overhead=(0.60, 0.80, 0.86),
        shadow_tint=(0.10, 0.25, 0.30), rim_tint=(0.72, 0.58, 1.00),
        glass_white=0.24, shadow_strength=0.55, caustic=0.40,
        edge_tint=(0.20, 0.45, 0.50), k1=1.2, rb_center=0.36, rb_width=0.2, holo=0.16,
        symbol_glow=(0.25, 0.55, 0.50),
    )


def frost():
    return Look(
        face=blob_face([(0.08, 0.06, (250, 252, 255)), (0.5, 0.5, (226, 232, 248)), (0.95, 0.95, (232, 220, 250)),
                        (0.92, 0.1, (222, 236, 252)), (0.1, 0.92, (220, 228, 250))], base=(232, 236, 250), spread=0.75),
        env_top=(0.86, 0.94, 1.00), env_bottom=(0.92, 0.82, 1.00), overhead=(0.97, 0.97, 1.00),
        shadow_tint=(0.55, 0.52, 0.85), rim_tint=(1.00, 0.80, 0.96),
        glass_white=0.0, shadow_strength=0.40, caustic=0.5,
        edge_tint=(0.30, 0.30, 0.70), k1=1.0, rb_center=0.36, rb_width=0.2, holo=0.14,
        body_tint=(0.78, 0.76, 1.0),
    )


LOOKS = {"pastel": pastel, "midnight": midnight, "aurora": aurora, "frost": frost}
FACE_UV = {"pastel": "canvas"}
