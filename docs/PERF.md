# Performance

See also: [PLAN](PLAN.md) (Phase 5), [ARCHITECTURE](ARCHITECTURE.md) (performance budget), [debug-log](debug-log.md)
(benchmark pitfalls), [COMPAT_MATRIX](COMPAT_MATRIX.md).

## What MW19 does for FPS
| Feature | Where | What it changes |
|---|---|---|
| **Entity Culling** (module, on by default) | all 18 targets | Mobs, items and other entities fully hidden behind solid blocks are not drawn. Rays from the camera to each entity's box (centre and 8 corners) walk the block grid. The entity is skipped only if every ray hits a full opaque block. Players, glowing entities and anything showing a name tag are always drawn, because vanilla shows those through walls. Results are cached per entity, re-checks are spread over frames, and block lookups are capped per frame, so the check cannot cost frames. It fails open: drawing is the default. |
| **FPS Boost** (Performance page) | all 18 targets | One click switches the vanilla options that cost the most frames: clouds off, fewer particles, no entity shadows, no smooth lighting, biome blend 0, entity distance 75 %, VSync off, unlimited FPS, render distance capped at 12 (only lowered, never raised), and fast leaves (plus no improved transparency, vignette and weather radius 5 on 1.21.11+; fast graphics on older versions). *Undo* restores your values, even after a restart. |
| **Vulkan renderer** (Performance page) | 26.2, 26.3 | Sets Minecraft's own graphics API preference to Vulkan. It applies after a restart, and the game falls back to OpenGL if the GPU cannot run it. |
| Allocation-free HUD | all | No per-frame allocation in HUD paths; per-module cost is on the Performance page (budget 300 µs/frame). |

Sodium, ImmediatelyFast and similar mods are complementary. Their compatibility is recorded in the
[COMPAT_MATRIX](COMPAT_MATRIX.md) once it has been run.

## How it is measured
`scripts/bench.sh <mc>` (core [`Bench`](../core/src/main/java/dev/mw19/core/Bench.java)) builds one fixed scene in a fixed-seed world: a stone floor and wall at y=200,
120 villagers (no AI) behind the wall and 14 in front, with the camera facing the wall. It measures phases in one run:
an unreported warm-up, **baseline** (culling off, the options as they were), **culling**, **boost**, **culling+boost**,
and **baseline-end** (FPS Boost undone). baseline-end exposes drift within the run. Each phase gets a warm-up (25 s after
option changes, which rebuild every chunk section) and then 15 s of frames. It reports average FPS, 1 % low (the mean of
the worst 1 % of frames), p99 frame time, hitches over 50 ms and GC time.

Runs must have the machine to themselves: llvmpipe uses every core, and a Gradle build running alongside cut
one phase from 127 to 72 FPS (2026-09-26). The script does not guard against that; the operator has to.

**Caveat:** this machine renders on the CPU (Xvfb + Mesa llvmpipe), which makes GPU work very expensive. That
exaggerates savings from drawing less (culling) and says nothing about a real GPU. Compare phases within a run, never
across machines. Vanilla's inactivity limiter (30 FPS after 60 s without input) is switched off for the bench
([debug-log](debug-log.md)).

## Results
Pending: filled from the benchmark runs below.
