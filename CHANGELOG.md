# Changelog

## 0.7.0
- **A look of its own:** buttons are now keycaps, with a lit top edge and a side that glows orange on hover. Sliders
  match. Behind the menus there is a dark backdrop with a warm glow and rising embers, and on the home screen a
  blocky skyline catches that glow. This covers Minecraft's pause menu, server list and options (every version), the
  home screen and MW19's own menu. Themes → *MW19 game menus* switches it all back to vanilla.
- **Skin changes show at once:** after *Use this skin*, MW19 loads the new skin from Mojang and puts it on your player
  in the menus and in your world (other players see it after you rejoin a server). Before, you kept seeing the old skin
  until you restarted the game.
- **Fixed:** on 1.21–1.21.8 the pause menu's *MW19 Menu* and *Packs* row sat above *Back to Game*, as wide as the
  screen. MW19's menu opened from the home screen had a black background.

## 0.6.0
- **Marlow's Crystal Optimizer and Hero's Anchor Optimizer, built in** (1.21+, Mods → Utility, off by default): the two
  MIT mods by Bram & Marlow and by HerobaneNair, ported into MW19 with credit.
  - Crystal optimizer: a crystal you break stops blocking your next click at once, instead of after the server's reply.
    It uses the original mod's server protocol, so a server can see it and switch it off; when one does, you get the
    original's chat notice.
  - Anchor optimizer: a respawn anchor you blow up turns into a see-through ghost block at once, and you can build into it.
  - Neither needs Fabric API. If the original mod is installed, MW19's copy steps aside.

## 0.5.0
- **Vulkan inside MW19 (1.21.9 – 1.21.11):** the jar now carries VulkanMod (LGPL-3.0, unmodified), so one file in the
  mods folder gives you the Vulkan renderer. It never stops your game: the first start runs on OpenGL while MW19 checks in
  the background whether your graphics card can run Vulkan, and switches to it from the next start. If a Vulkan start
  ever fails, the next one is back on OpenGL by itself. Performance page → *Vulkan renderer* shows what is running and
  switches it. With Sodium or Iris installed, VulkanMod stays off. On 26.2+ the switch uses Minecraft's own Vulkan
  renderer, as before.
- **Fast Chests** (on by default, 1.21+): chests, trapped chests, ender chests and copper chests are drawn as ordinary
  blocks instead of every frame, which is much faster in storage rooms. Lids no longer open visibly; turn the module off
  to get the animation back. Works with resource packs that change chest textures.
- **Block entity culling:** Entity Culling now also skips chests, signs, banners and heads hidden behind walls.
- **Minecraft's menus in the MW19 style:** the pause menu, server list and options use the home screen's buttons, and
  the pause menu has **MW19 Menu** and **Packs** (the pack browser in game). Themes → *MW19 game menus* turns it off.
- **New Mods page:** tiles with each module's icon and an ENABLED / DISABLED bar, a list view with descriptions, a
  highlight that glides between categories, and the Performance category is back (it no longer drops off the row).
- **Lighter menu:** it is unloaded when closed (no memory or GPU cost), cheaper corners, blur off by default.
- **Plugins removed**, with the Tier Tags and Session Stats addons.
- **Fixed:** if the very first start of a new install crashed, the next start skipped the automatic graphics preset.
  On 1.21.9+ the Hitboxes module no longer leaves hitboxes switched on in the game's own saved F3 settings.
  Hovering your skin on the home screen and the Skins page no longer shows a "drag to turn" tooltip.

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
- **Faster:** Memory/CPU no longer reads the CPU load on the render thread (it was the costliest HUD module). On
  1.8.9, HUD text is drawn in one batch instead of one draw call per letter: MW19's HUD costs about 30 % less there
  (KeyCPS about 4.5× less).

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
