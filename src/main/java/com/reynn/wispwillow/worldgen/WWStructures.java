package com.reynn.wispwillow.worldgen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.datagen.BiomeTagProvider;
import com.reynn.wispwillow.worldgen.structures.FluxiaOasisStructure;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Map;

/**
 * The structures this mod adds to the world, written by datagen rather than by hand.
 *
 * <p>Datagen and not a checked-in JSON because the advancement for finding an oasis needs a
 * real {@code Holder<Structure>} to point at, and only a structure the generator knows about
 * can hand one over.</p>
 */
public final class WWStructures {
    private WWStructures() {}

    public static final ResourceKey<Structure> FLUXIA_OASIS = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "fluxia_oasis"));

    public static final ResourceKey<StructureSet> FLUXIA_OASIS_SET = ResourceKey.create(Registries.STRUCTURE_SET,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "fluxia_oasis"));

    public static void bootstrap(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        context.register(FLUXIA_OASIS, new FluxiaOasisStructure(new Structure.StructureSettings(
                biomes.getOrThrow(BiomeTagProvider.HAS_FLUXIA_OASIS),
                Map.of(),
                // Underground, and after the carvers, so caves cannot cut the dome open again.
                GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
                TerrainAdjustment.NONE)));
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> context) {
        // Roughly one per 1,024 blocks of exploration, and never two within 320: rarer than a
        // village, a little commoner than a woodland mansion. Because the dome never breaks the
        // surface there is nothing to spot one by, so caving into an oasis is luck and /locate
        // is the dependable way to go looking for one.
        context.register(FLUXIA_OASIS_SET, new StructureSet(
                context.lookup(Registries.STRUCTURE).getOrThrow(FLUXIA_OASIS),
                new RandomSpreadStructurePlacement(64, 20, RandomSpreadType.LINEAR, 1931704711)));
    }
}
