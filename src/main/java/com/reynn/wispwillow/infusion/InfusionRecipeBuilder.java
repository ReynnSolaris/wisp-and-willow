package com.reynn.wispwillow.infusion;

import com.reynn.wispwillow.datagen.util.SimpleRecipeBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class InfusionRecipeBuilder extends SimpleRecipeBuilder {
    private final List<Ingredient> inputs = new ArrayList<>();
    private final ItemStack result;
    private int fluxia;

    public InfusionRecipeBuilder(ItemStack result) {
        super(result.copy());
        this.result = result.copy();
    }

    public InfusionRecipeBuilder input(Ingredient ing) { inputs.add(ing); return this; }

    public InfusionRecipeBuilder fluxia(int amount) { this.fluxia = amount; return this; }

    @Override
    public void save(RecipeOutput out, ResourceLocation id) {
        Advancement.Builder adv = buildAdvancement(out, id);
        out.accept(id, new InfusionRecipe(id, List.copyOf(inputs), result, fluxia),
                adv.build(id.withPrefix("recipes/")));
    }
}
