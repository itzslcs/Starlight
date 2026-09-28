# Third-party components and licenses

Starlight itself is MIT ([`LICENSE`](../LICENSE), shipped in every jar as `LICENSE_starlight`). Licenses below were checked on 2026-09-25
from the artifact itself (POM `<licenses>` or a license file in the jar) or, if the artifact declares none, from the
project's source repository. The *Evidence* column names the source.

## Shipped inside Starlight jars
| Component | Version | Where | License | Evidence | How we comply |
|---|---|---|---|---|---|
| SpongePowered Mixin | 0.7.11-SNAPSHOT | shaded into the **1.8.9** jar only (`org/spongepowered/asm`) | MIT | POM: "MIT license"; jar: `LICENSE.txt` (The MIT License) | notice shipped as `LICENSE_mixin` in the jar; the build-only `org/spongepowered/tools` part is not shipped |
| Marlow's Crystal Optimizer (by Bram and Marlow) | 2.0.0-SNAPSHOT, commit [62831e6](https://github.com/Bram1903/MarlowsCrystalOptimizer/tree/62831e69755797a1572e76d091af880584556653) | ported code in every 1.21+ jar ([`CrystalOptimizer`](../fabric/src/main/java/dev/starlight/fabric/port/CrystalOptimizer.java) and its mixins) | MIT, © 2026 Bram and Marlow | `LICENSE` at that commit (the repository's licence) | see "Ported optimizers" below ([DECISIONS](DECISIONS.md) D-028) |
| HerosAnchorOptimizer (by HerobaneNair) | 1.1.3, commit [8e70b8a](https://github.com/HerobaneNair/herosanchoroptimizer/tree/8e70b8aba3f23d83805469b721725c6062b0d4c2) | ported code in every 1.21+ jar ([`AnchorOptimizer`](../fabric/src/main/java/dev/starlight/fabric/port/AnchorOptimizer.java) and its mixins) | MIT, © 2024 HerobaneNair | `LICENSE` at that commit; `"license": "MIT"` in its [`fabric.mod.json`](../fabric/src/main/resources/fabric.mod.json) | see "Ported optimizers" below (D-028) |
| VulkanMod (by Collateral) | 0.6.8+1.21.11 (1.21.11 jar); 0.6.6 (1.21.9 and 1.21.10 jars) | nested unmodified as jar-in-jar (`META-INF/jars/vulkanmod-<modrinth id>.jar`); it nests its own Fabric API modules (Apache-2.0) and LWJGL Vulkan/VMA/shaderc bindings (BSD-3-Clause) | LGPL-3.0-only | Modrinth project licence; `LICENSE` at the source commits below; `LICENSE_VulkanMod` in its jar | see "VulkanMod" below ([DECISIONS](DECISIONS.md) D-024) |

The Fabric jars otherwise contain only Starlight classes (`dev/starlight/{api,core,fabric}`) and resources, checked by listing
the built jars.

### Ported optimizers
- **What:** the owner asked for these two client mods to be built in "from the code, … give credits" (D-028). Their
  logic and Marlow's Crystal Optimizer's server protocol are ported. Each file's header names the source commit, the
  authors, the licence and every change (for example, vanilla purple stained glass stands in for the anchor mod's own
  ghost block, and no Fabric API is needed). The update checker of Marlow's Crystal Optimizer is not ported.
- **How we comply with the MIT licence:** the copyright and permission notice of each ships in every Fabric jar as
  `META-INF/licenses/MIT-MarlowsCrystalOptimizer.txt` and [`MIT-HerosAnchorOptimizer.txt`](licenses/MIT-HerosAnchorOptimizer.txt) (copied unchanged from
  [`docs/licenses`](licenses/), which match the upstream `LICENSE` files byte for byte). `THIRD_PARTY_NOTICES.txt` and
  the About page name both mods and their authors, and each module's name and description in the Mods page credit them.

### VulkanMod
- **Which builds:** only those whose release commit is public, pinned in [`fabric/bundled.json`](../fabric/bundled.json) by
  [`scripts/vulkanmod-pin.py`](../scripts/vulkanmod-pin.py) with their SHA-512 (the build fails on a mismatch):
  0.6.8+1.21.11 = [d3db079](https://github.com/xCollateral/VulkanMod/tree/d3db07925f60257433233409125a08d7c08bf023)
  ("Bump version", dev branch, 2026-06-14); 0.6.6 for 1.21.9/1.21.10 = tag
  [0.6.6](https://github.com/xCollateral/VulkanMod/tree/67e20f1f359a3a0611e2312a8a72e47fc9a09737). Other Starlight jars
  bundle no VulkanMod (their upstream sources are not published, or Minecraft has its own Vulkan backend).
- **How we comply with the LGPL:** the jar is not modified; `THIRD_PARTY_NOTICES.txt` in every jar that carries it
  names it, its licence and the source commit; `META-INF/licenses/` holds the LGPL-3.0 and GPL-3.0 texts
  ([`docs/licenses`](licenses/)); the About page shows the notice while the game runs (section 4(c)); `dist/sources/`
  holds the source zip of each bundled commit, published next to the jars; a player can use a different or modified
  VulkanMod by putting it in the mods folder, where Fabric loads the newer version instead (section 4(d)). Starlight only
  decides at launch whether VulkanMod runs, through Fabric's own metadata (D-024); it does not link against or change it.

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
| LWJGL Vulkan binding | 3.3.3 (compile only) | BSD-3-Clause | POM of lwjgl-vulkan 3.3.3; at runtime the one VulkanMod nests is used |
| Mesa lavapipe (software Vulkan) | 26.2.2 | MIT | Arch package `vulkan-swrast`; used only to run the Vulkan smoke tests headless |
| Mojang official mappings | per version (Fabric ≤1.21.11 builds) | Mojang mappings licence (use permitted, no redistribution) | applied by Loom at build time and not redistributed. 26.x ships unobfuscated. |
| MCP mappings `stable_22` | 1.8.9 build | MCP terms (mod development use) | applied by Loom at build time and not redistributed |

GPL-2.0 (pack200) and LGPL-3.0 (Stonecutter) apply only to the build tools themselves. We neither link them into nor
ship them with our jars, so they place no obligations on Starlight's output.

## Referenced by the Starlight Performance packs (not redistributed)
The `.mrpack` files ([`scripts/mrpack.py`](../scripts/mrpack.py)) list these by Modrinth CDN URL and hash, and the launcher downloads them from
Modrinth when you import the pack. They are not included in Starlight's jars.

| Mod | License (Modrinth) |
|---|---|
| Fabric API | Apache-2.0 |
| Sodium | PolyForm Shield 1.0.0 (its noncompete clause is why it is referenced, never bundled: D-024) |
| ImmediatelyFast | LGPL-3.0-or-later |
| FerriteCore | MIT |
| Lithium | LGPL-3.0-only |

## Online services (runtime, only when the user turns them on)
| Service | Used by | When |
|---|---|---|
| Modrinth API v2 `https://api.modrinth.com/v2` (search, project versions) and CDN `https://cdn.modrinth.com` | Packs page | while the player uses the Packs page (search, icons, *Install*) |
| Minecraft Services `https://api.minecraftservices.com/minecraft/profile/skins` | Skins page | the player presses *Use this skin* (sends the session token, D-022) |
| Mojang profile APIs `https://api.mojang.com/users/profiles/minecraft/{name}`, `https://sessionserver.mojang.com/session/minecraft/profile/{id}`, `https://textures.minecraft.net` | Skins page (*Copy*, and loading the new skin after *Use this skin*), Host World (*Invite*) | the player presses those buttons |
| MCTiers API v2 `https://mctiers.com/api/v2/profile/{uuid}` (or SubTiers `https://subtiers.net/api/v2`, or a list the player enters) | Tier Tagger module | the module is on (off by default): one lookup per player per 4 hours, sending only the UUID |
| The player's router (UPnP IGD: SSDP multicast, then SOAP on the local network) | Host World | the player switches on *Over the internet* |

No other network access exists in Starlight (see [README](../README.md) → Privacy). The bundled VulkanMod's only network code
(its jars were searched for URLs and HTTP use) is an update check against `api.modrinth.com` at every start, which
[`VulkanUpdateMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/VulkanUpdateMixin.java) turns off. Its options screen also has buttons that open its Modrinth and Ko-fi pages in the
browser when clicked.

## KeyCPS (owner's own mod)
[`core/.../modules/KeyCpsModule.java`](../core/src/main/java/dev/starlight/core/modules/KeyCpsModule.java) and [`InputRates.java`](../core/src/main/java/dev/starlight/core/modules/InputRates.java) port KeyCPS 1.6.1 (modrinth.com/mod/keycps). Its
fabric.mod.json declares MIT, while the Modrinth page lists All Rights Reserved. KeyCPS's author is Starlight's owner, who
asked for the port ([DECISIONS](DECISIONS.md) D-018). No KeyCPS binary is bundled, and the ported code is covered by Starlight's MIT licence.

## ExploitPreventer (idea list only)
Exploit Protection covers the exploits listed on [ExploitPreventer](https://modrinth.com/mod/exploitpreventer)'s Modrinth
page (NikOverflow, MIT). Only that public description was read; the implementation is Starlight's own ([DECISIONS](DECISIONS.md) D-021),
so no ExploitPreventer code or licence notice ships with Starlight.

## Original work
Apart from the KeyCPS port above, all Starlight code, UI, icons (drawn from rectangles in code, [`core/.../gui/Icons.java`](../core/src/main/java/dev/starlight/core/gui/Icons.java), D-009), themes and texts are original. No code or
assets were copied or decompiled from other clients or mods. The Fast Chests models are generated by
[`scripts/fast-chests.py`](../scripts/fast-chests.py) from the layout of Minecraft's own chest textures, which they reference by name (D-025).
Starlight renders text with Minecraft's own font and bundles no fonts. The icon and logo are drawn by
[`scripts/icon.py`](../scripts/icon.py) and [`TitleUi`](../core/src/main/java/dev/starlight/core/gui/TitleUi.java).

**The name:** Spottedleaf's lighting engine mod is also called Starlight (mod id `starlight`, Modrinth `starlight`). It is
unrelated. This client uses the mod id `starlight_client`, the Modrinth slug `starlight-client` and the full name
"Starlight Client" so the two are never confused by a loader or on Modrinth ([DECISIONS](DECISIONS.md) D-033).
