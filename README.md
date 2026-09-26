# MW19

A client-side mod suite for Minecraft with a HUD, a GUI with its own module menu and HUD editor, profiles, and a plugin API.
It targets **1.8.9 (Forge)** and **every release from 1.21 to 26.3 (Fabric)**, 18 jars in total, all built from one source tree.
It is legit only: no cheats and no automation. Every module is checked against server rules
([RULES_MATRIX](docs/RULES_MATRIX.md)).

> Status: in development (0.1.0). What has actually been run and passed is in [COMPAT_MATRIX](docs/COMPAT_MATRIX.md),
> and where the project stands is in [PROGRESS](docs/PROGRESS.md).

## Install
| Minecraft | Loader | Jar |
|---|---|---|
| 1.8.9 | Forge 11.15.1.2318 | `MW19-<version>+mc1.8.9.jar` |
| 1.21 – 1.21.11 | Fabric Loader ≥ 0.16 | `MW19-<version>+mc<mc>.jar` |
| 26.1 – 26.3 | Fabric Loader ≥ 0.18 (Java 25) | `MW19-<version>+mc<mc>.jar` |

Put the jar for your version in the instance's `mods` folder. **Fabric API is not needed.**

**Prism Launcher:** import a ready-made instance (`dist/prism/MW19-<mc>.zip`) or add the jar to your own instance.
Both are covered step by step in [PRISM](docs/PRISM.md). Prism has not yet been checked with a real launch; see the
Prism column of [COMPAT_MATRIX](docs/COMPAT_MATRIX.md).

## Using it
- **Right Shift** opens the menu: Mods, HUD Editor, Profiles, Keybinds, Plugins, Server Rules, Performance, Themes, About.
  The title and pause screens also have a MW19 button.
- **HUD Editor:** drag elements (they snap), drag a corner to scale, and right-click an element for its text colour and shadow.
- **Profiles:** one layout and module set per profile. They switch automatically per server and can be shared as a
  `MW19-P1:` code.

### Modules
| HUD | Visual | Utility | Chat |
|---|---|---|---|
| FPS, **KeyCPS** (keystrokes + CPS), Ping, Coordinates, Direction, Armor Status, Potion Effects, Item Counter, Clock, Memory/CPU, Combo | Custom Crosshair, Brightness, Hit Color, Damage Tilt, Low Fire, Shield Overlay, Particle Multiplier, Show Own Nametag, 1.8 Combat Visuals | Zoom, Toggle Sprint/Sneak, Freelook, Screenshot Tools | Chat Tools (timestamps, highlights, stacking, search) |

KeyCPS is the author's own [KeyCPS](https://modrinth.com/mod/keycps) mod, built in ([D-018](docs/DECISIONS.md)).
Modules that some servers restrict (**GRAY**) are off by default, and a server can switch modules off
([`serverrules.json`](core/src/main/resources/mw19/serverrules.json), Server Rules page). The Hypixel suite for 1.8.9 is planned ([PLAN](docs/PLAN.md), Phase 6).

### Plugins
Plugin jars go in `MW19/plugins/`. Each new jar is off until you approve it. Plugins run with the same rights as
the game, so approve only plugins you trust. Two plugins ship with MW19: **Tier Tags** (PvP tier-list ranks next to
names) and **Session Stats**, which is the example for [PLUGIN_API](docs/PLUGIN_API.md).

## Privacy
MW19 has no telemetry, accounts or analytics, and nothing reads your session token (the plugin API has no access to
it). The only network requests happen when you turn them on:

| What | When | Where to |
|---|---|---|
| Tier Tags lookups | Tier Tags plugin approved **and** its module enabled (off by default) | `mctiers.com/api/v2`, `subtiers.net/api/v2`, or a URL you enter |

A plugin you approve can use the network through the plugin API. See [THIRD_PARTY](docs/THIRD_PARTY.md) for services and
licences.

## Building
```bash
./gradlew buildAll        # every jar -> dist/ + dist/SHA256SUMS (runs the core tests)
./gradlew :core:test      # unit tests
scripts/smoke.sh 1.21.11  # headless launch test (Xvfb + software GL), evidence in smoke-out/
```
Gradle runs on JDK 25, and the build downloads JDK 8 and 21 toolchains itself. Commands, conventions and gotchas are in
[CLAUDE.md](CLAUDE.md).

## Documentation map
| Doc | What it answers |
|---|---|
| [PLAN](docs/PLAN.md) | Phases, deliverables and exit criteria |
| [PROGRESS](docs/PROGRESS.md) | What is done, with evidence |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | Layers (api / core / platform adapters), mixin policy, error isolation |
| [CODE_MAP](docs/CODE_MAP.md) | Every source file with a one-line summary (generated) |
| [DECISIONS](docs/DECISIONS.md) | Why things are the way they are (D-001 …) |
| [RULES_MATRIX](docs/RULES_MATRIX.md) | Every module vs. server rules (Hypixel policy) |
| [COMPAT_MATRIX](docs/COMPAT_MATRIX.md) | Build/launch results per version and launcher |
| [FEATHER](docs/FEATHER.md) | Feather/Dawn launcher research |
| [PLUGIN_API](docs/PLUGIN_API.md) | Writing plugins |
| [PRISM](docs/PRISM.md) | Installing in Prism Launcher (instance zips or manual) |
| [THIRD_PARTY](docs/THIRD_PARTY.md) | Dependencies, licences, online services |
| [debug-log](docs/debug-log.md) | Every bug hunt: repro, hypothesis, evidence, fix |

The repo root also works as an [Obsidian](https://obsidian.md) vault. Open the folder as a vault and use Graph view to
see how the docs and code link together.

## License
MIT ([LICENSE](LICENSE)). Third-party notices: [THIRD_PARTY](docs/THIRD_PARTY.md).
