# MW19

A client-side mod suite for Minecraft with a HUD, a GUI with its own module menu and HUD editor, profiles, and a plugin API.
It targets **1.8.9 (Forge)** and **every release from 1.21 to 26.3 (Fabric)**, 18 jars in total, all built from one source tree.
It is legit only: no cheats and no automation. Every module is checked against server rules
([RULES_MATRIX](docs/RULES_MATRIX.md)).

> Status: in development (0.3.0). What has actually been run and passed is in [COMPAT_MATRIX](docs/COMPAT_MATRIX.md),
> and where the project stands is in [PROGRESS](docs/PROGRESS.md).

## Install
| Minecraft | Loader | Jar |
|---|---|---|
| 1.8.9 | Forge 11.15.1.2318 | `MW19-<version>+mc1.8.9.jar` |
| 1.21 – 1.21.11 | Fabric Loader ≥ 0.16 | `MW19-<version>+mc<mc>.jar` |
| 26.1 – 26.3 | Fabric Loader ≥ 0.18 (Java 25) | `MW19-<version>+mc<mc>.jar` |

Put the jar for your version in the instance's `mods` folder. **Fabric API is not needed.**

**Most FPS (1.21+):** import `dist/modrinth/MW19-Performance-<mc>.mrpack` in Prism or the Modrinth app. It is MW19 plus
Sodium, ImmediatelyFast, FerriteCore and Lithium; the launcher downloads those mods from Modrinth.

**Prism Launcher:** import a ready-made instance (`dist/prism/MW19-<mc>.zip`) or add the jar to your own instance.
Both are covered step by step in [PRISM](docs/PRISM.md). Prism has not yet been checked with a real launch; see the
Prism column of [COMPAT_MATRIX](docs/COMPAT_MATRIX.md).

## Using it
- **Home screen:** MW19 replaces Minecraft's title screen: the game's panorama, your own skin in 3D (drag it to turn
  it) with a *Skins* button, the usual buttons, and *Host World*, *Packs* and *MW19 Menu* on the right. *Vanilla menu*
  (bottom right) shows the original once, for buttons other mods add there. Turn it off under Themes → Custom home screen.
- **Right Shift** opens the menu: Mods, HUD Editor, Profiles, Keybinds, Skins, Packs, Host World, Plugins, Server Rules,
  Performance, Themes, About. The pause screen (and the vanilla title screen) also has an MW button.
- **Skins:** preview skins in 3D and change your Minecraft skin (Microsoft account) without leaving the game. Skins are
  64×64 PNG files in `MW19/skins/`: drop files on the window (1.21+), use *Open folder*, or type a player's name and
  press *Copy* to add their current skin. *Use this skin* uploads it to Mojang; others see it after you rejoin a server.
