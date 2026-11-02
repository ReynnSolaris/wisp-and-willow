package com.reynn.wispwillow.alchemy;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mirrors InfusionRecipeBuilder so both recipe families read the same way in datagen. */
public class AlchemyRecipeBuilder implements RecipeBuilder {

    private final List<SizedIngredient> ingredients = new ArrayList<>();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private FluidStack fluidInput = FluidStack.EMPTY;
    private FluidStack fluidResult = FluidStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private int fluxia = 0;
    private int ticks = 100;

    public static AlchemyRecipeBuilder brew() {
        return new AlchemyRecipeBuilder();
    }

    public AlchemyRecipeBuilder input(ItemLike item) { return input(Ingredient.of(item), 1); }
    public AlchemyRecipeBuilder input(ItemLike item, int count) { return input(Ingredient.of(item), count); }

    public AlchemyRecipeBuilder input(Ingredient ingredient, int count) {
        if (ingredients.size() >= AlchemyRecipe.MAX_INGREDIENTS) {
            throw new IllegalStateException("The cauldron only has "
                    + AlchemyRecipe.MAX_INGREDIENTS + " solid slots");
        }
        ingredients.add(new SizedIngredient(ingredient, count));
        return this;
    }

    public AlchemyRecipeBuilder fluid(FluidStack stack) { this.fluidInput = stack; return this; }
    public AlchemyRecipeBuilder fluxia(int amount)      { this.fluxia = amount; return this; }
    public AlchemyRecipeBuilder ticks(int t)            { this.ticks = t; return this; }
    public AlchemyRecipeBuilder result(ItemLike item, int count) { this.result = new ItemStack(item, count); return this; }
    public AlchemyRecipeBuilder result(ItemStack stack) { this.result = stack; return this; }
    public AlchemyRecipeBuilder fluidResult(FluidStack stack) { this.fluidResult = stack; return this; }

    @Override
    public AlchemyRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        criteria.put(name, criterion);
        return this;
    }

    @Override
    public AlchemyRecipeBuilder group(@Nullable String group) { return this; }

    @Override
    public net.minecraft.world.item.Item getResult() { return result.getItem(); }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        if (ingredients.isEmpty() && fluidInput.isEmpty()) {
            throw new IllegalStateException("Alchemy recipe " + id + " has no inputs at all");
        }
        if (result.isEmpty() && fluidResult.isEmpty()) {
            throw new IllegalStateException("Alchemy recipe " + id + " produces nothing");
        }
        if (criteria.isEmpty()) {
            throw new IllegalStateException("No unlock criterion for " + id);
        }

        Advancement.Builder adv = output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(adv::addCriterion);

        output.accept(id,
                new AlchemyRecipe(List.copyOf(ingredients), fluidInput, fluxia, ticks, result, fluidResult),
                adv.build(id.withPrefix("recipes/")));
    }
}
