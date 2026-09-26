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
Minecraft 1.21.11, `scripts/bench.sh`, this machine (Xvfb + Mesa llvmpipe, CPU rendering), 2026-09-26, MW19 0.2.0 code
(Entity Culling and FPS Boost are unchanged in 0.3.0). Two clean runs; a third ran while a Gradle build competed for
the CPU and is left out (its boost phases fell to 54 and 72 FPS).

| Phase | Run 2: avg FPS | 1 % low | p99 | Run 3: avg FPS | 1 % low | p99 |
|---|---|---|---|---|---|---|
| baseline | 46.2 | 13.8 | 63.8 ms | 48.7 | 28.3 | 32.4 ms |
| culling | 73.9 | 31.0 | 29.6 ms | 78.1 | 44.1 | 18.8 ms |
| boost | 68.2 | 16.7 | 50.1 ms | 74.4 | 21.4 | 37.8 ms |
| culling + boost | **127.0** | 24.0 | 30.1 ms | **140.1** | 31.9 | 25.5 ms |
| baseline-end | 45.5 | 13.7 | 61.7 ms | 51.5 | 19.8 | 29.3 ms |

- **Entity Culling** skipped 91 % of entity draws in this scene (about 9 000 of 10 000 per second) and raised both the
  average (+60 %) and the 1 % lows.
- **FPS Boost** raised the average (+48 % and +53 %), but its 1 % low was worse than baseline in run 3 (21.4 vs 28.3).
  Not explained yet; the GC and hitch counters added since will show whether it is chunk rebuilds after the option change.
- **Both together** gave 2.7× and 2.9× the baseline average.
- baseline-end matches baseline within about 6 %, so the runs did not drift.

These numbers exaggerate what a real GPU gains from drawing less (see the caveat above); they compare phases within
one run, not clients or machines.
