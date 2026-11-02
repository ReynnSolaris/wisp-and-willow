package com.reynn.wispwillow.alchemy;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.FluidRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Every cauldron brew, in the order the player is meant to meet them.
 *
 * <p>Held as a table rather than emitted straight into the recipe builder, so the Patchouli
 * pages and the recipes come from one place. {@link com.reynn.wispwillow.infusion.InfusionDefinitions}
 * is laid out the same way for the same reason: a book that describes a brew the recipe no
 * longer makes is worse than no book at all.</p>
 *
 * <p>The gate on the whole family is the multiblock itself: fluxite circle, willow pillars,
 * astralite caps, and 2,000 fluxia to light it. That already sits after the dynamo and the
 * mana pool, so nothing here needs its own unlock beyond "you have the ingredient".</p>
 *
 * <p>The design rule for the tiers: the cauldron never unlocks an item you could not already
 * get. It makes routes you already have cheaper, or turns solids into fluids and back. That
 * keeps it optional rather than mandatory, which is what stops a multiblock this expensive
 * from feeling like a tax on progression.</p>
 */
public final class AlchemyDefinitions {

    private AlchemyDefinitions() {}

    /**
     * One brew.
     *
     * @param inputs solids, where a stack of N means "N of this item in one slot"
     * @param unlock the item whose possession unlocks the recipe
     * @param lore   the book's blurb; why you would want this brew
     */
    public record Def(String path,
                      List<ItemStack> inputs,
                      FluidStack fluidInput,
                      int fluxia,
                      int ticks,
                      ItemStack result,
                      FluidStack fluidResult,
                      ItemLike unlock,
                      String tier,
                      String lore) {}

    /** Bucket-equivalents of the mod's own fluid — the still source, not the flowing one. */
    private static FluidStack fluxiaFluid(int mb) {
        return new FluidStack(FluidRegistry.FLUXIA_FLUID.get(), mb);
    }

    private static final List<Def> ALL = List.of(

            // ------------------------------------------------------------ tier 1: steeping
            // No fluxia beyond the pot's own idle draw. These exist so a freshly lit cauldron
            // does something useful immediately instead of waiting on a later material.

            new Def("amethyst_paper",
                    List.of(new ItemStack(BlockRegistry.AMETHYST_SUGAR_CANE.get(), 2)),
                    fluxiaFluid(250), 0, 80,
                    new ItemStack(ItemRegistry.AMETHYST_PAPER.get(), 4),
                    FluidStack.EMPTY,
                    BlockRegistry.AMETHYST_SUGAR_CANE.get(),
                    "Steeping",
                    "Wet-pressing beats dry-pressing. Three cane make three paper at a bench; "
                            + "two make four here. That is a straight discount on the Soul Pact chain, "
                            + "which is the longest grind in this book."),

            new Def("converter_dust",
                    List.of(new ItemStack(Items.AMETHYST_SHARD, 4),
                            new ItemStack(ItemRegistry.AMETHYST_COAL.get(), 1)),
                    FluidStack.EMPTY, 600, 120,
                    new ItemStack(ItemRegistry.DUSTCONVERT.get(), 2),
                    FluidStack.EMPTY,
                    ItemRegistry.AMETHYST_COAL.get(),
                    "Steeping",
                    "A sink for the amethyst you are drowning in by the time you can build this. "
                            + "Coal to carry the heat, shards to carry the charge."),

            // ------------------------------------------------------------ tier 2: leaching
            // Ore doubling. The classic reason to build a machine, and the point where the
            // 2,000 fluxia you spent lighting the thing starts paying itself back.

            new Def("astralite_leaching",
                    List.of(new ItemStack(ItemRegistry.RAW_ASTRALITE.get(), 1)),
                    fluxiaFluid(500), 1_200, 160,
                    new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 2),
                    FluidStack.EMPTY,
                    ItemRegistry.RAW_ASTRALITE.get(),
                    "Leaching",
                    "Ore doubling, and the reason most people build the cauldron at all. "
                            + "The lighting cost pays itself back inside a stack of raw astralite."),

