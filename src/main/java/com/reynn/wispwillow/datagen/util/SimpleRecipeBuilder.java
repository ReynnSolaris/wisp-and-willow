package com.reynn.wispwillow.datagen.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SimpleRecipeBuilder implements RecipeBuilder {
    protected final ItemStack resultIcon;
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    protected String group;

    protected SimpleRecipeBuilder(ItemStack resultIcon) { this.resultIcon = resultIcon; }

    @Override
    public SimpleRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        criteria.put(name, criterion);
        return this;
    }

    @Override
    public SimpleRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public Item getResult() {
        return resultIcon.getItem();
    }

    protected Advancement.Builder buildAdvancement(RecipeOutput out, ResourceLocation id) {
        Advancement.Builder adv = out.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(adv::addCriterion);
        return adv;
    }
}

