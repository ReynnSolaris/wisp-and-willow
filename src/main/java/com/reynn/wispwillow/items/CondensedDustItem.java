package com.reynn.wispwillow.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

public class CondensedDustItem extends Item {

    public CondensedDustItem(Properties props) {
        super(props.durability(64).rarity(Rarity.EPIC)); // set max uses here
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true; // always returns itself (damaged) when used in crafting
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }


    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);

        copy.setDamageValue(copy.getDamageValue() + 1);

        // break when fully damaged
        if (copy.getDamageValue() >= copy.getMaxDamage()) {
            return ItemStack.EMPTY;
        }

        return copy;
    }
    // Optional: prevent it being “repairable” in an anvil if you want
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
