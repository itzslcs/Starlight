package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mw19$button(CallbackInfo ci) {
        if (!Mw19.wantMenuButton()) return;
        addRenderableWidget(Button.builder(Component.literal("MW"), b -> Mw19.get().openGui())
                .bounds(6, 6, 20, 20).tooltip(Tooltip.create(Component.literal(Mw19.NAME + " menu"))).build());
    }
}