            new Def("nugget_reclaim",
                    List.of(new ItemStack(ItemRegistry.ASTRALITE_NUGGET.get(), 4)),
                    fluxiaFluid(200), 400, 100,
                    new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.ASTRALITE_NUGGET.get(),
                    "Leaching",
                    "Four nuggets back into an ingot instead of nine. Scrap you would otherwise "
                            + "have thrown away is worth carrying home."),

            // ------------------------------------------------------------ tier 3: condensing
            // Solid to fluid and back. This is the tier that makes the fluid tanks worth
            // plumbing, and the reason the cauldron exposes a fluid capability at all.

            new Def("condensed_dust",
                    List.of(new ItemStack(ItemRegistry.DUSTCONVERT.get(), 2)),
                    fluxiaFluid(1_000), 3_000, 240,
                    new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.DUSTCONVERT.get(),
                    "Condensing",
                    "Dust packed down under liquid fluxia until it holds together. Fill a bucket "
                            + "straight from a Willow Fluxia Pool and pour it in — the cauldron no "
                            + "longer has to make its own reagent before it can start."),

            new Def("dust_to_fluid",
                    List.of(new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1)),
                    FluidStack.EMPTY, 2_000, 200,
                    ItemStack.EMPTY,
                    fluxiaFluid(1_000),
                    ItemRegistry.CONDENSED_DUST.get(),
                    "Condensing",
                    "And back the other way: condensed dust let go until it runs. Not the cheapest "
                            + "way to fill a bucket — a Willow Fluxia Pool will do that for nothing — "
                            + "but it is the only one that works with no pool in reach, and it is the "
                            + "reason the decanting tank and its pour-back arrow exist."),

            // ------------------------------------------------------------ tier 4: transmutation
            // Endgame. Expensive enough that it is a project, not a habit.

            new Def("astralite_transmutation",
                    List.of(new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1),
                            new ItemStack(BlockRegistry.FLUXITE.get().asItem(), 4)),
                    fluxiaFluid(2_000), 8_000, 400,
                    new ItemStack(BlockRegistry.ASTRALITE_BLOCK.get().asItem(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.CONDENSED_DUST.get(),
                    "Transmutation",
                    "Fluxite pushed the rest of the way into astralite. Slower and dearer than "
                            + "mining it, and it does not care whether there is any left to mine."),

            // ------------------------------------------------------------ throughput
            // The pot's own upgrades. Deliberately made in the pot: the first thing you want
            // once the cauldron is the bottleneck is a faster cauldron, and having to run the
            // slow one to build the fast one is the whole shape of that.

            new Def("upgrade_haste",
                    List.of(new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 2),
                            new ItemStack(ItemRegistry.AMETHYST_COAL.get(), 1)),
                    fluxiaFluid(500), 1_500, 200,
                    new ItemStack(ItemRegistry.UPGRADE_HASTE.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.AMETHYST_COAL.get(),
                    "Throughput",
                    "Coal to drive the fire harder. Each one cuts a fifth off every brew, "
                            + "compounding, so the second is worth less than the first and no "
                            + "number of them will ever make a brew instant."),

            new Def("upgrade_thrift",
                    List.of(new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 2),
                            new ItemStack(ItemRegistry.DUSTCONVERT.get(), 2)),
                    fluxiaFluid(500), 1_500, 200,
                    new ItemStack(ItemRegistry.UPGRADE_THRIFT.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.DUSTCONVERT.get(),
                    "Throughput",
                    "Converter dust laid into the lining, so less of the charge escapes as "
                            + "light. Takes a fifth off what a brew costs, compounding, and never "
                            + "quite reaches free."),

            new Def("upgrade_capacity",
                    List.of(new ItemStack(BlockRegistry.FLUXITE.get().asItem(), 4),
                            new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1)),
                    FluidStack.EMPTY, 2_000, 220,
                    new ItemStack(ItemRegistry.UPGRADE_CAPACITY.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.CONDENSED_DUST.get(),
                    "Throughput",
                    "Eight more buckets in each tank. Worth fitting the moment you are piping "
                            + "fluid in faster than a brew drinks it."),

            new Def("wisp_division",
                    List.of(new ItemStack(ItemRegistry.SOUL_WISP.get(), 1),
                            new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 4)),
                    fluxiaFluid(4_000), 12_000, 600,
                    new ItemStack(ItemRegistry.SOUL_WISP.get(), 2),
                    FluidStack.EMPTY,
                    ItemRegistry.SOUL_WISP.get(),
                    "Transmutation",
                    "One wisp coaxed into two. Familiars are the mod's long tail, and this is the "
                            + "only place the wisp supply is not strictly gated by the gateway. "
                            + "Its price is fluxia and time, not another material chain: four "
                            + "buckets of the liquid and twelve thousand charge is quite enough to "
                            + "make you think about it, without a dust ladder in front of it."),

            // ------------------------------------------------------------ tier 5: ascension
            // The only tier the mod adds, and the only thing in this table that is not a
            // cheaper route to something you already had.

            new Def("empyrite_ingot",
                    List.of(new ItemStack(ItemRegistry.ASTRALITE_INGOT.get(), 4),
                            new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1),
                            new ItemStack(ItemRegistry.AMETHYST_COAL.get(), 2)),
                    fluxiaFluid(3_000), 16_000, 800,
                    new ItemStack(ItemRegistry.EMPYRITE_INGOT.get(), 1),
                    FluidStack.EMPTY,
                    ItemRegistry.CONDENSED_DUST.get(),
                    "Ascension",
                    "Astralite taken past what the star-metal will hold on its own. There is no "
                            + "ore for this and no furnace that will make it — only the pot."
                            + " Note the amethyst coal: the rite that makes it is the one piece of "
                            + "altar work the cauldron cannot do without, so the two halves of the "
                            + "mod meet here."),

            new Def("fluxite_synthesis",
                    List.of(new ItemStack(ItemRegistry.CONDENSED_DUST.get(), 1),
                            new ItemStack(Items.AMETHYST_SHARD, 4)),
                    fluxiaFluid(1_000), 2_400, 240,
                    new ItemStack(BlockRegistry.FLUXITE.get().asItem(), 4),
                    FluidStack.EMPTY,
                    ItemRegistry.CONDENSED_DUST.get(),
                    "Condensing",
                    "Fluxite grown rather than dug. Slower than a pickaxe and far dearer, and "
                            + "the only thing in the mod that makes the stone renewable — which "
                            + "matters once the circle under every cauldron wants twelve of it.")
    );

    public static List<Def> all() {
        return ALL;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "alchemy/" + path);
    }

    public static void build(RecipeOutput out, Function<ItemLike, Criterion<?>> has) {
        for (Def def : ALL) {
            AlchemyRecipeBuilder b = AlchemyRecipeBuilder.brew();
            for (ItemStack in : def.inputs()) {
                b.input(in.getItem(), in.getCount());
            }
            if (!def.fluidInput().isEmpty()) b.fluid(def.fluidInput());
            b.fluxia(def.fluxia()).ticks(def.ticks());
            if (!def.result().isEmpty()) b.result(def.result().copy());
            if (!def.fluidResult().isEmpty()) b.fluidResult(def.fluidResult());
            b.unlockedBy("has_" + itemPath(def.unlock()), has.apply(def.unlock()));
            b.save(out, id(def.path()));
        }
    }

    private static String itemPath(ItemLike item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    /** Distinct tier names, in table order, for the book's summary. */
    public static List<String> tiers() {
        List<String> out = new ArrayList<>();
        for (Def def : ALL) {
            if (!out.contains(def.tier())) out.add(def.tier());
        }
        return out;
    }
}
