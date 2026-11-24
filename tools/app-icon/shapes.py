"""Symbol shapes for the Zenith icon, in T (the 72 dp visible tile), y down."""
import numpy as np

from glass import sdf_capsule, sdf_round_box, smin


def polyline(points, r, k=0.02):
    """Rounded glass rods along a polyline, fused where they meet."""
    def f(X, Y, T):
        d = None
        for (ax, ay), (bx, by) in zip(points[:-1], points[1:]):
            di = sdf_capsule(X, Y, ax * T, ay * T, bx * T, by * T, r * T)
            d = di if d is None else smin(d, di, k * T)
        return d
    return f


def ring(cx, cy, R, r):
    def f(X, Y, T):
        return np.abs(np.hypot(X - cx * T, Y - cy * T) - R * T) - r * T
    return f


def arc(cx, cy, R, r, a0, a1):
    """A rod bent along a circle from angle a0 to a1 (radians, 0 = right, y down)."""
    def f(X, Y, T):
        px, py = X - cx * T, Y - cy * T
        ang = np.arctan2(py, px)
        mid = 0.5 * (a0 + a1)
        half = 0.5 * abs(a1 - a0)
        rel = np.angle(np.exp(1j * (ang - mid)))
        on = np.abs(rel) <= half
        d_ring = np.abs(np.hypot(px, py) - R * T) - r * T
        e0 = (cx + R * np.cos(a0), cy + R * np.sin(a0))
        e1 = (cx + R * np.cos(a1), cy + R * np.sin(a1))
        d0 = np.hypot(X - e0[0] * T, Y - e0[1] * T) - r * T
        d1 = np.hypot(X - e1[0] * T, Y - e1[1] * T) - r * T
        return np.where(on, d_ring, np.minimum(d0, d1))
    return f


def circle(cx, cy, R):
    def f(X, Y, T):
        return np.hypot(X - cx * T, Y - cy * T) - R * T
    return f


def round_box(cx, cy, hw, hh, rad):
    def f(X, Y, T):
        return sdf_round_box(X - cx * T, Y - cy * T, hw * T, hh * T, rad * T)
    return f


def triangle(p0, p1, p2, rad):
    """A triangle with corners rounded by rad (the corners given are the outer extent)."""
    P = [np.array(p, dtype=np.float64) for p in (p0, p1, p2)]
    cen = sum(P) / 3.0
    # pull the corners in so the rounded shape keeps the given extent
    inr = []
    for i in range(3):
        a, b, c = P[i], P[(i + 1) % 3], P[(i + 2) % 3]
        u = (b - a) / np.linalg.norm(b - a)
        v = (c - a) / np.linalg.norm(c - a)
        half_angle = np.arccos(np.clip(np.dot(u, v), -1, 1)) / 2
        bis = (u + v) / np.linalg.norm(u + v)
        inr.append(a + bis * (rad / np.sin(half_angle) - rad))
    q0, q1, q2 = inr

    def f(X, Y, T):
        px, py = X / T, Y / T
        e0, e1, e2 = q1 - q0, q2 - q1, q0 - q2
        v0x, v0y = px - q0[0], py - q0[1]
        v1x, v1y = px - q1[0], py - q1[1]
        v2x, v2y = px - q2[0], py - q2[1]

        def pq(vx, vy, e):
            h = np.clip((vx * e[0] + vy * e[1]) / (e @ e), 0, 1)
            return vx - e[0] * h, vy - e[1] * h
        p0x, p0y = pq(v0x, v0y, e0)
        p1x, p1y = pq(v1x, v1y, e1)
        p2x, p2y = pq(v2x, v2y, e2)
        s = np.sign(e0[0] * e2[1] - e0[1] * e2[0])
        dd = np.minimum(np.minimum(p0x ** 2 + p0y ** 2, p1x ** 2 + p1y ** 2), p2x ** 2 + p2y ** 2)
        c0 = s * (v0x * e0[1] - v0y * e0[0])
        c1 = s * (v1x * e1[1] - v1y * e1[0])
        c2 = s * (v2x * e2[1] - v2y * e2[0])
        sg = np.minimum(np.minimum(c0, c1), c2)
        return (-np.sqrt(dd) * np.sign(sg) - rad) * T
    return f


def ellipse_ring(cx, cy, A, B, rot_deg, r):
    """A rod bent along a tilted ellipse (first-order distance, good near the curve)."""
    th = np.radians(rot_deg)
    c, s = np.cos(th), np.sin(th)

    def f(X, Y, T):
        px, py = X / T - cx, Y / T - cy
        xr = c * px + s * py
        yr = -s * px + c * py
        g = np.sqrt((xr / A) ** 2 + (yr / B) ** 2) + 1e-9
        gx, gy = xr / (A * A * g), yr / (B * B * g)
        d = (g - 1) / (np.sqrt(gx * gx + gy * gy) + 1e-9)
        return (np.abs(d) - r) * T
    return f


def ellipse_front(cx, cy, rot_deg):
    """How far a point is on the near side of a tilted ring (local y, in T)."""
    th = np.radians(rot_deg)
    c, s = np.cos(th), np.sin(th)

    def f(X, Y, T):
        px, py = X / T - cx, Y / T - cy
        return -s * px + c * py
    return f
