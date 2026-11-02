package com.reynn.wispwillow.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What the cauldron is holding right now: its four solid slots plus whatever is in the
 * input tank. Wrapping the live handler rather than copying it keeps the per-tick recheck
 * cheap; nothing here mutates.
 */
public record AlchemyInput(IItemHandler items, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.getStackInSlot(index);
    }

    @Override
    public int size() {
        return items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        if (!fluid.isEmpty()) return false;
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }
}
