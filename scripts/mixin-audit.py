#!/usr/bin/env python3
"""Proves Starlight's injections were applied: disassembles the classes Mixin exported during a dev run
(-Dmixin.debug.export) and checks each target method calls our handler.
usage: scripts/mixin-audit.py <run-dir> <mc-version>   (exit 1 if anything is not wired)"""
import glob, os, re, subprocess, sys

run, mc = sys.argv[1], sys.argv[2]
def ver(v): return tuple(int(x) for x in re.findall(r'\d+', v))
V = ver(mc)
if mc == "1.8.9":
    checks = [("net.minecraft.client.renderer.entity.RendererLivingEntity", "setBrightness", "starlight$r"),
              ("net.minecraft.client.renderer.entity.RendererLivingEntity", "canRenderName", "starlight$own"),
              ("net.minecraft.client.renderer.EntityRenderer", "hurtCameraEffect", "starlight$tilt"),
              ("net.minecraft.client.renderer.EntityRenderer", "orientCamera", "starlight$yaw"),
              ("net.minecraft.client.renderer.ItemRenderer", "renderFireInFirstPerson", "starlight$fireAlpha"),
              ("net.minecraft.client.particle.EffectRenderer", "emitParticleAtEntity", "starlight$more"),
              ("net.minecraft.entity.Entity", "setAngles", "starlight$turn"),
              ("net.minecraft.client.Minecraft", "shutdownMinecraftApplet", "starlight$shutdown"),
              ("net.minecraft.client.renderer.entity.RenderManager", "shouldRender", "starlight$cull"),
              ("net.minecraft.world.World", "getRainStrength", "starlight$rain"),
              ("net.minecraft.client.network.NetHandlerPlayClient", "handleResourcePack", "starlight$guard"),
              ("net.minecraft.client.gui.GuiScreen", "drawWorldBackground", "starlight$backdrop"),
              # Tier Tagger (nametags and the tab list)
              ("net.minecraft.client.renderer.entity.Render", "renderLivingLabel", "starlight$label"),
              ("net.minecraft.client.gui.GuiPlayerTabOverlay", "getPlayerName", "starlight$decorate"),
              ("net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher", "renderTileEntity", "starlight$cull")]
    jars = [j for j in glob.glob(os.path.expanduser("~/.gradle/caches/essential-loom/minecraftMaven/**/*.jar"), recursive=True)
            if "sources" not in j and "-srg-" not in j and "-intermediary-" not in j]
