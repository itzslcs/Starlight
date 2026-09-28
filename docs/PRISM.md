# Prism Launcher

See also: [README](../README.md) (install table), [DECISIONS](DECISIONS.md) (D-017), [COMPAT_MATRIX](COMPAT_MATRIX.md) (Prism column).

Starlight is an ordinary mod jar, so Prism needs nothing special. There are three ways to set it up.

## A. Import a ready-made instance (easiest)
`./gradlew buildAll` writes one instance zip per Minecraft version to `dist/prism/Starlight-<mc>.zip`.

1. In Prism: **Add Instance → Import**, choose the zip, then **OK**.
2. Select the new instance **Starlight &lt;mc&gt;** and press **Launch** (log in with your own account when Prism asks).
3. In game, press **Right Shift** to open Starlight.

Each zip contains:
| File | Contents |
|---|---|
| `mmc-pack.json` | Minecraft + Fabric Loader 0.19.5 (+ intermediary), or Minecraft 1.8.9 + Forge 11.15.1.2318. The component ids and versions were checked against Prism's own metadata |
| `instance.cfg` | name `Starlight <mc>`, everything else left to Prism's defaults |
| `minecraft/mods/` | the Starlight jar for that version |

Starlight needs the same Java as its Minecraft version: 8 for 1.8.9, 21 for 1.21.x, 25 for 26.x. With Prism's automatic
Java setting on, Prism downloads it; otherwise pick one under **Edit → Settings → Java**.

## B. Starlight Performance pack (most FPS on 1.21+)
`scripts/mrpack.py dist` writes `dist/modrinth/Starlight-Performance-<mc>.mrpack`: Starlight plus Sodium, ImmediatelyFast,
FerriteCore and Lithium. In Prism: **Add Instance → Import**, pick the `.mrpack`, then **OK**. Prism downloads those mods from
Modrinth itself; the pack only lists them ([DECISIONS](DECISIONS.md) D-020). The Modrinth app imports `.mrpack` too.

## C. Add the jar to an instance you already have
1. **Edit** the instance → **Version**. It needs *Fabric Loader* (0.16 or newer, 0.18 or newer for 26.x), or *Forge*
   11.15.1.2318 for 1.8.9. **Fabric API is not needed.**
2. **Mods → Add file**, then pick `Starlight-<version>+mc<mc>.jar` for exactly that Minecraft version.

Starlight is meant to run next to performance mods such as Sodium, but that is not tested yet. If a mod does the same job
as a Starlight feature (freelook, perspective), Starlight's matching mixin steps aside ([ARCHITECTURE](ARCHITECTURE.md)). The
standalone KeyCPS mod is not needed: it is built in, and Starlight shows a notice if both are installed.

## Where things are
- Starlight's settings and profiles: `<instance>/minecraft/Starlight/`.
- Log: Prism's **Minecraft Log** tab. Starlight's lines are tagged `(Starlight)`.

## Verification status
The instance zips and a real Prism launch are **not verified yet**: the Prism column of
[COMPAT_MATRIX](COMPAT_MATRIX.md) is `not-run`. Phase 7 checks them with a production-layout launch (the real loader and
the built jar, outside Gradle). That test does not drive your Prism install, because that would use your Microsoft
account.
