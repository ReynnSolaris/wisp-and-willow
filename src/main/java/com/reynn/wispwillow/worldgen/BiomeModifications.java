package com.reynn.wispwillow.worldgen;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BiomeModifications {
    public static final ResourceKey<BiomeModifier> ADD_ASTRALITE_ORE = registerKey("add_astralite_ore");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_ASTRALITE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(PlacedFeatures.ASTRALITE_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        com.reynn.wispwillow.worldgen.fen.FenFeatures.bootstrapModifiers(context);
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }
}
