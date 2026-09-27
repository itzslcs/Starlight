#!/usr/bin/env python3
"""Writes MW19's Fast Chests resource pack (fabric/src/main/resources/mw19packs/fast_chests): block models that draw
chests as ordinary blocks, so the chunk mesh carries them instead of a block entity renderer every frame.

The geometry and UVs are read off the vanilla chest textures (entity/chest/*.png, 64x64), which use the standard box
unwrap: for a box w x h x d at texture offset (u, v) the caps sit at (u+d, v) and (u+d+w, v) and the four sides run
along the row below. Checked against the textures (see docs/debug-log.md, 2026-09-26 "fast chests"):
  - the first cap is the box's underside, the second its top (the base's second cap is the dark chest floor);
  - the sides run east, south, west, north for a chest facing north (a half chest's seam side is left blank, and
    the latch notch is on the fourth side, the front);
  - every region is stored rotated 180 degrees relative to a block model face, so each face uses "rotation": 180.
Faces that can never be seen are left out: the base's top and the lid's underside (they touch), the back of the
latch, and the seam faces between the halves of a double chest. The base stops at y=9 where the lid starts (vanilla
overlaps them by one pixel, which would z-fight in a chunk mesh); its hidden top texture row is skipped to match.

Run after changing it: scripts/fast-chests.py (the output is committed).
"""
import json, os, shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "fabric", "src", "main", "resources",
                    "mw19packs", "fast_chests")
PX = 16 / 64  # 64 px textures in a 16-unit UV space


def regions(u, v, w, h, d):
    """Face -> (u, v, width, height) in texture pixels, for a chest facing north."""
    return {
        "down": (u + d, v, w, d),
        "up": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "south": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "north": (u + 2 * d + w, v + d, w, h),
    }


def box(frm, to, tex, size, faces, rows=None, cull=None, trim=None):
    """One element. size = (w, h, d); rows = texture rows the sides use (default: all h); trim = {face: +1 | -1} drops
    the region's first (+1) or last (-1) texture column."""
    w, h, d = size
    r = regions(tex[0], tex[1], w, h, d)
    out = {}
    for f in faces:
        ru, rv, rw, rh = r[f]
        if rows is not None and f not in ("up", "down"):
            rh = rows  # rows from the region's first (the bottom of the box, once rotated)
        if trim and f in trim:
            if trim[f] > 0:
                ru += 1
            rw -= 1
        face = {"uv": [ru * PX, rv * PX, (ru + rw) * PX, (rv + rh) * PX], "rotation": 180, "texture": "#chest"}
        if cull and f in cull:
            face["cullface"] = f
        out[f] = face
    return {"from": frm, "to": to, "faces": out}


def chest(kind):
    """kind: single, left (seam on the east, x 1..16) or right (seam on the west, x 0..15)."""
    if kind == "single":
        x0, x1, w, latch = 1, 15, 14, ([7, 7, 0], [9, 11, 1], (2, 4, 1))
        sides = ["north", "south", "east", "west"]
    elif kind == "left":
        x0, x1, w, latch = 1, 16, 15, ([15, 7, 0], [16, 11, 1], (1, 4, 1))
        sides = ["north", "south", "west"]
    else:
        x0, x1, w, latch = 0, 15, 15, ([0, 7, 0], [1, 11, 1], (1, 4, 1))
        sides = ["north", "south", "east"]
    latch_sides = [f for f in sides if f != "south"]  # its back touches the chest
    # Block textures are mipmapped (entity ones are not): a cap region starting on an odd column shares its first
    # mip texel with the region beside it (the dark lid underside or chest floor). On a seam that shows as a dark
    # line between the halves, so the seam-side column is dropped there: the left half's lid top, the right half's
    # bottom (the outer edges already have a dark rim).
    trim_lid = {"up": 1} if kind == "left" else None
    trim_base = {"down": -1} if kind == "right" else None
    return [
        box([x0, 0, 1], [x1, 9, 15], (0, 19), (w, 10, 14), ["down"] + sides, rows=9, cull=["down"], trim=trim_base),
        box([x0, 9, 1], [x1, 14, 15], (0, 0), (w, 5, 14), ["up"] + sides, trim=trim_lid),
        box(latch[0], latch[1], (0, 0), latch[2], ["down", "up"] + latch_sides),
    ]


# texture base name -> (particle texture, blocks using it)
CHESTS = {
    "normal": ("minecraft:block/oak_planks", ["chest"]),
    "trapped": ("minecraft:block/oak_planks", ["trapped_chest"]),
    "copper": ("minecraft:block/copper_block", ["copper_chest", "waxed_copper_chest"]),
    "copper_exposed": ("minecraft:block/exposed_copper", ["exposed_copper_chest", "waxed_exposed_copper_chest"]),
    "copper_weathered": ("minecraft:block/weathered_copper", ["weathered_copper_chest", "waxed_weathered_copper_chest"]),
    "copper_oxidized": ("minecraft:block/oxidized_copper", ["oxidized_copper_chest", "waxed_oxidized_copper_chest"]),
}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def write(rel, data):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(json.dumps(data, separators=(",", ":")) + "\n")


def main():
    shutil.rmtree(ROOT, ignore_errors=True)
    # Chest textures join the block atlas (atlas definitions from every pack are merged).
    write("assets/minecraft/atlases/blocks.json",
          {"sources": [{"type": "directory", "source": "entity/chest", "prefix": "entity/chest/"}]})
    for kind in ("single", "left", "right"):
        write(f"assets/mw19/models/block/fast_chest/{kind}.json", {"elements": chest(kind)})

    def model(name, texture, particle, kind):
        write(f"assets/mw19/models/block/fast_chest/{name}.json", {
            "parent": f"mw19:block/fast_chest/{kind}",
            "textures": {"chest": f"minecraft:entity/chest/{texture}", "particle": particle}})

    for tex, (particle, blocks) in CHESTS.items():
        model(f"{tex}_single", tex, particle, "single")
        model(f"{tex}_left", f"{tex}_left", particle, "left")
        model(f"{tex}_right", f"{tex}_right", particle, "right")
        variants = {}
        for facing, y in FACING_Y.items():
            for kind in ("single", "left", "right"):
                v = {"model": f"mw19:block/fast_chest/{tex}_{kind}"}
                if y:
                    v["y"] = y
                variants[f"facing={facing},type={kind}"] = v
        for b in blocks:
            write(f"assets/minecraft/blockstates/{b}.json", {"variants": variants})
    model("ender", "ender", "minecraft:block/obsidian", "single")
    variants = {}
    for facing, y in FACING_Y.items():
        v = {"model": "mw19:block/fast_chest/ender"}
        if y:
            v["y"] = y
        variants[f"facing={facing}"] = v
    write("assets/minecraft/blockstates/ender_chest.json", {"variants": variants})
    print("wrote", ROOT)


if __name__ == "__main__":
    main()