else:
    unobf = V >= (26, 1)  # 26.1: render -> extract, new homes for FOV, gamma and camera angles
    hud = "net.minecraft.client.gui.Hud" if V >= (26, 2) else "net.minecraft.client.gui.Gui"
    hands = "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer" if V >= (26, 3) else "net.minecraft.client.renderer.ItemInHandRenderer"
    swap = "net.minecraft.client.player.FirstPersonHandsAndItems" if V >= (26, 3) else "net.minecraft.client.renderer.ItemInHandRenderer"
    fire = ([("net.minecraft.client.renderer.ScreenEffectRenderer", "lambda$submitFire$0", "starlight$fireY"),
             ("net.minecraft.client.renderer.ScreenEffectRenderer", "buildFireQuad", "starlight$fireAlpha")] if V >= (26, 2) else
            [("net.minecraft.client.renderer.ScreenEffectRenderer", "renderFire", "starlight$fireAlpha")])
    checks = [(hud, "extractRenderState" if unobf else "render", "starlight$hud"),
              (hud, "extractCrosshair" if unobf else "renderCrosshair", "starlight$crosshair"),
              ("net.minecraft.client.Minecraft", "tick", "starlight$tickStart"),
              ("net.minecraft.client.KeyboardHandler", "keyPress", "starlight$key"),
              ("net.minecraft.client.MouseHandler", "onScroll", "starlight$scroll"),
              ("net.minecraft.client.renderer.LightmapRenderStateExtractor", "extract", "starlight$gamma") if unobf else
              ("net.minecraft.client.renderer.LightTexture", "updateLightTexture", "starlight$gamma"),
              ("net.minecraft.client.Camera", "calculateFov", "starlight$zoom") if unobf else
              ("net.minecraft.client.renderer.GameRenderer", "getFov", "starlight$zoom"),
              ("net.minecraft.client.renderer.GameRenderer", "bobHurt", "starlight$tilt"),
              *fire,
              (swap, "tick", "starlight$noDip"),
              (hands, "submitArmWithItem" if V >= (26, 2) else "renderArmWithItem", "starlight$shieldDown"),
              ("net.minecraft.client.Camera", "alignWithEntity" if unobf else "setup", "starlight$pitch"),
              ("net.minecraft.world.entity.Entity", "turn", "starlight$turn"),
              ("net.minecraft.client.gui.components.ChatComponent", "addMessage", "starlight$chat"),
              ("net.minecraft.client.particle.ParticleEngine", "createTrackingEmitter", "starlight$more"),
              ("net.minecraft.client.renderer.entity.LivingEntityRenderer", "shouldShowName", "starlight$own"),
              ("net.minecraft.client.multiplayer.MultiPlayerGameMode", "attack", "starlight$attack"),
              ("net.minecraft.client.gui.Gui" if V >= (26, 2) else "net.minecraft.client.Minecraft", "setScreen", "starlight$home"),
              ("net.minecraft.client.renderer.entity.EntityRenderDispatcher", "shouldRender", "starlight$cull"),
              ("net.minecraft.world.level.Level", "getRainLevel", "starlight$rain"),
              # Exploit Protection (the smoke self-test loads the sign and anvil screens)
              ("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen", "AbstractSignEditScreen", "starlight$guard"),
              ("net.minecraft.client.gui.screens.inventory.AnvilScreen", "slotChanged", "starlight$guard"),
              ("net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl", "parseResourcePackUrl", "starlight$local"),
              ("net.minecraft.util.HttpUtil", "downloadFile", "starlight$local"),
              ("net.minecraft.client.Minecraft", "Minecraft", "starlight$perAccount"),
              ("net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher",
               "tryExtractRenderState" if V >= (1, 21, 9) else "render", "starlight$cull"),
              # Fast Chests: the pack source, the skipped block entity renderer, and per-version chest block paths
              ("net.minecraft.server.packs.repository.BuiltInPackSource", "loadPacks", "starlight$packs"),
              ("net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher",
               "tryExtractRenderState" if V >= (1, 21, 9) else "render", "starlight$baked"),
              # Crystal and anchor optimizers (ported, D-028): the smoke's optimizer stage runs every one of these
              ("net.minecraft.client.multiplayer.MultiPlayerGameMode", "attack", "starlight$crystal"),
              ("net.minecraft.client.multiplayer.MultiPlayerGameMode", "performUseItemOn", "starlight$anchor"),
              ("net.minecraft.world.level.Level", "getEntities", "starlight$hideKeptCrystals"),
              ("net.minecraft.client.multiplayer.ClientLevel", "entitiesForRendering", "starlight$hideKeptCrystals"),
              ("net.minecraft.client.multiplayer.ClientLevel", "handleBlockChangedAck", "starlight$releaseKeptCrystals"),
              ("net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl", "handleCustomPayload", "starlight$crystalMessage"),
              ("net.minecraft.network.protocol.common.custom.DiscardedPayload", "codec", "starlight$crystalChannels"),
              ("net.minecraft.network.protocol.common.custom.CustomPacketPayload$1", "encode", "starlight$crystalMessage"),
              ("net.minecraft.world.item.context.BlockPlaceContext", "BlockPlaceContext", "starlight$anchorGhost"),
              # Starlight look (keycaps are checked through the pause menu row; this is the backdrop) and the skin refresh
              ("net.minecraft.client.gui.screens.Screen", "extractBackground" if unobf else "renderBackground", "starlight$backdrop"),
              ("net.minecraft.client.gui.components.AbstractSliderButton", "extractWidgetRenderState" if unobf else "renderWidget", "starlight$style"),
              ("net.minecraft.client.gui.components.AbstractSliderButton", "extractWidgetRenderState" if unobf else "renderWidget", "starlight$hideSprite"),
              ("net.minecraft.client.Minecraft", "starlight$profileFuture", "profileFuture"),
              ("net.minecraft.client.multiplayer.PlayerInfo", "starlight$setSkinLookup", "skinLookup"),
              # Tier Tagger (nametags and the tab list)
              ("net.minecraft.client.renderer.entity.EntityRenderer", "getNameTag" if V >= (1, 21, 2) else "renderNameTag", "starlight$decorate"),
              ("net.minecraft.client.gui.components.PlayerTabOverlay", "getNameForDisplay", "starlight$decorate")]
    if V >= (26, 2):  # the pause menu stopped calling Screen's background (debug-log 2026-09-27)
        checks += [("net.minecraft.client.gui.screens.PauseScreen", "extractBackground", "starlight$backdrop")]
    if V >= (1, 21, 9):  # Hitboxes without touching the saved debug profile
        checks += [("net.minecraft.client.gui.components.debug.DebugScreenEntryList", "isCurrentlyEnabled", "starlight$hitboxes")]
    if V < (1, 21, 4):
        checks += [("net.minecraft.world.level.block.ChestBlock", "getRenderShape", "starlight$model"),
                   ("net.minecraft.world.level.block.EnderChestBlock", "getRenderShape", "starlight$model")]
    elif V < (26, 1):  # the smoke's chest minecart and block display load it
        checks += [("net.minecraft.client.renderer.SpecialBlockModelRenderer", "renderByBlock", "starlight$baked")]
    M = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")
    jars = [j for p in (f"{M}/minecraft-clientonly/{mc}-loom.mappings.*/*.jar", f"{M}/minecraft-common/{mc}-loom.mappings.*/*.jar",
                        f"{M}/minecraft-merged/{mc}/*.jar", f"{M}/minecraft-clientonly/{mc}/*.jar", f"{M}/minecraft-common/{mc}/*.jar")
            for j in glob.glob(p) if "sources" not in j]
    if not jars:  # 26.x ships unobfuscated: Loom keeps the raw, already-named jar
        jars = glob.glob(os.path.expanduser(f"~/.gradle/caches/fabric-loom/{mc}/minecraft-client.jar"))
