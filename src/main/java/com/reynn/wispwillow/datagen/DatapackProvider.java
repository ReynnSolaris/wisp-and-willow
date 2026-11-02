package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.worldgen.BiomeModifications;
import com.reynn.wispwillow.worldgen.ConfiguredFeatures;
import com.reynn.wispwillow.worldgen.PlacedFeatures;
import com.reynn.wispwillow.worldgen.WWStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatapackProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.BIOME, context -> {
                com.reynn.wispwillow.worldgen.fen.WillowFenBiome.bootstrap(context);
                com.reynn.wispwillow.worldgen.spirit.SpiritBiome.bootstrap(context);
            })
            .add(Registries.NOISE_SETTINGS, com.reynn.wispwillow.worldgen.spirit.SpiritNoise::bootstrap)
            .add(Registries.DIMENSION_TYPE, com.reynn.wispwillow.worldgen.spirit.SpiritRealm::bootstrapType)
            .add(Registries.LEVEL_STEM, com.reynn.wispwillow.worldgen.spirit.SpiritRealm::bootstrapStem)
            .add(Registries.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, PlacedFeatures::bootstrap)
            .add(Registries.STRUCTURE, WWStructures::bootstrap)
            .add(Registries.STRUCTURE_SET, WWStructures::bootstrapSets)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifications::bootstrap)
            .add(Registries.ENCHANTMENT, EnchantmentGeneration::bootstrap);

    public DatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(WispWillow.MODID));
    }
}
