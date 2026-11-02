package com.reynn.wispwillow.worldgen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.worldgen.fen.FenFeatures;
import com.reynn.wispwillow.worldgen.tree.WeepingFoliagePlacer;
import com.reynn.wispwillow.worldgen.tree.WeepingWillowTrunkPlacer;
import com.reynn.wispwillow.worldgen.tree.WillowTreeVineDecorator;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.OptionalInt;

public class ConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILLOW_TREE_KEY = registerKey("willow_tree");

    //ORE
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_ASTRALITE_ORE_KEY = registerKey("overworld_astralite_ore");


    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        var holdergetter = context.lookup(Registries.BLOCK);

        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        //ORE

        List<OreConfiguration.TargetBlockState> overworldAstraliteOre = List.of(
                OreConfiguration.target(stoneReplaceables, BlockRegistry.ASTRALITE_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get().defaultBlockState())
        );

        register(context, OVERWORLD_ASTRALITE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldAstraliteOre, 3));


        // TREE
        // The willow proper: a short bole that breaks into arching limbs, each trailing a curtain
        // of leaves nearly to the ground. The shape is the mod's own -- vanilla has no placer that
        // weeps -- and it is what the infusion rite is ultimately for.
        register(context, WILLOW_TREE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(BlockRegistry.WILLOW_LOG.get()),
                new WeepingWillowTrunkPlacer(5, 2, 2, UniformInt.of(2, 4), UniformInt.of(3, 5)),
                new WeightedStateProvider(
                        SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlockRegistry.WILLOW_LEAVES.get().defaultBlockState(), 3)
                                .add(BlockRegistry.FLOWERING_WILLOW_LEAVES.get().defaultBlockState(), 1)
                ),
                // Every limb carries its own cap, so each one has to be light: a single full row
                // of leaf, and strands on about a third of its rim.
                new WeepingFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(0),
                        UniformInt.of(3, 6), 0.4f),
                new ThreeLayersFeatureSize(1, 1, 1, 2, 3, OptionalInt.empty())
        ).decorators(
                List.of(
                        new WillowTreeVineDecorator(
                                0.07f
                        )
                )
        ).ignoreVines().build());

        FenFeatures.bootstrapConfigured(context);
        com.reynn.wispwillow.worldgen.spirit.SpiritFeatures.bootstrapConfigured(context);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
