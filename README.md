# Starlight

A client-side mod suite for Minecraft focused on frame rate, with a HUD, its own module menu and HUD editor, profiles,
and animated themes (a night sky by default). Formerly MW19.
It targets **1.8.9 (Forge)** and **every release from 1.21 to 26.3 (Fabric)**, 18 jars in total, all built from one source tree.
It is legit only: no cheats and no automation. Every module is checked against server rules
([RULES_MATRIX](docs/RULES_MATRIX.md)).

> Status: in development (0.8.0). What has actually been run and passed is in [COMPAT_MATRIX](docs/COMPAT_MATRIX.md),
> and where the project stands is in [PROGRESS](docs/PROGRESS.md).

## Install
| Minecraft | Loader | Jar |
|---|---|---|
| 1.8.9 | Forge 11.15.1.2318 | `Starlight-<version>+mc1.8.9.jar` |
| 1.21 – 1.21.11 | Fabric Loader ≥ 0.16 | `Starlight-<version>+mc<mc>.jar` |
| 26.1 – 26.3 | Fabric Loader ≥ 0.18 (Java 25) | `Starlight-<version>+mc<mc>.jar` |

Put the jar for your version in the instance's `mods` folder. **Fabric API is not needed**, and nothing else is: one jar.

**Coming from MW19?** Replace the MW19 jar with the Starlight one (Fabric will not start with both). Your settings move
over by themselves: the `MW19/` folder becomes `Starlight/` on first start, and MW19 profile codes still import.

