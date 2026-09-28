#!/usr/bin/env python3
"""Draws Starlight's icon (core/src/main/resources/assets/starlight/icon.png, also the Modrinth icon): a night-sky tile
with a gold four-point pixel star, a soft glow and a few small stars, drawn on a 32x32 grid and scaled 4x without
smoothing, so it matches the client's pixel look. Original art; run it to regenerate. Needs Pillow."""
import math, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
N, SCALE = 32, 4
TOP, BOTTOM = (8, 12, 34), (30, 42, 92)
GOLD, GOLD_DEEP, CORE, EDGE = (255, 200, 87), (214, 146, 44), (255, 250, 228), (255, 214, 110)


def inside(x, y, r=5):
    """Rounded tile: False in the four corner arcs."""
    cx = r - 0.5 if x < r else N - r - 0.5 if x >= N - r else x
    cy = r - 0.5 if y < r else N - r - 0.5 if y >= N - r else y
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def main():
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = img.load()
    for y in range(N):
        t = y / (N - 1)
        row = tuple(round(TOP[i] + (BOTTOM[i] - TOP[i]) * t) for i in range(3))
        for x in range(N):
            if inside(x, y):
                px[x, y] = row + (255,)
    # soft glow around the star's centre
    cx, cy = 15.5, 15.5
    for y in range(N):
        for x in range(N):
            if px[x, y][3] == 0:
                continue
            d = math.hypot(x - cx, y - cy)
            a = max(0.0, 1 - d / 11) ** 2 * 0.55
            r, g, b, _ = px[x, y]
            px[x, y] = (round(r + (GOLD[0] - r) * a), round(g + (GOLD[1] - g) * a), round(b + (GOLD[2] - b) * a), 255)
    # small background stars
    for (x, y, c) in [(5, 6, CORE), (26, 5, EDGE), (24, 25, CORE), (6, 24, EDGE), (27, 17, CORE), (9, 13, EDGE)]:
        px[x, y] = c + (255,)
    for (x, y) in [(25, 5), (27, 5), (26, 4), (26, 6)]:
        px[x, y] = GOLD_DEEP + (255,)
    # the four-point star: arms taper from the centre
    for i in range(-11, 12):
        w = 2 if abs(i) < 3 else 1 if abs(i) < 8 else 0
        for j in range(-w, w + 1):
            for (x, y) in [(15 + i if i < 0 else 16 + i, 15 + j), (15 + j, 15 + i if i < 0 else 16 + i),
                           (15 + i if i < 0 else 16 + i, 16 + j), (16 + j, 15 + i if i < 0 else 16 + i)]:
                if 0 <= x < N and 0 <= y < N and px[x, y][3]:
                    tip = abs(i) >= 8
                    px[x, y] = (EDGE if tip else GOLD if abs(j) == w and w else CORE) + (255,)
    for (x, y) in [(15, 15), (16, 15), (15, 16), (16, 16)]:
        px[x, y] = (255, 255, 255, 255)
    # a gold rim along the rounded edge
    for y in range(N):
        for x in range(N):
            if px[x, y][3] and any(not (0 <= x + dx < N and 0 <= y + dy < N) or not px[x + dx, y + dy][3]
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = GOLD_DEEP + (255,)
    out = os.path.join(ROOT, "core", "src", "main", "resources", "assets", "starlight", "icon.png")
    img.resize((N * SCALE, N * SCALE), Image.NEAREST).save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
