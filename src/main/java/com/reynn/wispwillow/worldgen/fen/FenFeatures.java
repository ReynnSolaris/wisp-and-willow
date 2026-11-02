package com.reynn.wispwillow.worldgen.fen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.FeatureRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import com.reynn.wispwillow.worldgen.feature.BogPoolFeature;
import com.reynn.wispwillow.worldgen.feature.FallenLogFeature;
import com.reynn.wispwillow.worldgen.feature.FenFloorFeature;
import com.reynn.wispwillow.worldgen.tree.FenDrapeDecorator;
import com.reynn.wispwillow.worldgen.tree.WeepingFoliagePlacer;
import com.reynn.wispwillow.worldgen.tree.WeepingWillowTrunkPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.OptionalInt;

/**
 * Everything that grows in the Willow Fen, and the little of it that strays elsewhere.
 *
 * <p>The fen is a biome of its own ({@link WillowFenBiome}), and that biome lists these features
 * directly. What is left here as a biome modifier is deliberately thin: swamps get reeds and
 * lilies so the fen's plants are not a complete surprise when you first meet them, and rivers
 * get reeds along the bank. Everything else — the trees, the pools, the peat, the creatures —
 * belongs to the fen alone.</p>
 */
public final class FenFeatures {
    private FenFeatures() {}

