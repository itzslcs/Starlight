package dev.mw19.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mw19.fabric.ExploitGuard;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Exploit Protection: the anvil's name field (sent back when renaming) gets the item name as an unmodded client resolves it. */
@Mixin(AnvilScreen.class)
public abstract class AnvilNameMixin {
    @WrapOperation(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String mw19$guard(Component name, Operation<String> original) {
        return ExploitGuard.text(name);
    }
}
