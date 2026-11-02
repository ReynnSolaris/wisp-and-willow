package com.reynn.wispwillow.worldgen;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class PlacedFeatures {

    public static final ResourceKey<PlacedFeature> ASTRALITE_ORE_PLACED_KEY = registerKey("astralite_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(
                context,
                ASTRALITE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ConfiguredFeatures.OVERWORLD_ASTRALITE_ORE_KEY),
                OreWorldGen.commonOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(42)))
        );

        com.reynn.wispwillow.worldgen.fen.FenFeatures.bootstrapPlaced(context);
        com.reynn.wispwillow.worldgen.spirit.SpiritFeatures.bootstrapPlaced(context);
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                 Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifers)));
    }
}