    // ---- Configured -------------------------------------------------------
    public static final ResourceKey<ConfiguredFeature<?, ?>> WEEPING_TREE = configured("weeping_fen_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GREAT_WEEPING_TREE = configured("great_weeping_fen_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FEN_TREES = configured("fen_trees");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOG_CYPRESS = configured("bog_cypress");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERREED_PATCH = configured("whisperreed_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOONLILY_PATCH = configured("moonlily_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FEN_FLOWERS = configured("fen_flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WISPCAP_PATCH = configured("wispcap_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PEAT_DISK = configured("peat_disk");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG = configured("fallen_fen_log");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOG_POOL = configured("bog_pool");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FEN_BOULDER = configured("fen_boulder");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FEN_FLOOR = configured("fen_floor");

    // ---- Placed -----------------------------------------------------------
    public static final ResourceKey<PlacedFeature> FEN_TREES_PLACED = placed("fen_trees");
    public static final ResourceKey<PlacedFeature> FEN_TREES_DENSE_PLACED = placed("fen_trees_dense");
    public static final ResourceKey<PlacedFeature> BOG_CYPRESS_PLACED = placed("bog_cypress");
    public static final ResourceKey<PlacedFeature> WHISPERREED_PATCH_PLACED = placed("whisperreed_patch");
    public static final ResourceKey<PlacedFeature> MOONLILY_PATCH_PLACED = placed("moonlily_patch");
    public static final ResourceKey<PlacedFeature> FEN_FLOWERS_PLACED = placed("fen_flowers");
    public static final ResourceKey<PlacedFeature> WISPCAP_PATCH_PLACED = placed("wispcap_patch");
    public static final ResourceKey<PlacedFeature> PEAT_DISK_PLACED = placed("peat_disk");
    public static final ResourceKey<PlacedFeature> FALLEN_LOG_PLACED = placed("fallen_fen_log");
    public static final ResourceKey<PlacedFeature> BOG_POOL_PLACED = placed("bog_pool");
    public static final ResourceKey<PlacedFeature> FEN_BOULDER_PLACED = placed("fen_boulder");
    public static final ResourceKey<PlacedFeature> FEN_FLOOR_PLACED = placed("fen_floor");

    // ---- Biome modifiers --------------------------------------------------
    public static final ResourceKey<BiomeModifier> ADD_FEN_TRACES = modifier("add_fen_traces");
    public static final ResourceKey<BiomeModifier> ADD_RIVER_REEDS = modifier("add_river_reeds");

    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        // Wider caps and one more limb apiece than the first pass. A willow's crown is its whole
        // silhouette, and at radius two on three limbs these read as saplings with a fringe.
        FeatureUtils.register(context, WEEPING_TREE, Feature.TREE,
                weepingTree(new WeepingWillowTrunkPlacer(4, 2, 1, UniformInt.of(3, 5), UniformInt.of(3, 4)),
                        3, UniformInt.of(3, 6), new TwoLayersFeatureSize(1, 0, 2)));
        FeatureUtils.register(context, GREAT_WEEPING_TREE, Feature.TREE,
                weepingTree(new WeepingWillowTrunkPlacer(6, 3, 2, UniformInt.of(5, 7), UniformInt.of(4, 6)),
                        // Long curtains are safe again now that weeping leaves reach fourteen rather
                        // than vanilla's six; the branch logs hanging through them are structure, not
                        // a rescue.
                        // Radius four against the small willow's three. Without this the "great"
                        // tree was only taller and bushier-limbed, carrying a cap the same width as
                        // its smaller cousin, so the two never actually read as different trees.
                        4, UniformInt.of(5, 9), new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())));
        FeatureUtils.register(context, FEN_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(GREAT_WEEPING_TREE)), 0.3F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(WEEPING_TREE))));

        // The cypress stands where the willows will not: out in the open water, narrow and bare-
        // legged, with needles instead of a curtain.
        FeatureUtils.register(context, BOG_CYPRESS, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.MANGROVE_LOG),
                new UpwardsBranchingTrunkPlacer(8, 4, 3, UniformInt.of(1, 4), 0.5F, UniformInt.of(0, 1),
                        blocks.getOrThrow(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH)),
                BlockStateProvider.simple(BlockRegistry.CYPRESS_NEEDLES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 70),
                // The same allowance vanilla gives a mangrove; anything tighter and a crowded fen
                // never has room for one.
                new TwoLayersFeatureSize(2, 0, 2))
                .decorators(List.of(new FenDrapeDecorator(0.12F, UniformInt.of(1, 3))))
                .ignoreVines()
                .build());

        BlockPredicate standable = BlockPredicate.anyOf(
                BlockPredicate.matchesBlocks(Blocks.AIR),
                BlockPredicate.allOf(BlockPredicate.matchesBlocks(Blocks.WATER), BlockPredicate.matchesFluids(Fluids.WATER)));
        BlockPredicate besideWater = BlockPredicate.anyOf(
                BlockPredicate.matchesFluids(Fluids.WATER),
                BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), Fluids.WATER),
                BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), Fluids.WATER),
                BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), Fluids.WATER),
                BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), Fluids.WATER));
        FeatureUtils.register(context, WHISPERREED_PATCH, Feature.RANDOM_PATCH, new RandomPatchConfiguration(40, 5, 2,
                PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(BlockRegistry.WHISPERREED.get())),
                        BlockPredicate.allOf(
                                standable,
                                BlockPredicate.matchesBlocks(new Vec3i(0, 1, 0), Blocks.AIR),
                                BlockPredicate.wouldSurvive(BlockRegistry.WHISPERREED.get().defaultBlockState(), BlockPos.ZERO),
                                besideWater))));

        FeatureUtils.register(context, MOONLILY_PATCH, Feature.RANDOM_PATCH, new RandomPatchConfiguration(10, 7, 3,
                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(BlockRegistry.MOONLILY.get())))));

        FeatureUtils.register(context, FEN_FLOWERS, Feature.RANDOM_PATCH, new RandomPatchConfiguration(48, 6, 2,
                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                        SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.BOGBELL.get().defaultBlockState(), 3)
                                .add(BlockRegistry.DUSKPETAL.get().defaultBlockState(), 2))))));

        FeatureUtils.register(context, WISPCAP_PATCH, Feature.RANDOM_PATCH, new RandomPatchConfiguration(24, 5, 2,
                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(BlockRegistry.WISPCAP.get())))));

        FeatureUtils.register(context, PEAT_DISK, Feature.DISK, new DiskConfiguration(
                RuleBasedBlockStateProvider.simple(BlockRegistry.PEAT.get()),
                BlockPredicate.matchesBlocks(List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.CLAY, Blocks.MUD)),
                UniformInt.of(2, 4), 1));

        FeatureUtils.register(context, FALLEN_LOG, FeatureRegistry.FALLEN_LOG.get(), new FallenLogFeature.Config(
                BlockStateProvider.simple(Blocks.DARK_OAK_LOG),
                UniformInt.of(3, 6),
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.MOSS_CARPET.defaultBlockState(), 4)
                        .add(BlockRegistry.WISPCAP.get().defaultBlockState(), 1)),
                0.5F));

        FeatureUtils.register(context, BOG_POOL, FeatureRegistry.BOG_POOL.get(), new BogPoolFeature.Config(
                BlockStateProvider.simple(BlockRegistry.BOG_WATER_BLOCK.get()),
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(BlockRegistry.PEAT.get().defaultBlockState(), 3)
                        .add(Blocks.MUD.defaultBlockState(), 2)),
                BlockStateProvider.simple(BlockRegistry.WHISPERREED.get()),
                0.3F,
                UniformInt.of(2, 4)));

        FeatureUtils.register(context, FEN_BOULDER, Feature.FOREST_ROCK,
                new BlockStateConfiguration(BlockRegistry.FENSTONE.get().defaultBlockState()));

        // The fen's own ground: peat and mud in ragged patches, with moss where it has dried out.
        FeatureUtils.register(context, FEN_FLOOR, FeatureRegistry.FEN_FLOOR.get(), new FenFloorFeature.Config(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(BlockRegistry.PEAT.get().defaultBlockState(), 5)
                        .add(Blocks.MUD.defaultBlockState(), 3)
                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 2)
                        .add(Blocks.MOSS_BLOCK.defaultBlockState(), 2)),
                BlockStateProvider.simple(BlockRegistry.PEAT.get()),
                UniformInt.of(4, 9),
                0.85F));
    }

    /**
     * A willow: a short bole breaking into arching limbs, each trailing its own curtain.
     *
     * @param trunk  the limb frame, which decides how wide the crown spreads
     * @param radius how far the leaf cap reaches from each limb tip
     * @param strand how far the curtain trails below it
     */
    private static TreeConfiguration weepingTree(TrunkPlacer trunk, int radius, UniformInt strand, FeatureSize size) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.DARK_OAK_LOG),
                trunk,
                BlockStateProvider.simple(BlockRegistry.WEEPING_LEAVES.get()),
                // Foliage height one, not zero. A zero-height cap is a single row of leaves, so
                // every willow in the fen was a flat plate with strands hanging off it — thin from
                // the side and transparent from below. One extra row gives each limb's cap some
                // body without closing the canopy over the fen.
                new WeepingFoliagePlacer(ConstantInt.of(radius), ConstantInt.of(0), ConstantInt.of(1), strand, 0.5F),
                size)
                .decorators(List.of(new FenDrapeDecorator(0.15F, UniformInt.of(2, 5))))
                .ignoreVines()
                .build();
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // Placed the way vanilla places swamp oaks, so fen trees may stand in water up to two deep.
        PlacementUtils.register(context, FEN_TREES_PLACED, configured.getOrThrow(FEN_TREES),
                treePlacement(PlacementUtils.countExtra(1, 0.5F, 1)));
        // countExtra encodes the chance as a weighted list, so it has to be exactly 1/n.
        // Enough trees to read as a fen, with sky between them rather than a closed canopy.
        PlacementUtils.register(context, FEN_TREES_DENSE_PLACED, configured.getOrThrow(FEN_TREES),
                treePlacement(PlacementUtils.countExtra(1, 0.5F, 1)));
        PlacementUtils.register(context, BOG_CYPRESS_PLACED, configured.getOrThrow(BOG_CYPRESS),
                treePlacement(PlacementUtils.countExtra(0, 0.25F, 1)));

        PlacementUtils.register(context, WHISPERREED_PATCH_PLACED, configured.getOrThrow(WHISPERREED_PATCH),
                CountPlacement.of(5), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());

        PlacementUtils.register(context, MOONLILY_PATCH_PLACED, configured.getOrThrow(MOONLILY_PATCH),
                VegetationPlacements.worldSurfaceSquaredWithCount(2));

        PlacementUtils.register(context, FEN_FLOWERS_PLACED, configured.getOrThrow(FEN_FLOWERS),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        PlacementUtils.register(context, WISPCAP_PATCH_PLACED, configured.getOrThrow(WISPCAP_PATCH),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        PlacementUtils.register(context, PEAT_DISK_PLACED, configured.getOrThrow(PEAT_DISK),
                CountPlacement.of(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)), BiomeFilter.biome());

        PlacementUtils.register(context, FALLEN_LOG_PLACED, configured.getOrThrow(FALLEN_LOG),
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(1),
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());

        PlacementUtils.register(context, BOG_POOL_PLACED, configured.getOrThrow(BOG_POOL),
                RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());

        PlacementUtils.register(context, FEN_BOULDER_PLACED, configured.getOrThrow(FEN_BOULDER),
                RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

        // The floor paints itself from the surface up, so it only needs somewhere in the chunk to start.
        PlacementUtils.register(context, FEN_FLOOR_PLACED, configured.getOrThrow(FEN_FLOOR),
                CountPlacement.of(6), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
    }

    private static List<net.minecraft.world.level.levelgen.placement.PlacementModifier> treePlacement(
            net.minecraft.world.level.levelgen.placement.PlacementModifier count) {
        return List.of(count,
                InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(2),
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BiomeFilter.biome(),
                BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING.defaultBlockState(), BlockPos.ZERO)));
    }

    public static void bootstrapModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        // Swamps get a hint of the fen — reeds at the waterline, the odd moonlily — and nothing more.
        context.register(ADD_FEN_TRACES, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(TagRegistry.Biomes.HAS_FEN_TRACES),
                HolderSet.direct(placed.getOrThrow(WHISPERREED_PATCH_PLACED), placed.getOrThrow(MOONLILY_PATCH_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_RIVER_REEDS, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_RIVER),
                HolderSet.direct(placed.getOrThrow(WHISPERREED_PATCH_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    private static ResourceKey<BiomeModifier> modifier(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }
}