out_dir = os.path.join(run, ".mixin.out", "class")
# A run with VulkanMod active (smoke.sh WITH_MODS=vulkanmod or the bundle): its update check must be switched off.
if os.path.exists(os.path.join(out_dir, "net/vulkanmod/config/UpdateChecker.class")):
    checks.append(("net.vulkanmod.config.UpdateChecker", "checkForUpdates", "starlight$offline"))
cp = out_dir + ":" + ":".join(jars)
bad = 0
for cls, meth, handler in checks:
    res = subprocess.run(["javap", "-c", "-p", "-cp", cp, cls], capture_output=True, text=True).stdout
    wired = any(re.search(r"\b" + re.escape(meth) + r"\(", part.split("\n", 1)[0]) and handler in part.split("\n", 1)[1]
                for part in re.split(r"\n  (?=\S)", res) if "\n" in part)
    exported = os.path.exists(os.path.join(out_dir, cls.replace(".", "/") + ".class"))
    if not wired: bad += 1
    print(f"{'WIRED' if wired else 'NOT WIRED' + ('' if exported else ' (class not exported)')}  {cls.split('.')[-1]}.{meth} -> {handler}")
print(f"mixin audit {mc}: {len(checks) - bad}/{len(checks)} wired")
sys.exit(1 if bad else 0)
