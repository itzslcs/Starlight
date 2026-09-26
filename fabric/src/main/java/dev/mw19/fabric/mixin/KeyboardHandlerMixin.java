package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import dev.mw19.fabric.FabricCompat;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//? if >=1.21.9
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    //? if >=1.21.9 {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void mw19$key(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;
        if (Mw19.onKey(FabricCompat.key(event.key()), FabricCompat.action(action), FabricCompat.mods(event.modifiers()))) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void mw19$key(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().getWindow()) return;
        if (Mw19.onKey(key, action, modifiers)) ci.cancel();
    }
    *///?}
}