**Vulkan built in (1.21.9 – 1.21.11):** those jars carry [VulkanMod](https://github.com/xCollateral/VulkanMod)
(LGPL-3.0, unmodified). The first start runs on OpenGL while Starlight checks whether your graphics card can run Vulkan;
from the next start it does. A Vulkan start that fails puts the next one back on OpenGL by itself, so it cannot lock you
out. With Sodium or Iris installed, VulkanMod stays off. On 26.2 and 26.3 the same switch uses Minecraft's own Vulkan
renderer. Other versions have no VulkanMod build with published source, so their jars bundle none (details: [D-024](docs/DECISIONS.md)).

**With Sodium instead (1.21+):** import `dist/modrinth/Starlight-Performance-<mc>.mrpack` in Prism or the Modrinth app. It is
Starlight plus Sodium, ImmediatelyFast, FerriteCore and Lithium (and Fabric API, which Sodium needs); the launcher downloads
those mods from Modrinth. Sodium's licence does not allow bundling it in Starlight.

**Prism Launcher:** import a ready-made instance (`dist/prism/Starlight-<mc>.zip`) or add the jar to your own instance.
Both are covered step by step in [PRISM](docs/PRISM.md). Prism has not yet been checked with a real launch; see the
Prism column of [COMPAT_MATRIX](docs/COMPAT_MATRIX.md).

## Using it
- **Home screen:** Starlight replaces Minecraft's title screen: your theme's animated scene, your own skin in 3D (drag it to turn
  it) with a *Skins* button, the usual buttons, and *Host World*, *Packs* and *Starlight Menu* on the right. *Vanilla menu*
  (bottom right) shows the original once, for buttons other mods add there. Turn it off under Themes → Custom home screen.
- **Right Shift** opens the menu: Mods (tiles with an ENABLED/DISABLED bar, or a list), HUD Editor, Profiles, Keybinds,
  Skins, Packs, Host World, Server Rules, Performance, Themes, About. The pause menu has **Starlight Menu** and **Packs**, and
  Minecraft's own menus (pause, server list, options) use Starlight's look: keycap buttons and sliders over your
  theme's scene (Themes → Starlight game menus, [D-029](docs/DECISIONS.md)).
- **Skins:** preview skins in 3D and change your Minecraft skin (Microsoft account) without leaving the game. Skins are
  64×64 PNG files in `Starlight/skins/`: drop files on the window (1.21+), use *Open folder*, or type a player's name and
  press *Copy* to add their current skin. *Use this skin* uploads it to Mojang, and you see it on your player within
  seconds (1.21+; on 1.8.9 after a restart). Others see it after you rejoin a server.
- **Packs:** search Modrinth's resource packs from inside the game (your Minecraft version or all), *Install* downloads
  one into `resourcepacks/` (checked against Modrinth's SHA-512), and *Enable* puts it on top of your packs.
- **Host World:** opens your singleplayer world to friends. On your network it works like *Open to LAN* (game mode and
  commands for guests, address with a Copy button). *Over the internet* asks your router to forward the port (UPnP)
  and turns on a whitelist: only players you invite by name can join. The forward is removed when you stop, leave
  the world or quit. If your router has UPnP off, forward the port shown by hand.
- **Graphics presets (Performance page):** **Potato** (integrated graphics, old laptops), **Low**, **Medium** and **High**
  set the vanilla video options in one click, all tuned for frame rate (no VSync, unlimited FPS, even High stays at the
  default view distance). **Auto** picks one from your graphics card, CPU and memory, and Starlight does that by itself the
  first time it starts (it never raises your view distance then). *Undo* restores your own settings, even after a
  restart. The **Vulkan renderer** row (1.21.9 – 1.21.11 and 26.2+) shows what renders now and switches it for the next
  start. Numbers are in [PERF](docs/PERF.md).
- **Themes:** ten themes, each with its own animated scene behind the menus ([D-031](docs/DECISIONS.md)): **Starlight** (a night sky
  with twinkling stars, a shooting star and a crescent moon; the default), **Ember** (rising embers over a block skyline),
  **Aurora** (northern lights over pines), **Nebula** (glowing gas clouds), **Glacier** and **Crystal** (snowfall over
  mountains), **Sakura** (falling petals at dusk), **Daylight** and **White** (blocky clouds and a square sun) and
  **Black** (OLED). The Themes page shows them live, and you can set your own accent colour. Menus animate (page cross-fades, a sliding sidebar highlight, a staggered home screen); set the animation speed to 0
  to turn that off.
- **HUD Editor:** drag elements (they snap), drag a corner to scale, and right-click an element for its text colour and shadow.
- **Bind profiles (Keybinds page):** save Minecraft's key binds as a named profile and switch between profiles in one
  click. The default profile (★) is applied to every new instance on its first start. Profiles are shared by all your
  instances and versions ([D-032](docs/DECISIONS.md)).
- **Profiles:** one layout and module set per profile. They switch automatically per server and can be shared as a
  `STARLIGHT-P1:` code.

### Modules
| HUD | Visual | Performance | Utility | Chat |
|---|---|---|---|---|
| FPS, **KeyCPS** (keystrokes + CPS), Speed, Ping, Coordinates, Direction, Armor Status, Potion Effects, Item Counter, Clock, Memory/CPU, FPS Graph, Server Address, Combo, Saturation, Session Time, Pack Display, Stopwatch, Day Counter, Reach Display | Custom Crosshair, Brightness, Hit Color, Damage Tilt, Low Fire, Shield Overlay, Particle Multiplier, Show Own Nametag, 1.8 Combat Visuals, Clear Weather, Low Health Warning, Hitboxes, TNT Timer | **Entity Culling** (entities and block entities behind walls, on by default), **Fast Chests** (chests drawn as blocks, 1.21+, on by default; lids do not open visibly), graphics presets with auto-detection, Vulkan renderer | Zoom, Toggle Sprint/Sneak, Freelook, Screenshot Tools, Durability Alert, **Exploit Protection**, Quick Commands, **Tier Tagger**, **Marlow's Crystal Optimizer**, **Hero's Anchor Optimizer** (1.21+) | Chat Tools (timestamps, highlights, stacking, search) |

KeyCPS is the author's own [KeyCPS](https://modrinth.com/mod/keycps) mod, built in ([D-018](docs/DECISIONS.md)).
**Exploit Protection** closes the client-side probes that [ExploitPreventer](https://modrinth.com/mod/exploitpreventer)
lists, in Starlight's own code ([D-021](docs/DECISIONS.md)): servers cannot read your mods, keybinds or language through sign
and anvil text, cannot make Minecraft contact your computer or home network through a resource pack address (and, on
1.8.9, cannot check which files are on your disk through `level://` packs), and server packs are cached per account.
It is GRAY in the rules matrix (it changes what the client answers a probing server), so switch it on under Mods.
**Tier Tagger** shows PvP tiers from [MCTiers](https://mctiers.com) (or SubTiers, or your own list) after player names, on
nametags and in the tab list ([D-030](docs/DECISIONS.md)). It is off by default and off on Hypixel.
**Marlow's Crystal Optimizer** (by Bram and Marlow) and **Hero's Anchor Optimizer** (by HerobaneNair) are those two MIT
mods ported in at the owner's request, with credit ([D-028](docs/DECISIONS.md)). A crystal you break, or an anchor you blow up, clears at once
instead of after the server's reply. They are GRAY and off by default. The crystal optimizer tells the server it is there,
using the original mod's protocol, and a server can switch it off for its players.
Modules that some servers restrict (**GRAY**) are off by default, and a server can switch modules off
([`serverrules.json`](core/src/main/resources/starlight/serverrules.json), Server Rules page). The Hypixel suite for 1.8.9 is planned ([PLAN](docs/PLAN.md), Phase 6).

## Privacy
Starlight has no telemetry, accounts or analytics. It reads your session token only when you press *Use this skin*, and
sends it only to Mojang's skin service; it is never logged or stored. The only network requests happen when you use a
feature that needs them:

| What | When | Where to |
|---|---|---|
| Resource pack search, icons and downloads | while you use the Packs page | `api.modrinth.com`, `cdn.modrinth.com` |
| Changing your skin | you press *Use this skin* | `api.minecraftservices.com` (with your session token), then `sessionserver.mojang.com` to load the new skin |
| Copying a player's skin | you press *Copy* on the Skins page | `api.mojang.com`, `sessionserver.mojang.com`, `textures.minecraft.net` |
| Inviting a player to your world | you press *Invite* on Host World | `api.mojang.com` (name to player id) |
| Port forwarding (UPnP) | you switch on *Over the internet* on Host World | your router only (local network) |
| Player tier lookups (their UUID only) | the Tier Tagger module is on and other players are near or in the tab list | `mctiers.com` (or `subtiers.net`, or the list you enter) |
| Crystal optimizer channels and version (`marlowcrystal:*`) | Marlow's Crystal Optimizer is on and you join a server | only the server you play on, over the game connection |

Bind profiles are a file on your computer and are never uploaded: `~/.starlight/bind-profiles.json`, shared by your
instances (with a Flatpak launcher such as Prism's Flatpak, in the launcher's own data folder, shared by its instances). The bundled VulkanMod's own update check (which would ask Modrinth for a newer version at every start) is switched off.
See [THIRD_PARTY](docs/THIRD_PARTY.md) for services and licences.

## Building
```bash
./gradlew buildAll        # every jar -> dist/ + dist/SHA256SUMS (runs the core tests)
./gradlew :core:test      # unit tests
scripts/smoke.sh 1.21.11  # headless launch test (Xvfb + software GL), evidence in smoke-out/
PROD=1 scripts/smoke.sh 1.21.11  # the same with the built jar in a real Fabric production launch
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
| [PRISM](docs/PRISM.md) | Installing in Prism Launcher (instance zips or manual) |
| [PUBLISHING](docs/PUBLISHING.md) | Uploading to Modrinth and pushing to GitHub, step by step |
| [MODRINTH](docs/MODRINTH.md) | The Modrinth listing text |
| [THIRD_PARTY](docs/THIRD_PARTY.md) | Dependencies, licences, online services |
| [debug-log](docs/debug-log.md) | Every bug hunt: repro, hypothesis, evidence, fix |

The repo root also works as an [Obsidian](https://obsidian.md) vault. Open the folder as a vault and use Graph view to
see how the docs and code link together.

## License
MIT ([LICENSE](LICENSE)). The 1.21+ jars contain code ported from Marlow's Crystal Optimizer (© 2026 Bram and Marlow)
and HerosAnchorOptimizer (© 2024 HerobaneNair), both MIT, with their licence texts in `META-INF/licenses`. The 1.21.9 –
1.21.11 jars also contain VulkanMod under the GNU LGPL 3.0, with its licence texts and a link to its source inside the
jar, and its source zip in `dist/sources/`. Third-party notices: [THIRD_PARTY](docs/THIRD_PARTY.md).
