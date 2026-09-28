package dev.starlight.forge;

import dev.starlight.api.render.ItemRef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Reusable ItemRef view over a (possibly null) 1.8.9 ItemStack. */
public final class ForgeItem implements ItemRef {
    ItemStack stack;

    ForgeItem set(ItemStack s) {
        this.stack = s;
        return this;
    }

    @Override
    public boolean isEmpty() {
        return stack == null || stack.stackSize <= 0 || stack.getItem() == null;
    }

    @Override
    public String id() {
        if (isEmpty()) return "minecraft:air";
        Object key = Item.itemRegistry.getNameForObject(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }

    @Override
    public int count() {
        return isEmpty() ? 0 : stack.stackSize;
    }

    @Override
    public int damage() {
        return !isEmpty() && stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
    }

    @Override
    public int maxDamage() {
        return !isEmpty() && stack.isItemStackDamageable() ? stack.getMaxDamage() : 0;
    }

    @Override
    public String displayName() {
        return isEmpty() ? "" : stack.getDisplayName();
    }
}
