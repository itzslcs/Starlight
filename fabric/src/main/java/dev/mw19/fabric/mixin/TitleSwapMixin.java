package dev.mw19.fabric.mixin;

import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Home screen: vanilla's TitleScreen is swapped for MW19's as it is set (Minecraft.setScreen until 26.1, Gui.setScreen from 26.2). */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?} else {
@Mixin(net.minecraft.client.Minecraft.class)
//?}
public abstract class TitleSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen mw19$home(Screen screen) {
        return Mw19Fabric.home(screen);
    }
}
