package dev.starlight.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Smoke self-test: reads the lines a sign editor would send back. */
@Mixin(AbstractSignEditScreen.class)
public interface SignEditScreenAccessor {
    @Accessor("messages")
    String[] starlight$messages();
}
