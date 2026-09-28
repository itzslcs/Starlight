package dev.starlight.forge.mixin;

import dev.starlight.forge.StarlightForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Starlight game menus" on 1.8.9: vanilla buttons become Starlight keycaps (MenuStyle). The slider knob (drawn by mouseDragged with
 * the button texture bound) and custom label colours keep working; buttons with their own drawing keep it.
 */
@Mixin(GuiButton.class)
public abstract class GuiButtonMixin {
    @Shadow @Final protected static ResourceLocation buttonTextures;
    @Shadow public int width, height, xPosition, yPosition, packedFGColour;
    @Shadow public String displayString;
    @Shadow public boolean enabled, visible;
    @Shadow protected boolean hovered;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void starlight$style(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        if (!visible) return;
        boolean hot = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        if (!StarlightForge.button(xPosition, yPosition, width, height, hot, enabled)) return;
        hovered = hot;
        mc.getTextureManager().bindTexture(buttonTextures);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mouseDragged(mc, mouseX, mouseY);
        int color = packedFGColour != 0 ? packedFGColour : (!enabled ? dev.starlight.core.gui.MenuStyle.LABEL_OFF
                : hot ? dev.starlight.core.gui.MenuStyle.LABEL_HOVER : dev.starlight.core.gui.MenuStyle.LABEL) & 0xFFFFFF;
        mc.fontRendererObj.drawStringWithShadow(displayString, xPosition + width / 2 - mc.fontRendererObj.getStringWidth(displayString) / 2,
                yPosition + (height - 8) / 2, color);
        ci.cancel();
    }
}
