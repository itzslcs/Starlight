package dev.starlight.core.perf;

/**
 * Entity culling: an entity is hidden only when every ray from the camera to its box (centre + 8 corners) hits a
 * solid opaque block first. It is conservative on purpose: anything near the camera, anything unchecked this frame, and
 * anything whose check ran out of budget is drawn. Results are cached per entity id for {@link #FRESH_MS}, and block
 * lookups are capped per frame ({@link #newFrame}, from the HUD pass; every 50 ms when the HUD is hidden), so the check
 * cannot cost frames. Cache lifetimes are staggered per entity so re-checks spread over frames instead of landing in one.
 * Render thread only; allocation-free.
 */
public final class Occlusion {
    /** A block grid: true when the block at (x, y, z) fully hides what is behind it. */
    public interface Blocks {
        boolean occludes(int x, int y, int z);
    }

    static final long FRESH_MS = 150, STALE_MS = 600;
    static final double NEAR = 4.0;
    static final int LOOKUP_BUDGET = 12000;
    private static final int CAP = 4096; // power of two
    private final int[] ids = new int[CAP];
    private final long[] at = new long[CAP];
    private final boolean[] visible = new boolean[CAP];
    private int lookups;
    private long lastReset, statsSec;
    private int callsNow, culledNow;
    /** Entity draw decisions and how many were skipped, over the last full second (Performance page). */
    public int calls, culled;

    /** Start of a frame: resets the lookup budget. */
    public void newFrame(long now) {
        lookups = 0;
        lastReset = now;
    }

    /** True when the entity should be drawn. Box and camera are world coordinates. */
    public boolean visible(Blocks w, int id, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                           double cx, double cy, double cz, long now) {
        double nx = Math.max(minX, Math.min(cx, maxX)) - cx, ny = Math.max(minY, Math.min(cy, maxY)) - cy,
                nz = Math.max(minZ, Math.min(cz, maxZ)) - cz;
        if (now - lastReset > 50) newFrame(now); // no frame signal (HUD hidden)
        if (now / 1000 != statsSec) {
            statsSec = now / 1000;
            calls = callsNow;
            culled = culledNow;
            callsNow = culledNow = 0;
        }
        callsNow++;
        if (nx * nx + ny * ny + nz * nz < NEAR * NEAR) return true;
        int slot = (id * 0x9E3779B1) >>> 20; // 12 bits
        boolean known = ids[slot] == id && at[slot] != 0;
        long age = known ? now - at[slot] : Long.MAX_VALUE;
        if (age < FRESH_MS + (id & 63)) return count(visible[slot]);
        if (lookups >= LOOKUP_BUDGET) return age < STALE_MS ? count(visible[slot]) : true;
        boolean v = test(w, minX, minY, minZ, maxX, maxY, maxZ, cx, cy, cz);
        ids[slot] = id;
        at[slot] = now;
        visible[slot] = v;
        return count(v);
    }

    private boolean count(boolean v) {
        if (!v) culledNow++;
        return v;
    }

    private boolean test(Blocks w, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                         double cx, double cy, double cz) {
        double mx = (minX + maxX) / 2, my = (minY + maxY) / 2, mz = (minZ + maxZ) / 2;
        if (clear(w, cx, cy, cz, mx, my, mz)) return true;
        for (int i = 0; i < 8; i++) {
            double px = (i & 1) == 0 ? minX : maxX, py = (i & 2) == 0 ? minY : maxY, pz = (i & 4) == 0 ? minZ : maxZ;
            if (clear(w, cx, cy, cz, px, py, pz)) return true;
            if (lookups >= LOOKUP_BUDGET) return true; // ran out mid-check: draw
        }
        return false;
    }

    /**
     * Voxel walk (Amanatides and Woo) from the camera cell to the target cell. True if no occluding block lies strictly
     * between them. The camera's own cell and the target cell never block.
     */
    boolean clear(Blocks w, double x0, double y0, double z0, double x1, double y1, double z1) {
        int x = floor(x0), y = floor(y0), z = floor(z0);
        int tx = floor(x1), ty = floor(y1), tz = floor(z1);
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        int sx = dx > 0 ? 1 : dx < 0 ? -1 : 0, sy = dy > 0 ? 1 : dy < 0 ? -1 : 0, sz = dz > 0 ? 1 : dz < 0 ? -1 : 0;
        double inf = Double.POSITIVE_INFINITY;
        double ddx = sx != 0 ? Math.abs(1 / dx) : inf, ddy = sy != 0 ? Math.abs(1 / dy) : inf, ddz = sz != 0 ? Math.abs(1 / dz) : inf;
        double mxT = sx > 0 ? (x + 1 - x0) * ddx : sx < 0 ? (x0 - x) * ddx : inf;
        double myT = sy > 0 ? (y + 1 - y0) * ddy : sy < 0 ? (y0 - y) * ddy : inf;
        double mzT = sz > 0 ? (z + 1 - z0) * ddz : sz < 0 ? (z0 - z) * ddz : inf;
        int max = Math.abs(tx - x) + Math.abs(ty - y) + Math.abs(tz - z);
        for (int step = 0; step < max; step++) {
            if (mxT < myT && mxT < mzT) {
                x += sx;
                mxT += ddx;
            } else if (myT < mzT) {
                y += sy;
                myT += ddy;
            } else {
                z += sz;
                mzT += ddz;
            }
            if (x == tx && y == ty && z == tz) return true;
            lookups++;
            if (w.occludes(x, y, z)) return false;
        }
        return true;
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
