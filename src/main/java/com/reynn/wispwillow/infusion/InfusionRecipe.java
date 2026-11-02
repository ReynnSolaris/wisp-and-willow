package com.reynn.wispwillow.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluxia Infusion: items thrown into a charged Willow Fluxia Pool transmute.
 *
 * <p>The counterpart to a ritual — where a rite is about geometry and scale, an infusion
 * is about pouring raw current into matter. It is how the foundation runes are made.</p>
 */
public class InfusionRecipe implements Recipe<RecipeInput> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "infusion");
    public static final RecipeType<InfusionRecipe> TYPE = RecipeType.simple(ID);

    public static final MapCodec<InfusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("id", ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "infusion/unnamed")).forGetter(InfusionRecipe::getId),
            Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(InfusionRecipe::inputs),
            ItemStack.CODEC.fieldOf("result").forGetter(InfusionRecipe::result),
            Codec.INT.fieldOf("fluxia").orElse(0).forGetter(InfusionRecipe::fluxia)
    ).apply(i, InfusionRecipe::new));

    private final ResourceLocation id;
    private final List<Ingredient> inputs;
    private final ItemStack result;
    private final int fluxia;

    public InfusionRecipe(ResourceLocation id, List<Ingredient> inputs, ItemStack result, int fluxia) {
        this.id = id;
        this.inputs = inputs;
        this.result = result;
        this.fluxia = fluxia;
    }

    public ResourceLocation getId() { return id; }
    public List<Ingredient> inputs() { return inputs; }
    public ItemStack result() { return result; }
    public int fluxia() { return fluxia; }

    /**
     * Do these floating stacks satisfy the recipe?
     *
     * <p>Every ingredient must be matched, and everything floating must be an ingredient —
     * so a pool holding stray junk will not silently eat it. Surplus of an item the recipe
     * does want is fine and is left behind, which makes a pool a batch converter: drop a
     * stack of fluxite and nuggets in and it turns out runes one a second.</p>
     */
    public boolean matches(List<ItemStack> floating) {
        return match(floating) != null;
    }

    /**
     * Works out how many items to take from each floating stack, or returns null if this
     * recipe does not apply.
     *
     * <p>A repeated ingredient means a quantity — four astralite entries want four ingots.
     * This used to compare {@code floating.size()} against {@code inputs.size()}, i.e. it
     * counted <em>item entities</em> rather than items, and dropped items merge: four
     * ingots thrown into a pool are one entity of four, so a recipe asking for four could
     * never match. Greater and Grand tablets were both unobtainable because of it.</p>
     *
     * @return counts to consume, parallel to {@code floating}, or null for no match
     */
    public int[] match(List<ItemStack> floating) {
        int[] taken = new int[floating.size()];

        for (Ingredient ingredient : inputs) {
            boolean satisfied = false;
            for (int i = 0; i < floating.size(); i++) {
                ItemStack stack = floating.get(i);
                if (taken[i] >= stack.getCount() || !ingredient.test(stack)) continue;
                taken[i]++;
                satisfied = true;
                break;
            }
            if (!satisfied) return null;
        }

        // Every floating stack has to be something the recipe actually wanted, but a surplus
        // of a wanted item is fine and gets left behind.
        //
        // This used to demand an exact count with nothing left over, which meant a recipe
        // asking for two astralite ingots failed silently against one, three, or a stack of
        // sixty-four — no smoke, no message, nothing to tell you what was wrong. Two-item
        // infusions were easy to get exactly right; the four-ingredient ones essentially
        // never fired. Junk in the pool still blocks the match, which is what the strictness
        // was for.
        for (int i = 0; i < floating.size(); i++) {
            if (taken[i] == 0) return null;
        }
        return taken;
    }

    // ---- Recipe plumbing; the pool drives this, not the vanilla crafting UI ----
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider regs) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider regs) { return result; }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return RecipeRegistry.INFUSION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }
}
