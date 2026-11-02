package com.reynn.wispwillow.ritual;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.datagen.util.SimpleRecipeBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RitualRecipeBuilder extends SimpleRecipeBuilder {
    private final RitualRecipe.Grid grid;
    private final List<RitualRecipe.Cell> pattern = new ArrayList<>();
    private final List<Ingredient> inputs = new ArrayList<>();
    private Optional<ItemStack> resultItem = Optional.empty();
    private Optional<ResourceLocation> resultMob = Optional.empty();
    private Optional<Integer> fluxiaRequired = Optional.empty();
    private Optional<Vigil> vigil = Optional.empty();
    private Optional<TagKey<Item>> awakens = Optional.empty();
    private Optional<Imbuement> imbues = Optional.empty();
    private int resultMobCount = 1;
    private int minTier = 1;

    public RitualRecipeBuilder(RitualRecipe.Grid grid, ItemStack advancementIcon) {
        super(advancementIcon);
        this.grid = grid;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, path);
    }

    public RitualRecipeBuilder anySigil(int dx, int dz, boolean required) {
        pattern.add(new RitualRecipe.Cell(dx, dz, Optional.empty(), Optional.empty(), required));
        return this;
    }
    public RitualRecipeBuilder cell(int dx, int dz, SigilType typeOrNull, DyeColor colorOrNull, boolean required) {
        pattern.add(new RitualRecipe.Cell(dx, dz,
                Optional.ofNullable(typeOrNull),
                Optional.ofNullable(colorOrNull),
                required));
        return this;
    }

    public RitualRecipeBuilder input(Ingredient ing) { inputs.add(ing); return this; }

    public RitualRecipeBuilder fluxiaRequired(int required) { this.fluxiaRequired = Optional.of(required); return this; }

    public RitualRecipeBuilder minTier(int tier) { this.minTier = Math.max(1, tier); return this; }

    public RitualRecipeBuilder vigil(Vigil v) { this.vigil = Optional.of(v); return this; }

    /** Makes this rite embue the tagged items already lying in the circle. */
    public RitualRecipeBuilder awakens(TagKey<Item> tag) { this.awakens = Optional.of(tag); return this; }

    /** Makes this rite imbue the awakened pieces already lying in the circle. */
    public RitualRecipeBuilder imbues(Imbuement imbuement) { this.imbues = Optional.of(imbuement); return this; }

    public RitualRecipeBuilder resultItem(ItemStack stack) {
        this.resultItem = Optional.of(stack.copy());
        this.resultMob = Optional.empty();
        return this;
    }
    public RitualRecipeBuilder resultItem(Item item, int count) {
        return resultItem(new ItemStack(item, count));
    }
    public RitualRecipeBuilder resultMob(ResourceLocation entityId, int count) {
        this.resultMob = Optional.of(entityId);
        this.resultMobCount = Math.max(1, count);
        this.resultItem = Optional.empty();
        return this;
    }

    @Override
    public void save(RecipeOutput out, ResourceLocation id) {
        Advancement.Builder adv = buildAdvancement(out, id);
        RitualRecipe recipe = new RitualRecipe(
                id,
                grid,
                List.copyOf(pattern),
                List.copyOf(inputs),
                resultItem,
                resultMob,
                resultMobCount,
                fluxiaRequired,
                minTier,
                vigil,
                awakens,
                imbues
        );
        out.accept(id, recipe, adv.build(id.withPrefix("recipes/")));
    }
}