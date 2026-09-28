package dev.starlight.fabric.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the home screen and Skins page turn vanilla's player model (rotationX is pitch, rotationY yaw). */
@Mixin(PlayerSkinWidget.class)
public interface PlayerSkinWidgetAccessor {
    @Accessor("rotationX")
    void starlight$setRotationX(float v);

    @Accessor("rotationY")
    void starlight$setRotationY(float v);
}
