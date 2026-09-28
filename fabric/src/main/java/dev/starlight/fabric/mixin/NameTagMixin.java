package dev.starlight.fabric.mixin;

import dev.starlight.core.Starlight;
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

/** Tier Tagger: the tier after a player's name on nametags (Starlight.nameSuffix). */
@Mixin(EntityRenderer.class)
public abstract class NameTagMixin {
    //? if >=1.21.2 {
    /** 1.21.2+: every nametag (players via AvatarRenderer/PlayerRenderer → super) comes from getNameTag. */
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void starlight$decorate(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null || !(entity instanceof Player)) return;
        String suffix = Starlight.nameSuffix(entity.getUUID(), false);
        if (suffix != null) cir.setReturnValue(name.copy().append(Component.literal(suffix)));
    }
    //?} else {
    /*@ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component starlight$decorate(Component name, @Local(argsOnly = true) Entity entity) {
        if (name == null || !(entity instanceof Player)) return name;
        String suffix = Starlight.nameSuffix(entity.getUUID(), false);
        return suffix == null ? name : name.copy().append(Component.literal(suffix));
    }
    *///?}
}
