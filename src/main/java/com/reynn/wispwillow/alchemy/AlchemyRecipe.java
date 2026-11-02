package com.reynn.wispwillow.alchemy;

import com.reynn.wispwillow.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One brew.
 *
 * Solids are order-independent: the cauldron is a pot, not a crafting grid, so the recipe
 * matches as a multiset. Fluid input is optional and so is fluid output, which is what lets
 * one recipe type cover "grind a solid", "steep a solid in fluxia" and "condense a fluid
 * into another fluid" without needing three machines.
 */
public record AlchemyRecipe(
        List<SizedIngredient> ingredients,
        FluidStack fluidInput,
        int fluxia,
        int ticks,
        ItemStack result,
        FluidStack fluidResult
) implements Recipe<AlchemyInput> {

    public static final int MAX_INGREDIENTS = 4;

    @Override
    public boolean matches(AlchemyInput input, Level level) {
        if (!fluidInput.isEmpty()) {
            if (!FluidStack.isSameFluidSameComponents(input.fluid(), fluidInput)) return false;
            if (input.fluid().getAmount() < fluidInput.getAmount()) return false;
        }
        return planConsumption(input.items()) != null;
    }

    /**
     * Works out how much to take from each slot, or null if some ingredient cannot be met.
     *
     * <p>An ingredient may draw across several slots. Requiring all N of an ingredient to sit
     * in one stack was the single most annoying thing about this pot: split four amethyst
     * across two slots — which is what a hopper or a shift-click will do to you — and a
     * recipe that visibly had everything it asked for simply refused to start, with nothing
     * on screen to explain why. It is a cauldron. It should not care how the reagents are
     * stacked, any more than it cares what order they went in.</p>
     *
     * <p>Ingredients are still tried most-restrictive-first. Without that ordering a broad
     * tag ingredient happily drains the slot a specific ingredient needed.</p>
     *
     * @return per-slot amounts to remove, indexed by slot; null if the recipe is not satisfied
     */
    private int @Nullable [] planConsumption(IItemHandler items) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) order.add(i);
        order.sort(Comparator.comparingInt(a ->
                ingredients.get(a).ingredient().getItems().length));

        int[] take = new int[items.getSlots()];

        for (int idx : order) {
            SizedIngredient need = ingredients.get(idx);
            int remaining = need.count();

            for (int slot = 0; slot < items.getSlots() && remaining > 0; slot++) {
                ItemStack have = items.getStackInSlot(slot);
                if (have.isEmpty()) continue;
                // Whatever an earlier ingredient already claimed here is spoken for.
                int spare = have.getCount() - take[slot];
                if (spare <= 0) continue;
                if (!need.ingredient().test(have)) continue;

                int used = Math.min(spare, remaining);
                take[slot] += used;
                remaining -= used;
            }

            if (remaining > 0) return null;
        }
        return take;
    }

    /** Removes exactly what this recipe asked for. Only valid after a successful match. */
    public void consume(ItemStackHandler items, FluidTank tank) {
        int[] take = planConsumption(items);
        if (take == null) return;
        for (int slot = 0; slot < take.length; slot++) {
            if (take[slot] > 0) items.extractItem(slot, take[slot], false);
        }
        if (!fluidInput.isEmpty()) {
            tank.drain(fluidInput.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * How demanding this brew is, for breaking ties when more than one matches.
     *
     * <p>Matching is subset-based — a recipe fires if the pot contains at least what it asks
     * for — so a pot holding the reagents for a big brew necessarily also satisfies any
     * smaller brew drawing on the same items. Left to {@code getRecipeFor}, which returns
     * whichever it happened to find first, that meant loading a pot for one recipe and
     * watching it produce a different, cheaper one: the reagents were right, the answer was
     * arbitrary, and nothing on screen explained it.</p>
     *
     * <p>Ranking by distinct ingredients first, then by whether a fluid was called for, then
     * by raw count, encodes the obvious intent: if you went to the trouble of adding a fourth
     * reagent and a tank of fluxia, you meant the recipe that wanted them.</p>
     */
    public int specificity() {
        int total = 0;
        for (SizedIngredient ing : ingredients) total += ing.count();
        return ingredients.size() * 1_000 + (fluidInput.isEmpty() ? 0 : 500) + total;
    }

    @Override public ItemStack assemble(AlchemyInput in, HolderLookup.Provider reg) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider reg) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return RecipeRegistry.ALCHEMY_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return RecipeRegistry.ALCHEMY_TYPE.get(); }
}
