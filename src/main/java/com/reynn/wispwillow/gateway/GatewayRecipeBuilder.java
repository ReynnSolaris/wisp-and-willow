package com.reynn.wispwillow.gateway;

import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.datagen.util.SimpleRecipeBuilder;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Datagen builder for one {@link GatewayRecipe}: a soul in, a creature and/or items out. */
public class GatewayRecipeBuilder extends SimpleRecipeBuilder {
    private final ResourceLocation soul;
    @Nullable private EntityType<?> summons;
    private int count = 1;
    private final List<ItemStack> results = new ArrayList<>();
    private int fluxia = GatewayCoreBlockEntity.CAST_COST;

    public GatewayRecipeBuilder(ResourceLocation soul) {
        // The icon the unlock advancement shows: the pact is what you spend.
        super(new ItemStack(ItemRegistry.SEALED_SOUL_PACT.get()));
        this.soul = soul;
    }

    public GatewayRecipeBuilder(ResourceLocation soul, EntityType<?> summons) {
        this(soul);
        this.summons = summons;
    }

    /** What steps out, and how many. */
    public GatewayRecipeBuilder summons(EntityType<?> type, int count) {
        this.summons = type;
        this.count = count;
        return this;
    }

    /** Something that falls out in front of the core as well as, or instead of, a creature. */
    public GatewayRecipeBuilder result(ItemStack stack) {
        this.results.add(stack);
        return this;
    }

    public GatewayRecipeBuilder fluxia(int amount) { this.fluxia = amount; return this; }

    @Override
    public void save(RecipeOutput out, ResourceLocation id) {
        if (summons == null && results.isEmpty()) {
            throw new IllegalStateException("Gateway casting for " + soul + " summons nothing and drops nothing");
        }
        Advancement.Builder adv = buildAdvancement(out, id);
        out.accept(id, new GatewayRecipe(id, soul, summons, count, results, fluxia), adv.build(id.withPrefix("recipes/")));
    }
}
