package dev.mw19.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Hitboxes module, 1.21.9+: while it is on, vanilla's debug entry list reports entity hitboxes as enabled. Setting the
 * entry itself would be saved in the game's debug profile and outlive the module (debug-log 2026-09-27).
 */
//? if >=1.21.11 {
@Mixin(net.minecraft.client.gui.components.debug.DebugScreenEntryList.class)
public abstract class HitboxesMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "isCurrentlyEnabled", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void mw19$hitboxes(net.minecraft.resources.Identifier id, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (dev.mw19.fabric.Mw19Fabric.hitboxes && net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES.equals(id)) cir.setReturnValue(true);
    }
}
//?} elif >=1.21.9 {
/*@Mixin(net.minecraft.client.gui.components.debug.DebugScreenEntryList.class)
public abstract class HitboxesMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "isCurrentlyEnabled", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void mw19$hitboxes(net.minecraft.resources.ResourceLocation id, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (dev.mw19.fabric.Mw19Fabric.hitboxes && net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES.equals(id)) cir.setReturnValue(true);
    }
}
*///?} else {
/*@Mixin(net.minecraft.client.Minecraft.class)
public abstract class HitboxesMixin {}
*///?}
