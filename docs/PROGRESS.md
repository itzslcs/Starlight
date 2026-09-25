# Progress

## Phase 0: research & plan (done 2026-09-25)
- Verified the 18 targets against Mojang's manifest (versions.json), with Java 8/21/25 and mappings per version.
- Fabric: Loom 1.18.x with `fabric-loom-remap` (≤1.21.11) and `fabric-loom` (26.x); Loader 0.19.5; Gradle 9.8.0; Stonecutter 0.9.8 + loom-back-compat 0.4.2.
- Feather is now Dawn: Fabric for 1.17.1–26.3 (no 1.21.2/1.21.6/1.21.9), Forge 11.15.1.2318 + MixinTweaker for 1.8.9. Fabric API is injected by Dawn (docs/FEATHER.md).
- Hypixel mod policy mapped (docs/RULES_MATRIX.md). The Hypixel **API** policy forbids entering keys into public mods (DECISIONS D-010).
- Tier lists: MCTiers v2 + SubTiers v2 APIs documented and live. PvPTiers is down (D-015).

## Next: Phase 1
Walking skeleton on 1.21.11 + 1.8.9 (see PLAN.md).
