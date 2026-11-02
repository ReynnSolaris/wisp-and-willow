package com.reynn.wispwillow.worldgen.fen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The Willow Fen: standing water under a low, weeping canopy.
 *
 * <p>Its own biome, not a coat of paint over a swamp. The water is dark and green, the grass and
 * leaves are muted to match, mist drifts through it, and everything the fen owns — its trees,
 * reeds, lilies, pools, peat and creatures — is listed here rather than added to someone else's
 * biome. Vanilla swamps keep a light scattering of fen plants, which is what makes finding the
 * real thing feel like arriving somewhere.</p>
 *
 * <p>{@link WillowFenClimate} decides where it generates.</p>
 */
public final class WillowFenBiome {
    private WillowFenBiome() {}

    public static final ResourceKey<Biome> WILLOW_FEN = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "willow_fen"));

    // Standing water, seen through peat: green-brown, and dark below the surface.
    private static final int WATER_COLOUR = 0x4E6B4A;
    private static final int WATER_FOG_COLOUR = 0x24331F;
    private static final int FOG_COLOUR = 0x9CA88C;
    private static final int GRASS_COLOUR = 0x6E8B4E;
    private static final int FOLIAGE_COLOUR = 0x56763F;
    private static final float TEMPERATURE = 0.7F;

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        context.register(WILLOW_FEN, new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(TEMPERATURE)
                .downfall(0.9F)
                .specialEffects(effects())
                .mobSpawnSettings(spawns())
                .generationSettings(generation(features, carvers))
                .build());
    }

    private static BiomeSpecialEffects effects() {
        return new BiomeSpecialEffects.Builder()
                .waterColor(WATER_COLOUR)
                .waterFogColor(WATER_FOG_COLOUR)
                .fogColor(FOG_COLOUR)
                .skyColor(calculateSkyColor(TEMPERATURE))
                .grassColorOverride(GRASS_COLOUR)
                .foliageColorOverride(FOLIAGE_COLOUR)
                // Mist over the water, thin enough to read as air rather than weather.
                .ambientParticle(new AmbientParticleSettings(ParticleTypes.SPORE_BLOSSOM_AIR, 0.004F))
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP))
                .build();
    }

    private static MobSpawnSettings spawns() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(builder, 70);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityRegistry.FEN_HERON.get(), 12, 1, 3));
        // Uncommon and in ones and twos: a companion you have to go and find is worth more than one
        // you trip over, and a pack of them would be a threat the fen is not meant to have.
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityRegistry.WILLOW_HOUND.get(), 8, 1, 2));
        builder.addSpawn(MobCategory.AMBIENT, new MobSpawnSettings.SpawnerData(EntityRegistry.WISPFLY.get(), 20, 3, 6));
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5));
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.BOGGED, 30, 4, 4));
        return builder.build();
    }

    private static BiomeGenerationSettings generation(HolderGetter<PlacedFeature> features,
                                                      HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(features, carvers);

        // Everything an overworld biome is expected to have: caves, springs, ores, monster rooms.
        //
        // The ORDER of these calls is not cosmetic. Features shared between biomes must appear in
        // the same relative order in every biome that has them, or the game refuses to load the
        // world with "Feature order cycle found". Fossils before the global block is what every
        // vanilla biome does, and calling them the other way round put fossils after monster rooms
        // here while desert had them before -- two biomes, two shared features, opposite order.
        // Follow vanilla's sequence exactly; the fen's own features go after, where they are free.
        BiomeDefaultFeatures.addFossilDecoration(builder);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(builder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
        BiomeDefaultFeatures.addDefaultSprings(builder);
        BiomeDefaultFeatures.addSurfaceFreezing(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
        BiomeDefaultFeatures.addDefaultMushrooms(builder);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);

        // The fen's own ground, laid over the top before anything grows on it.
        builder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features.getOrThrow(FenFeatures.FEN_FLOOR_PLACED));
        builder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features.getOrThrow(FenFeatures.FALLEN_LOG_PLACED));
        builder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features.getOrThrow(FenFeatures.FEN_BOULDER_PLACED));
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, features.getOrThrow(FenFeatures.PEAT_DISK_PLACED));
        builder.addFeature(GenerationStep.Decoration.LAKES, features.getOrThrow(FenFeatures.BOG_POOL_PLACED));

        // Denser here than in the swamps that only borrow a little of the fen's character.
        // Cypress first. The willows are dense and their crowns are eight blocks across, so a tall
        // narrow tree placed after them never finds the headroom -- it grew fine on clear ground
        // and not once in a generated fen until it got first pick.
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.BOG_CYPRESS_PLACED));
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.FEN_TREES_DENSE_PLACED));
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.WHISPERREED_PATCH_PLACED));
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.MOONLILY_PATCH_PLACED));
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.FEN_FLOWERS_PLACED));
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(FenFeatures.WISPCAP_PATCH_PLACED));

        return builder.build();
    }

    /** Vanilla's sky tint for a given temperature; the helper itself is not public. */
    private static int calculateSkyColor(float temperature) {
        float scaled = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - scaled * 0.05F, 0.5F + scaled * 0.1F, 1.0F);
    }
}
