# Changelog

## 0.4.0
- **Graphics presets:** Potato, Low, Medium and High on the Performance page, all tuned for frame rate, with Undo.
  **Auto-detection:** on first start MW19 reads your graphics card (and CPU threads and memory) and picks a preset by
  itself; the Performance page shows what it detected. FPS Boost became these presets.
- **Smooth menus:** page cross-fades, a sliding sidebar highlight and a staggered home screen entrance (the animation
  speed setting turns them off).
- **Themes:** Black, White and Crystal.
- **New modules:** Hitboxes (vanilla's F3+B view as a module), TNT Timer, Reach Display (off on Hypixel), Quick
  Commands (a key sends one command, like /hub).
- **Custom Crosshair:** finer steps and an overall scale, down to a quarter size.
- **Faster:** Memory/CPU no longer reads the CPU load on the render thread (it was the costliest HUD module).

## 0.3.0
- **New home screen look:** the game's own panorama, a pixel-block MW19 logo, your skin in 3D on the left (drag to
  turn it) and *Host World*, *Packs* and *MW19 Menu* on the right. No more particles or tip cards.
- **Skins:** preview skins in 3D, copy any player's skin by name, and change your own skin in game (Microsoft
  accounts; uses Mojang's official skin service).
- **Packs:** search and install resource packs from Modrinth in game, then enable them with one click.
- **Host World:** open your world to friends on your network, or over the internet through your router (UPnP) with
  an invite-only whitelist.
- **Exploit Protection** (off by default, GRAY): servers cannot detect your mods through sign/anvil text probes, cannot
  probe your home network (or, on 1.8.9, your files) through resource pack addresses, and server packs are cached per
  account. Our own implementation of the protections ExploitPreventer lists.
- **New modules:** Saturation, Session Time, Pack Display, Stopwatch, Day Counter, Low Health Warning.
- **Fixed:** at exit, two config saves could race and one failed. The 1.8.9 build reported its version as "dev" in dev runs.

## 0.2.0: MW19
- **New name:** Kestrel is now **MW19 Client**. Existing settings (`Kestrel/`) move to `MW19/` automatically, and old
  profile codes still import.
- **New home screen** replaces the title screen (Themes → Custom home screen turns it off; *Vanilla menu* shows the old one).
- **More FPS:** Entity Culling (on by default) skips mobs and items hidden behind blocks. **FPS Boost** on the
  Performance page switches the heaviest vanilla options to fast values in one click, with Undo. On 26.2 and 26.3 a
  **Vulkan renderer** switch uses Minecraft's own Vulkan backend.
- **New modules:** Speed, FPS Graph, Server Address, Clear Weather, Durability Alert.
- **Fixed:** clicking inside the Right Shift menu did nothing. Toggle Sprint/Sneak broke after a restart. Rainbow
  (chroma) colours did not animate.
- KeyCPS keystrokes + CPS is built in (it replaces the old CPS and Keystrokes modules).

## 0.1.0
- First alpha: HUD editor, profiles, plugin API, server rules, 1.8.9 and 1.21 – 26.3.
