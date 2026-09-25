package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void kestrel$key(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;
        if (Kestrel.onKey(event.key(), action, event.modifiers())) ci.cancel();
    }
}
