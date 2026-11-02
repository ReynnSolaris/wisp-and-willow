package com.reynn.wispwillow.worldgen.spirit;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.FeatureRegistry;
import com.reynn.wispwillow.worldgen.feature.BogPoolFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritFloorFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritRuinFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritSpireFeature;
import com.reynn.wispwillow.worldgen.tree.WillowTreeVineDecorator;
import java.util.List;
import com.reynn.wispwillow.worldgen.placement.SpacedFilter;
import com.reynn.wispwillow.worldgen.tree.SoulwoodFoliagePlacer;
import com.reynn.wispwillow.worldgen.tree.SoulwoodTrunkPlacer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * What grows on the islands, and what stands on them.
 *
 * <p>Everything here is the mod's own. That is deliberate rather than tidy: features shared between
 * biomes have to appear in the same relative order in every biome that uses them, and a mismatch
 * makes any world containing both unloadable with <em>"Feature order cycle found"</em>. Borrowing
 * nothing from vanilla — and nothing from the fen, which is why the pool below is its own
 * configured feature rather than the fen's — means the Spirit Realm cannot contribute to such a
 * cycle at all.</p>
 */
public final class SpiritFeatures {
    private SpiritFeatures() {}

    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_FLOOR = configured("spirit_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_FLOOR_SPARSE = configured("spirit_floor_sparse");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_GRASS = configured("spirit_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_POOL = configured("spirit_pool");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOULWOOD = configured("soulwood");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_FLOWERS = configured("spirit_flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_CAIRN = configured("spirit_cairn");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_HALL = configured("spirit_hall");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_SPIRE = configured("spirit_spire");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOULWOOD_ANCIENT = configured("soulwood_ancient");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WISPCAP_GLADE = configured("wispcap_glade");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_VEIL = configured("spirit_veil");

    public static final ResourceKey<PlacedFeature> SPIRIT_FLOOR_PLACED = placed("spirit_floor");
    public static final ResourceKey<PlacedFeature> SPIRIT_FLOOR_SPARSE_PLACED = placed("spirit_floor_sparse");
    public static final ResourceKey<PlacedFeature> SPIRIT_GRASS_PLACED = placed("spirit_grass");
    public static final ResourceKey<PlacedFeature> SPIRIT_POOL_PLACED = placed("spirit_pool");
    public static final ResourceKey<PlacedFeature> SOULWOOD_PLACED = placed("soulwood");
    public static final ResourceKey<PlacedFeature> SOULWOOD_DENSE_PLACED = placed("soulwood_dense");
    public static final ResourceKey<PlacedFeature> SPIRIT_FLOWERS_PLACED = placed("spirit_flowers");
    public static final ResourceKey<PlacedFeature> SPIRIT_CAIRN_PLACED = placed("spirit_cairn");
    public static final ResourceKey<PlacedFeature> SPIRIT_HALL_PLACED = placed("spirit_hall");
    public static final ResourceKey<PlacedFeature> SPIRIT_SPIRE_PLACED = placed("spirit_spire");
    public static final ResourceKey<PlacedFeature> SPIRIT_SPIRE_SPARSE_PLACED = placed("spirit_spire_sparse");
    public static final ResourceKey<PlacedFeature> SOULWOOD_ANCIENT_PLACED = placed("soulwood_ancient");
    public static final ResourceKey<PlacedFeature> WISPCAP_GLADE_PLACED = placed("wispcap_glade");
    public static final ResourceKey<PlacedFeature> SPIRIT_VEIL_PLACED = placed("spirit_veil");

    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Mostly grass, because the biome's grass override is what turns the islands ash-green;
        // moss and coarse dirt break up the sheet so it does not read as a lawn.
        FeatureUtils.register(context, SPIRIT_FLOOR, FeatureRegistry.SPIRIT_FLOOR.get(),
                new SpiritFloorFeature.Config(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.GRASS_BLOCK.defaultBlockState(), 7)
                                .add(Blocks.MOSS_BLOCK.defaultBlockState(), 2)
                                .add(Blocks.COARSE_DIRT.defaultBlockState(), 1)),
                        BlockStateProvider.simple(Blocks.DIRT),
                        UniformInt.of(5, 11),
                        0.9F));

