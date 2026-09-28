package dev.starlight.fabric.mixin;

import dev.starlight.fabric.ExploitGuard;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exploit Protection: the lines a sign editor sends back are resolved as an unmodded client would (ExploitGuard). */
@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditMixin {
    @Shadow
    @Final
    private String[] messages;

    //? if >=26.3 {
    /*@Shadow
    @Final
    private net.minecraft.world.level.block.entity.SignText.Mutable text;

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V", at = @At("RETURN"))
    private void starlight$guard(SignBlockEntity sign, net.minecraft.world.level.block.entity.SignTextSlot slot, boolean filtered, Component title, CallbackInfo ci) {
        ExploitGuard.signLines(messages, sign.getText(slot).getMessages(filtered));
        for (int i = 0; i < messages.length; i++) text.setLine(i, Component.literal(messages[i]));
    }
    *///?} else {
    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V", at = @At("RETURN"))
    private void starlight$guard(SignBlockEntity sign, boolean front, boolean filtered, Component title, CallbackInfo ci) {
        ExploitGuard.signLines(messages, sign.getText(front).getMessages(filtered));
    }
    //?}
}
