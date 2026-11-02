package com.reynn.wispwillow.infusion;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every Fluxia Infusion, in one table.
 *
 * <p>Same arrangement as {@link com.reynn.wispwillow.ritual.RitualDefinitions}: the recipe
 * provider, the grimoire and the JEI category all read this, so they cannot disagree.</p>
 */
public final class InfusionDefinitions {
    private InfusionDefinitions() {}

    /**
     * @param path      recipe path under {@code wispwillow:infusion/}
     * @param inputs    what must be floating in the pool, exactly
     * @param result    what comes back out
     * @param fluxia    charge drawn from the pool
     * @param lore      flavour for the grimoire
     */
    public record Def(String path, List<ItemStack> inputs, ItemStack result, int fluxia, String lore) {
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "infusion/" + path);
        }
    }

    private static final List<Def> ALL = build();

    public static List<Def> all() { return ALL; }

    private static Def infusion(String path, ItemLike result, int count, int fluxia, String lore,
                                ItemStack... inputs) {
        return new Def(path, List.of(inputs), new ItemStack(result, count), fluxia, lore);
    }

    private static ItemStack of(ItemLike item) { return new ItemStack(item); }

    private static ItemStack of(ItemLike item, int count) { return new ItemStack(item, count); }

    private static List<Def> build() {
        List<Def> out = new ArrayList<>();

        // ---- Foundation runes -------------------------------------------------
        out.add(infusion("blank_rune", BlockRegistry.RUNES.get(RuneType.BLANK).get(), 1, 1000,
                "Fluxite is stone that remembers. Hold it under a pool's current long enough "
                        + "and the memory sets hard enough to build on.",
                of(BlockRegistry.FLUXITE.get()), of(ItemRegistry.ASTRALITE_NUGGET.get())));

        out.add(infusion("water_rune", BlockRegistry.RUNES.get(RuneType.WATER).get(), 1, 2500,
                "A rune that widens rather than deepens. Each one laid into a foundation "
                        + "gives the altar above it more room to hold.",
                of(BlockRegistry.RUNES.get(RuneType.BLANK).get()), of(Items.PRISMARINE_CRYSTALS)));

        out.add(infusion("earth_rune", BlockRegistry.RUNES.get(RuneType.EARTH).get(), 1, 2500,
                "Patient stone that takes part of a rite's burden onto itself, so the altar "
                        + "spends less to do the same work.",
                of(BlockRegistry.RUNES.get(RuneType.BLANK).get()), of(Items.EMERALD)));

        out.add(infusion("blood_rune", BlockRegistry.RUNES.get(RuneType.BLOOD).get(), 1, 4000,
                "The greediest of the four. It asks nothing of a working and occasionally "
                        + "takes a second one anyway.",
                of(BlockRegistry.RUNES.get(RuneType.BLANK).get()), of(Items.GHAST_TEAR)));

        // ---- Tablets ----------------------------------------------------------
        out.add(infusion("greater_tablet", ItemRegistry.GREATER_TABLET.get(), 1, 12000,
                "A tablet folded through itself. Five times the reserve, and enough throughput "
                        + "to prime a grand circle in one trip.",
                of(ItemRegistry.FLUXIA_TABLET.get()), of(ItemRegistry.ASTRALITE_INGOT.get(), 4),
                of(Items.DIAMOND_BLOCK)));

        out.add(infusion("grand_tablet", ItemRegistry.GRAND_TABLET.get(), 1, 40000,
                "The largest vessel this tradition knows how to make. Beyond this you are "
                        + "expected to build pools.",
                of(ItemRegistry.GREATER_TABLET.get()), of(Items.NETHER_STAR),
                of(BlockRegistry.ASTRALITE_BLOCK.get())));

        // ---- Functional flora seeds ------------------------------------------
        out.add(infusion("verdant_bloom", BlockRegistry.VERDANT_BLOOM.get(), 1, 6000,
                "A willow cutting taught to be impatient on behalf of everything around it.",
                of(BlockRegistry.WILLOW_SAPLING.get()), of(Items.BONE_BLOCK), of(Items.MOSS_BLOCK)));

        out.add(infusion("gathering_bloom", BlockRegistry.GATHERING_BLOOM.get(), 1, 6000,
                "Petals that close around anything loose that drifts within reach.",
                of(BlockRegistry.WILLOW_SAPLING.get()), of(Items.HOPPER), of(Items.ENDER_PEARL)));

        out.add(infusion("warding_bloom", BlockRegistry.WARDING_BLOOM.get(), 1, 8000,
                "Its scent is not unpleasant, exactly. Things with bad intentions simply "
                        + "find somewhere else to be.",
                of(BlockRegistry.WILLOW_SAPLING.get()), of(Items.GOLDEN_APPLE), of(Items.WITHER_ROSE)));

        // ---- Soul pacts -------------------------------------------------------
        // Two trips to the pool: one to charge the pact so it can take a soul, one to seal
        // the soul in once it has. The captured creature rides across the second infusion
        // on the item's component — see SoulPactItem#carrySoul.
        out.add(infusion("primed_soul_pact", ItemRegistry.PRIMED_SOUL_PACT.get(), 1, 5000,
                "An empty pact is a promise with nobody to make it to. Held under the current "
                        + "it starts to hum, and something on the other side starts listening.",
                of(ItemRegistry.SOUL_PACT.get()), of(Items.GHAST_TEAR)));

        out.add(infusion("sealed_soul_pact", ItemRegistry.SEALED_SOUL_PACT.get(), 1, 25000,
                "A soul in an unsealed pact is a guest, and guests leave. Sealing costs more "
                        + "than the taking did, which is the correct order of things.",
                of(ItemRegistry.BOUND_SOUL_PACT.get()), of(Items.ECHO_SHARD),
                of(ItemRegistry.ASTRALITE_INGOT.get(), 2)));

        return Collections.unmodifiableList(out);
    }
}
