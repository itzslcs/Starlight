package dev.starlight.fabric;

import dev.starlight.api.render.ItemRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Reusable ItemRef view (one per slot, re-pointed each call; no allocation per frame). */
public final class FabricItem implements ItemRef {
    ItemStack stack = ItemStack.EMPTY;

    FabricItem set(ItemStack s) {
        this.stack = s == null ? ItemStack.EMPTY : s;
        return this;
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public String id() {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    @Override
    public int count() {
        return stack.getCount();
    }

    @Override
    public int damage() {
        return stack.isDamageableItem() ? stack.getDamageValue() : 0;
    }

    @Override
    public int maxDamage() {
        return stack.isDamageableItem() ? stack.getMaxDamage() : 0;
    }

    @Override
    public String displayName() {
        return stack.getHoverName().getString();
    }
}