- **Packs:** search Modrinth's resource packs from inside the game (your Minecraft version or all), *Install* downloads
  one into `resourcepacks/` (checked against Modrinth's SHA-512), and *Enable* puts it on top of your packs.
- **Host World:** opens your singleplayer world to friends. On your network it works like *Open to LAN* (game mode and
  commands for guests, address with a Copy button). *Over the internet* asks your router to forward the port (UPnP)
  and turns on a whitelist: only players you invite by name can join. The forward is removed when you stop, leave
  the world or quit. If your router has UPnP off, forward the port shown by hand.
- **Performance page:** **FPS Boost** switches the vanilla options that cost the most frames to fast values in one click,
  and *Undo* restores yours. On 26.2 and 26.3 there is a **Vulkan renderer** switch (it applies after a restart and
  falls back to OpenGL if the GPU cannot run it). Numbers are in [PERF](docs/PERF.md).
- **HUD Editor:** drag elements (they snap), drag a corner to scale, and right-click an element for its text colour and shadow.
- **Profiles:** one layout and module set per profile. They switch automatically per server and can be shared as a
  `MW19-P1:` code.

### Modules
| HUD | Visual | Performance | Utility | Chat |
|---|---|---|---|---|
| FPS, **KeyCPS** (keystrokes + CPS), Speed, Ping, Coordinates, Direction, Armor Status, Potion Effects, Item Counter, Clock, Memory/CPU, FPS Graph, Server Address, Combo, Saturation, Session Time, Pack Display, Stopwatch, Day Counter | Custom Crosshair, Brightness, Hit Color, Damage Tilt, Low Fire, Shield Overlay, Particle Multiplier, Show Own Nametag, 1.8 Combat Visuals, Clear Weather, Low Health Warning | **Entity Culling** (on by default), FPS Boost, Vulkan switch (26.2+) | Zoom, Toggle Sprint/Sneak, Freelook, Screenshot Tools, Durability Alert, **Exploit Protection** | Chat Tools (timestamps, highlights, stacking, search) |

KeyCPS is the author's own [KeyCPS](https://modrinth.com/mod/keycps) mod, built in ([D-018](docs/DECISIONS.md)).
**Exploit Protection** closes the client-side probes that [ExploitPreventer](https://modrinth.com/mod/exploitpreventer)
lists, in MW19's own code ([D-021](docs/DECISIONS.md)): servers cannot read your mods, keybinds or language through sign
and anvil text, cannot make Minecraft contact your computer or home network through a resource pack address (and, on
1.8.9, cannot check which files are on your disk through `level://` packs), and server packs are cached per account.
It is GRAY in the rules matrix (it changes what the client answers a probing server), so switch it on under Mods.
Modules that some servers restrict (**GRAY**) are off by default, and a server can switch modules off
([`serverrules.json`](core/src/main/resources/mw19/serverrules.json), Server Rules page). The Hypixel suite for 1.8.9 is planned ([PLAN](docs/PLAN.md), Phase 6).

### Plugins
Plugin jars go in `MW19/plugins/`. Each new jar is off until you approve it. Plugins run with the same rights as
the game, so approve only plugins you trust. Two plugins ship with MW19: **Tier Tags** (PvP tier-list ranks next to
names) and **Session Stats**, which is the example for [PLUGIN_API](docs/PLUGIN_API.md).

## Privacy
MW19 has no telemetry, accounts or analytics. It reads your session token only when you press *Use this skin*, and
sends it only to Mojang's skin service; it is never logged or stored, and the plugin API has no access to it. The only
network requests happen when you use a feature that needs them:

| What | When | Where to |
|---|---|---|
| Resource pack search, icons and downloads | while you use the Packs page | `api.modrinth.com`, `cdn.modrinth.com` |
| Changing your skin | you press *Use this skin* | `api.minecraftservices.com` (with your session token) |
| Copying a player's skin | you press *Copy* on the Skins page | `api.mojang.com`, `sessionserver.mojang.com`, `textures.minecraft.net` |
| Inviting a player to your world | you press *Invite* on Host World | `api.mojang.com` (name to player id) |
| Port forwarding (UPnP) | you switch on *Over the internet* on Host World | your router only (local network) |
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
| [CHANGELOG](CHANGELOG.md) | What changed in each version |
| [PLAN](docs/PLAN.md) | Phases, deliverables and exit criteria |
| [PROGRESS](docs/PROGRESS.md) | What is done, with evidence |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | Layers (api / core / platform adapters), mixin policy, error isolation |
| [CODE_MAP](docs/CODE_MAP.md) | Every source file with a one-line summary (generated) |
| [DECISIONS](docs/DECISIONS.md) | Why things are the way they are (D-001 …) |
| [RULES_MATRIX](docs/RULES_MATRIX.md) | Every module vs. server rules (Hypixel policy) |
| [COMPAT_MATRIX](docs/COMPAT_MATRIX.md) | Build/launch results per version and launcher |
| [FEATHER](docs/FEATHER.md) | Feather/Dawn launcher research |
| [PERF](docs/PERF.md) | FPS features and how they are measured |
| [PLUGIN_API](docs/PLUGIN_API.md) | Writing plugins |
| [PRISM](docs/PRISM.md) | Installing in Prism Launcher (instance zips or manual) |
| [THIRD_PARTY](docs/THIRD_PARTY.md) | Dependencies, licences, online services |
| [debug-log](docs/debug-log.md) | Every bug hunt: repro, hypothesis, evidence, fix |

The repo root also works as an [Obsidian](https://obsidian.md) vault. Open the folder as a vault and use Graph view to
see how the docs and code link together.

## License
MIT ([LICENSE](LICENSE)). Third-party notices: [THIRD_PARTY](docs/THIRD_PARTY.md).
