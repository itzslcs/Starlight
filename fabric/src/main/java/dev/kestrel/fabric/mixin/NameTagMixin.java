package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

/** Plugin name decorations (e.g. Tier Tags) on player nametags. */
@Mixin(EntityRenderer.class)
public abstract class NameTagMixin {
    //? if >=1.21.2 {
    /** 1.21.2+: every nametag (players via AvatarRenderer/PlayerRenderer → super) comes from getNameTag. */
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void kestrel$decorate(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null || !(entity instanceof Player)) return;
        String suffix = Kestrel.nameSuffix(entity.getUUID(), entity.getName().getString(), false);
        if (suffix != null) cir.setReturnValue(name.copy().append(Component.literal(suffix)));
    }
    //?} else {
    /*@ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component kestrel$decorate(Component name, @Local(argsOnly = true) Entity entity) {
        if (name == null || !(entity instanceof Player)) return name;
        String suffix = Kestrel.nameSuffix(entity.getUUID(), entity.getName().getString(), false);
        return suffix == null ? name : name.copy().append(Component.literal(suffix));
    }
    *///?}
}
