# Kestrel: working notes for a fresh session

Multi-version Minecraft client mod: 18 jars (1.8.9 Forge; 1.21–1.21.11 and 26.1–26.3 Fabric). Read `docs/PLAN.md`
(phases), `docs/ARCHITECTURE.md` (layering and platform contract), `docs/DECISIONS.md` (why), and `docs/PROGRESS.md` (where we are).

## Hard rules (from the owner's spec)
- Original work only: no copied or decompiled code or assets from other clients or mods. Every dependency gets a license check in THIRD_PARTY.md.
- Legit only: no cheats or automation, no minimap-style features. Every module is mapped in docs/RULES_MATRIX.md, and GRAY ones ship default-off.
- No telemetry. Network only on opt-in (listed in README).
- Client code must never crash the game: guard everything (see ARCHITECTURE "Error isolation").
- Debugging protocol: reproduce → hypothesis in docs/debug-log.md → isolate → fix with cited evidence → regression test.
- Never mark anything "verified" or "works" without having run it. COMPAT_MATRIX cells need evidence.

## Layout
`api/` + `core/`: Java 8, no MC imports. `fabric/`: Stonecutter tree (versions/<mc>). `legacy/`: separate Gradle
build for Forge 1.8.9. `addons/`: plugin jars. `scripts/`: smoke/prod tests. `docs/`: all documentation.

## Commands
- `./gradlew buildAll`: every jar → `dist/` + `SHA256SUMS` (runs core tests; builds `legacy/` through its own wrapper).
- `./gradlew :core:test`: unit tests.
- `./gradlew :fabric:<mc>:build -Pkestrel.fabricTargets=<mc>`: one Fabric target (the property limits configuration to it).
- `cd legacy && ./gradlew build`: 1.8.9 (needs `./gradlew :api:jar :core:jar` in the root first).
- `scripts/smoke.sh <mc> [seconds]`: headless smoke (Xvfb + llvmpipe). Evidence goes to `smoke-out/<mc>/`.
- Signatures, never from memory: javap against Loom's mapped jars in `~/.gradle/caches/fabric-loom/minecraftMaven/...`
  (Mojang names) and `~/.gradle/caches/essential-loom/minecraftMaven/...` (MCP names for 1.8.9).

## Conventions
- Version-specific code only in `fabric/src` (Stonecutter `//? if` blocks) and `legacy/src`. Core has no MC imports.
- Mixins: one-line bodies that call `KestrelFabric`/`Kestrel` static hooks. `required:false`, `defaultRequire:1`. No `@Overwrite`.
- Every module: an entry in docs/RULES_MATRIX.md (RulesMatrixTest enforces it). GRAY ⇒ default off.
- No per-frame allocation in HUD paths (reused call objects, cached strings).

## Environment facts
- Prism (Flatpak) data: `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/`. Dawn (Flatpak) data: `~/.var/app/gg.dawn.Launcher/data/dawn/`.
- Never use the user's launcher accounts (accounts.json/keyrings) and never read those files.
- `grep` is aliased to ugrep, which rejects some complex regexes. Use python3 for heavy log parsing.
