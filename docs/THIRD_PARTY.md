# Third-party components and licenses

MW19 itself is MIT ([`LICENSE`](../LICENSE), shipped in every jar as `LICENSE_mw19`). Licenses below were checked on 2026-09-25
from the artifact itself (POM `<licenses>` or a license file in the jar) or, if the artifact declares none, from the
project's source repository. The *Evidence* column names the source.

## Shipped inside MW19 jars
| Component | Version | Where | License | Evidence | How we comply |
|---|---|---|---|---|---|
| SpongePowered Mixin | 0.7.11-SNAPSHOT | shaded into the **1.8.9** jar only (`org/spongepowered/asm`) | MIT | POM: "MIT license"; jar: `LICENSE.txt` (The MIT License) | notice shipped as `LICENSE_mixin` in the jar; the build-only `org/spongepowered/tools` part is not shipped |

Nothing else is bundled. The Fabric jars contain only MW19 classes (`dev/mw19/{api,core,fabric}`) and resources,
checked by listing the built jars. The addon jars contain only their own classes.

## Provided by the player's install (not shipped by us)
| Component | Used by | License | Evidence |
|---|---|---|---|
| Fabric Loader (≥0.16; ≥0.18 for 26.x) | Fabric targets | Apache-2.0 | `LICENSE_fabric-loader` in fabric-loader-0.19.5.jar |
| MixinExtras (inside Fabric Loader) | Fabric mixins (`@ModifyExpressionValue`, `@Local`) | MIT | POM of mixinextras-fabric 0.5.5 |
| Minecraft Forge 1.8.9-11.15.1.2318 | 1.8.9 target | Forge Public License | POM of forge 1.8.9-11.15.1.2318 |
| Minecraft | all | Mojang EULA (proprietary) | not redistributed; referenced only by name through mixins |

## Build and test tools (not shipped)
| Component | Version | License | Evidence |
|---|---|---|---|
| Gradle (+ wrapper jar in this repo) | 9.8.0 | Apache-2.0 | GitHub API `repos/gradle/gradle/license` |
| Stonecutter | 0.9.8 | LGPL-3.0 | GitHub API `repos/stonecutter-versioning/stonecutter/license` (the POM declares none) |
| loom-back-compat | 0.4.2 | Unlicense (public domain) | `UNLICENSE` in codeberg.org/KikuGie/loom-back-compat (the POM declares none) |
| Fabric Loom | 1.18.2 | MIT | GitHub API `repos/FabricMC/fabric-loom/license` |
| Essential architectury-loom | 1.15.50 | MIT | GitHub API `repos/EssentialGG/architectury-loom/license` |
| architectury-pack200 | 0.1.3 | GPL-2.0 with Classpath exception | its `LICENSE` file ("subject to the 'Classpath' exception") |
| foojay-resolver-convention | 1.0.0 | Apache-2.0 | GitHub API `repos/gradle/foojay-toolchains/license` |
| JUnit | 5.13.4 | EPL-2.0 | POM of junit-jupiter-api 5.13.4 |
| Mojang official mappings | per version (Fabric ≤1.21.11 builds) | Mojang mappings licence (use permitted, no redistribution) | applied by Loom at build time and not redistributed. 26.x ships unobfuscated. |
| MCP mappings `stable_22` | 1.8.9 build | MCP terms (mod development use) | applied by Loom at build time and not redistributed |

GPL-2.0 (pack200) and LGPL-3.0 (Stonecutter) apply only to the build tools themselves. We neither link them into nor
ship them with our jars, so they place no obligations on MW19's output.

## Referenced by the MW19 Performance packs (not redistributed)
The `.mrpack` files ([`scripts/mrpack.py`](../scripts/mrpack.py)) list these by Modrinth CDN URL and hash, and the launcher downloads them from
Modrinth when you import the pack. They are not included in MW19's jars.

| Mod | License (Modrinth) |
|---|---|
| Sodium | PolyForm Shield 1.0.0 |
| ImmediatelyFast | LGPL-3.0-or-later |
| FerriteCore | MIT |
| Lithium | LGPL-3.0-only |

## Online services (runtime, only when the user turns them on)
| Service | Used by | When |
|---|---|---|
| MCTiers API v2 `https://mctiers.com/api/v2/profile/{uuid}` | Tier Tags addon | only while the Tier Tags module is enabled (GRAY, default off) and the plugin is approved |
| SubTiers API v2 `https://subtiers.net/api/v2/profile/{uuid}` | Tier Tags addon | same, if the user picks SubTiers |
| A user-entered `https://` v2-compatible URL | Tier Tags addon | same, if the user enters one |
| Modrinth API v2 `https://api.modrinth.com/v2` (search, project versions) and CDN `https://cdn.modrinth.com` | Packs page | while the player uses the Packs page (search, icons, *Install*) |
| Minecraft Services `https://api.minecraftservices.com/minecraft/profile/skins` | Skins page | the player presses *Use this skin* (sends the session token, D-022) |
| Mojang profile APIs `https://api.mojang.com/users/profiles/minecraft/{name}`, `https://sessionserver.mojang.com/session/minecraft/profile/{id}`, `https://textures.minecraft.net` | Skins page (*Copy*), Host World (*Invite*) | the player presses those buttons |
| The player's router (UPnP IGD: SSDP multicast, then SOAP on the local network) | Host World | the player switches on *Over the internet* |

No other network access exists in MW19 (see [README](../README.md) → Privacy).

## KeyCPS (owner's own mod)
[`core/.../modules/KeyCpsModule.java`](../core/src/main/java/dev/mw19/core/modules/KeyCpsModule.java) and [`InputRates.java`](../core/src/main/java/dev/mw19/core/modules/InputRates.java) port KeyCPS 1.6.1 (modrinth.com/mod/keycps). Its
fabric.mod.json declares MIT, while the Modrinth page lists All Rights Reserved. KeyCPS's author is MW19's owner, who
asked for the port ([DECISIONS](DECISIONS.md) D-018). No KeyCPS binary is bundled, and the ported code is covered by MW19's MIT licence.

## ExploitPreventer (idea list only)
Exploit Protection covers the exploits listed on [ExploitPreventer](https://modrinth.com/mod/exploitpreventer)'s Modrinth
page (NikOverflow, MIT). Only that public description was read; the implementation is MW19's own ([DECISIONS](DECISIONS.md) D-021),
so no ExploitPreventer code or licence notice ships with MW19.

## Original work
Apart from the KeyCPS port above, all MW19 code, UI, icons (drawn from rectangles in code, [`core/.../gui/Icons.java`](../core/src/main/java/dev/mw19/core/gui/Icons.java), D-009), themes and texts are original. No code or
assets were copied or decompiled from other clients or mods. TierTagger (MPL-2.0) and Tiers (GPL-3.0) were used only
to identify which public tier lists exist (DECISIONS D-015). Their code was never read or copied. MW19 renders text
with Minecraft's own font and bundles no fonts.
