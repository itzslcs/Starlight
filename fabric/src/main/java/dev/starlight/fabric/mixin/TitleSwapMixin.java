package dev.starlight.fabric.mixin;

import dev.starlight.fabric.StarlightFabric;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Home screen: vanilla's TitleScreen is swapped for Starlight's as it is set (Minecraft.setScreen until 26.1, Gui.setScreen from 26.2). */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?} else {
@Mixin(net.minecraft.client.Minecraft.class)
//?}
public abstract class TitleSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen starlight$home(Screen screen) {
        return StarlightFabric.home(screen);
    }
}