        // The barrens version: small patches, mostly bare, and what soil there is has gone over to
        // coarse dirt. The chance is what does the work — at 0.35 most of the end stone is left
        // showing through, which is the whole point of the biome.
        FeatureUtils.register(context, SPIRIT_FLOOR_SPARSE, FeatureRegistry.SPIRIT_FLOOR.get(),
                new SpiritFloorFeature.Config(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.COARSE_DIRT.defaultBlockState(), 6)
                                .add(Blocks.GRASS_BLOCK.defaultBlockState(), 2)
                                .add(Blocks.MOSS_BLOCK.defaultBlockState(), 1)),
                        BlockStateProvider.simple(Blocks.DIRT),
                        UniformInt.of(3, 6),
                        0.35F));

        // The soulwood. A pale trunk that grows straight, forked or wind-bent (SoulwoodTrunkPlacer
        // rolls the habit) into a holed, wisp-trailing crown (SoulwoodFoliagePlacer).
        FeatureUtils.register(context, SOULWOOD, Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(BlockRegistry.SOULWOOD_LOG.get()),
                        new SoulwoodTrunkPlacer(7, 3, 2, 0.35F, 0.30F, false),
                        // Persistent leaves, and this is a bug fix rather than a preference.
                        //
                        // TreeFeature.updateLeaves is what assigns a leaf its decay distance once a
                        // tree has generated. It enqueues candidates through
                        // LeavesBlock.getOptionalDistanceAt, which answers only for states carrying
                        // BlockStateProperties.DISTANCE — vanilla's IntegerProperty over 1..7.
                        // ModifiedLeavesBlock declares its own "distance" over 0..14, a different
                        // property object entirely, so our leaves are never enqueued and never
                        // assigned anything. They keep the default 14, which *is* DECAY_DISTANCE,
                        // so every one of them decays on the first random tick and the tree is left
                        // as a bare bole. The fen willow only survives the same trap because its
                        // drape() threads logs through the curtain and neighbour updates rescue
                        // most of the leaves; a crown fed by two hidden blocks has no such luck.
                        //
                        // Persistent is the honest answer here rather than a workaround: this
                        // canopy is deliberately not attached to a trunk, so there is no distance
                        // that would ever describe it correctly.
                        BlockStateProvider.simple(BlockRegistry.SOULWOOD_LEAVES.get().defaultBlockState()
                                .setValue(BlockStateProperties.PERSISTENT, Boolean.TRUE)),
                        new SoulwoodFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0),
                                ConstantInt.of(2), UniformInt.of(1, 3), 0.45F, 0.10F),
                        new TwoLayersFeatureSize(1, 0, 3))
                        .ignoreVines()
                        .build());

        // Standing water on an island top. The feature erodes any cell whose neighbour is not solid
        // before it places anything, three passes over, so a pool can neither spill off the edge of
        // an island nor drain through it into the void — which on floating terrain is the only way
        // water is survivable at all.
        FeatureUtils.register(context, SPIRIT_POOL, FeatureRegistry.BOG_POOL.get(),
                new BogPoolFeature.Config(
                        BlockStateProvider.simple(Blocks.WATER),
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.SHORT_GRASS.defaultBlockState(), 4)
                                .add(Blocks.FERN.defaultBlockState(), 2)),
                        0.35F,
                        UniformInt.of(2, 4)));

        FeatureUtils.register(context, SPIRIT_GRASS, Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(24, 6, 2,
                        PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(new WeightedStateProvider(
                                        SimpleWeightedRandomList.<BlockState>builder()
                                                .add(Blocks.SHORT_GRASS.defaultBlockState(), 6)
                                                .add(Blocks.FERN.defaultBlockState(), 1))))));

        // Both flowers from one patch feature rather than two. They share a habitat and a look, and
        // a single mixed patch means a clearing comes out speckled with both instead of striped.
        FeatureUtils.register(context, SPIRIT_FLOWERS, Feature.FLOWER,
                new RandomPatchConfiguration(32, 7, 3,
                        PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(new WeightedStateProvider(
                                        SimpleWeightedRandomList.<BlockState>builder()
                                                // The realm's own pale flowers carry the patch...
                                                .add(BlockRegistry.GLIMMERBLOOM.get().defaultBlockState(), 6)
                                                .add(BlockRegistry.VEILCAP.get().defaultBlockState(), 4)
                                                // ...and the charged ones are scattered thinly
                                                // through it. One in twelve, so a vivid bloom is
                                                // something you notice rather than something the
                                                // meadow is made of — which is the difference
                                                // between a highlight and a carpet.
                                                .add(BlockRegistry.MANABLOOM.get().defaultBlockState(), 1)
                                                .add(BlockRegistry.EMBERBLOOM.get().defaultBlockState(), 1)
                                                .add(BlockRegistry.DUSKBLOOM.get().defaultBlockState(), 1))))));

        // The small ruin: a few low stones, mostly broken, with a mossy floor between them. Moss
        // rather than soulwood boards on purpose — the centre flower has to be able to root in it,
        // and a flower cannot stand on planks.
        FeatureUtils.register(context, SPIRIT_CAIRN, FeatureRegistry.SPIRIT_RUIN.get(),
                new SpiritRuinFeature.Config(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.SPIRITSTONE_BRICKS.get().defaultBlockState(), 5)
                                .add(BlockRegistry.SPIRITSTONE.get().defaultBlockState(), 2)),
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        BlockStateProvider.simple(BlockRegistry.GLIMMERBLOOM.get()),
                        UniformInt.of(3, 4),
                        // Taller than before, and less of it broken. A stone that only clears the
                        // grass by a block is indistinguishable from litter; these read as built.
                        UniformInt.of(3, 5),
                        0.35F));

        // The landmark: the same hands, given room. Taller, wider, and much more of it still
        // standing, so it carries across the void and is worth walking to.
        FeatureUtils.register(context, SPIRIT_HALL, FeatureRegistry.SPIRIT_RUIN.get(),
                new SpiritRuinFeature.Config(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.SPIRITSTONE_BRICKS.get().defaultBlockState(), 6)
                                .add(BlockRegistry.SPIRITSTONE.get().defaultBlockState(), 1)),
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        BlockStateProvider.simple(BlockRegistry.VEILCAP.get()),
                        UniformInt.of(7, 10),
                        UniformInt.of(5, 9),
                        0.25F));

        // Spires: tapering spiritstone pillars with a brick band and a glowing wispcap on top.
        FeatureUtils.register(context, SPIRIT_SPIRE, FeatureRegistry.SPIRIT_SPIRE.get(),
                new SpiritSpireFeature.Config(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.SPIRITSTONE.get().defaultBlockState(), 8)
                                .add(BlockRegistry.SPIRITSTONE_BRICKS.get().defaultBlockState(), 1)),
                        BlockStateProvider.simple(BlockRegistry.SPIRITSTONE_BRICKS.get()),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.WISPCAP.get().defaultBlockState(), 5)
                                .add(BlockRegistry.GLIMMERBLOOM.get().defaultBlockState(), 2)
                                .add(BlockRegistry.VEILCAP.get().defaultBlockState(), 2)),
                        BlockStateProvider.simple(Blocks.MOSS_CARPET),
                        UniformInt.of(7, 19),
                        UniformInt.of(1, 2)));

        // Ancient soulwood: a 2x2 bole, usually forked into several crowns, willow vines trailing off it.
        FeatureUtils.register(context, SOULWOOD_ANCIENT, Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(BlockRegistry.SOULWOOD_LOG.get()),
                        new SoulwoodTrunkPlacer(11, 3, 3, 0.65F, 0.20F, true),
                        BlockStateProvider.simple(BlockRegistry.SOULWOOD_LEAVES.get().defaultBlockState()
                                .setValue(BlockStateProperties.PERSISTENT, Boolean.TRUE)),
                        new SoulwoodFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0),
                                ConstantInt.of(3), UniformInt.of(2, 4), 0.5F, 0.08F),
                        new TwoLayersFeatureSize(2, 1, 4))
                        .decorators(List.of(new WillowTreeVineDecorator(0.18F)))
                        .ignoreVines()
                        .build());

        // Wispcap glades: tight clusters of the glowing mushroom, the realm's night-lights.
        FeatureUtils.register(context, WISPCAP_GLADE, Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(14, 3, 1,
                        PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(new WeightedStateProvider(
                                        SimpleWeightedRandomList.<BlockState>builder()
                                                .add(BlockRegistry.WISPCAP.get().defaultBlockState(), 7)
                                                .add(BlockRegistry.VEILCAP.get().defaultBlockState(), 2))))));

        // Spirit veils: pale flowers that only the meadow carries, thick enough to read as a field.
        FeatureUtils.register(context, SPIRIT_VEIL, Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(40, 8, 3,
                        PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(new WeightedStateProvider(
                                        SimpleWeightedRandomList.<BlockState>builder()
                                                .add(BlockRegistry.GLIMMERBLOOM.get().defaultBlockState(), 5)
                                                .add(BlockRegistry.MOONLILY.get().defaultBlockState(), 2)
                                                .add(Blocks.FERN.defaultBlockState(), 2)
                                                .add(BlockRegistry.DUSKBLOOM.get().defaultBlockState(), 1))))));
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        PlacementUtils.register(context, SPIRIT_FLOOR_PLACED, configured.getOrThrow(SPIRIT_FLOOR),
                CountPlacement.of(8), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_FLOOR_SPARSE_PLACED, configured.getOrThrow(SPIRIT_FLOOR_SPARSE),
                CountPlacement.of(4), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_GRASS_PLACED, configured.getOrThrow(SPIRIT_GRASS),
                CountPlacement.of(4), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_POOL_PLACED, configured.getOrThrow(SPIRIT_POOL),
                CountPlacement.of(3), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        // Sparse. A stand of them here and there, never a forest — the emptiness is the realm.
        //
        // The spacing filter is what stops soulwoods growing through one another. Their crowns are
        // radius three and float clear of the trunk, so TreeFeature's own check — which only guards
        // the bole — happily lets two stand close enough that the canopies merge into one mass with
        // two stumps under it. The filter goes last, after the heightmap has resolved a real
        // position, because it needs a concrete spot to measure from.
        // HEIGHTMAP_TOP_SOLID, not HEIGHTMAP. The latter is MOTION_BLOCKING, which counts leaves,
        // so every candidate after the first was being placed on top of an existing canopy — which
        // is how trees ended up four deep. OCEAN_FLOOR_WG ignores leaves and lands on real ground.
        PlacementUtils.register(context, SOULWOOD_PLACED, configured.getOrThrow(SOULWOOD),
                RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SpacedFilter.of(6, 20, BlockRegistry.SOULWOOD_LOG.get()),
                BiomeFilter.biome());

        // Except in the grove, which is the exception that makes the rule legible: several per
        // chunk, so arriving in one feels like arriving somewhere rather than passing something.
        // The count is raised rather than the spacing lowered — attempts that land too close are
        // simply dropped, so asking for more is how a grove gets denser without trees overlapping.
        // The height passed here is twenty: a soulwood's bole plus its gap plus its crown is a good
        // deal taller than the five-block band the first version looked through, and anything less
        // than the tree's full extent lets a candidate sit above one without noticing it.
        PlacementUtils.register(context, SOULWOOD_DENSE_PLACED, configured.getOrThrow(SOULWOOD),
                CountPlacement.of(UniformInt.of(6, 10)), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SpacedFilter.of(5, 20, BlockRegistry.SOULWOOD_LOG.get()),
                BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_FLOWERS_PLACED, configured.getOrThrow(SPIRIT_FLOWERS),
                CountPlacement.of(2), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        // Much rarer than the first pass. At one in six they were everywhere, and because a cairn
        // that loses most of its ring to the void or to uneven ground comes out as three short
        // stubs, the realm read as though it had been sprinkled with random pillars rather than
        // dotted with ruins. One in twenty-two, and only where enough of the ring can stand.
        PlacementUtils.register(context, SPIRIT_SPIRE_PLACED, configured.getOrThrow(SPIRIT_SPIRE),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_SPIRE_SPARSE_PLACED, configured.getOrThrow(SPIRIT_SPIRE),
                RarityFilter.onAverageOnceEvery(9), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());

        PlacementUtils.register(context, SOULWOOD_ANCIENT_PLACED, configured.getOrThrow(SOULWOOD_ANCIENT),
                RarityFilter.onAverageOnceEvery(9), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SpacedFilter.of(9, 20, BlockRegistry.SOULWOOD_LOG.get()),
                BiomeFilter.biome());

        PlacementUtils.register(context, WISPCAP_GLADE_PLACED, configured.getOrThrow(WISPCAP_GLADE),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_VEIL_PLACED, configured.getOrThrow(SPIRIT_VEIL),
                CountPlacement.of(3), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        PlacementUtils.register(context, SPIRIT_CAIRN_PLACED, configured.getOrThrow(SPIRIT_CAIRN),
                RarityFilter.onAverageOnceEvery(22), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SpacedFilter.of(10, 6, BlockRegistry.SPIRITSTONE_BRICKS.get()),
                BiomeFilter.biome());

        // Rare enough to be a landmark, and held well clear of any other stonework so it is never
        // found growing out of a cairn.
        PlacementUtils.register(context, SPIRIT_HALL_PLACED, configured.getOrThrow(SPIRIT_HALL),
                RarityFilter.onAverageOnceEvery(110), InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SpacedFilter.of(16, 10, BlockRegistry.SPIRITSTONE_BRICKS.get()),
                BiomeFilter.biome());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }
}
