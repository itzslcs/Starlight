package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void kestrel$button(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle()) Kestrel.onMouseButton(info.button(), action);
    }
}
