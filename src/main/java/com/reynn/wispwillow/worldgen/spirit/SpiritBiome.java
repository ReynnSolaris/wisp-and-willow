package com.reynn.wispwillow.worldgen.spirit;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import javax.annotation.Nullable;

/**
 * The four biomes of the {@link SpiritRealm}, chosen between by {@link SpiritBiomeSource}.
 *
 * <p>The ground everywhere is end stone, laid down by {@link SpiritNoise};
 * {@link SpiritFeatures#SPIRIT_FLOOR_PLACED} and its sparser cousin skin the island crowns, and the
 * grass and foliage overrides below are what make what grows there the colour of ash and pale water
 * rather than of summer. Nothing hostile lives in any of them — the realm unsettles because it is
 * empty and has no floor, not because it is dangerous.</p>
 *
 * <p>No carvers, no ores, no springs anywhere. There is no stone for an overworld ore to replace and
 * nowhere on a floating island for a cave to go.</p>
 *
 * <p><b>On feature order.</b> Features shared between biomes must appear in the same relative order
 * in every biome that uses them or a world containing both is unloadable with <em>"Feature order
 * cycle found"</em>. That is why there is one {@link #generation} method taking nullable slots
 * rather than four hand-written lists: the order is stated once, and a biome opting out of a step
 * cannot reorder the steps around it. Nothing in the realm is borrowed from vanilla or from the fen,
 * so the Spirit Realm cannot form a cycle with anything outside itself either.</p>
 */
public final class SpiritBiome {
    private SpiritBiome() {}

    public static final ResourceKey<Biome> SPIRIT_MEADOW = key("spirit_meadow");
    public static final ResourceKey<Biome> SOULWOOD_GROVE = key("soulwood_grove");
    public static final ResourceKey<Biome> HOLLOW_BARRENS = key("hollow_barrens");
    public static final ResourceKey<Biome> MISTFALL_SHALLOWS = key("mistfall_shallows");

    /**
     * Pastel blue-green, and it is this rather than the texture that gives soulwood its colour.
     *
     * <p>Leaves are tinted by the biome, so painting the sprite itself a strong colour would multiply
     * against this one and come out muddy. The sprite is kept near-white and the hue lives here,
     * which is how vanilla does it — and it means the whole canopy can be retuned from one number.</p>
     */
    private static final int FOLIAGE_COLOUR = 0xA6DAD8;

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // ---- Spirit Meadow: the default, and the one everything else is a departure from.
        context.register(SPIRIT_MEADOW, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.4F)
                .specialEffects(effects(0x9CD8F0, 0x5E96C4, 0xC2C8F2, 0x8C9CE8, 0xA8DCC4,
                        ParticleTypes.END_ROD, 0.0035F))
                .mobSpawnSettings(living(20))
                .generationSettings(generation(features, carvers,
                        SpiritFeatures.SPIRIT_FLOOR_PLACED,
                        SpiritFeatures.SPIRIT_CAIRN_PLACED, SpiritFeatures.SPIRIT_HALL_PLACED,
                        null, SpiritFeatures.SOULWOOD_PLACED, true, true,
                        SpiritFeatures.SPIRIT_VEIL_PLACED, SpiritFeatures.SPIRIT_SPIRE_SPARSE_PLACED,
                        SpiritFeatures.SOULWOOD_ANCIENT_PLACED, SpiritFeatures.WISPCAP_GLADE_PLACED))
                .build());

        // ---- Soulwood Grove: the same ground, crowded with trees and darker for it.
        context.register(SOULWOOD_GROVE, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.45F)
                .downfall(0.5F)
                // Deeper and closer than the meadow. A grove of soulwood is the one place in the
                // realm with a canopy over it, and the fog is pulled in to make that felt.
                .specialEffects(effects(0x84C9E4, 0x54889C, 0xA8B4E0, 0x7A92DC, 0x8CCAB4,
                        ParticleTypes.END_ROD, 0.0080F))
                .mobSpawnSettings(living(24))
                .generationSettings(generation(features, carvers,
                        SpiritFeatures.SPIRIT_FLOOR_PLACED,
                        SpiritFeatures.SPIRIT_CAIRN_PLACED, null,
                        null, SpiritFeatures.SOULWOOD_DENSE_PLACED, true, true,
                        SpiritFeatures.SOULWOOD_ANCIENT_PLACED, SpiritFeatures.WISPCAP_GLADE_PLACED))
                .build());

        // ---- Hollow Barrens: scoured rock, and the emptiness the realm is supposed to be.
        // The landmark lives here as well as in the meadow, and it is here that it counts: a
        // standing hall is the only thing on the horizon in a biome with nothing else in it.
        context.register(HOLLOW_BARRENS, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.4F)
                .downfall(0.0F)
                .specialEffects(effects(0x8FD4EC, 0x5E96C4, 0xC8CCDC, 0x9AA6D8, 0xB4BCC8,
                        ParticleTypes.ASH, 0.0030F))
                .mobSpawnSettings(sparse())
                .generationSettings(generation(features, carvers,
                        SpiritFeatures.SPIRIT_FLOOR_SPARSE_PLACED,
                        SpiritFeatures.SPIRIT_CAIRN_PLACED, SpiritFeatures.SPIRIT_HALL_PLACED,
                        null, null, false, false,
                        SpiritFeatures.SPIRIT_SPIRE_PLACED))
                .build());

        // ---- Mistfall Shallows: standing water on the island tops, and heavy air over it.
        context.register(MISTFALL_SHALLOWS, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.55F)
                .downfall(0.9F)
                // The only bright water in the realm, and a fog pale enough to read as mist rather
                // than as dusk — this is the one biome that looks lit from within.
                .specialEffects(effects(0xA6EAE4, 0x6FB4B4, 0xC0DCEE, 0x93C4EE, 0xAFD6C4,
                        ParticleTypes.END_ROD, 0.0045F))
                .mobSpawnSettings(living(18))
                .generationSettings(generation(features, carvers,
                        SpiritFeatures.SPIRIT_FLOOR_PLACED,
                        SpiritFeatures.SPIRIT_CAIRN_PLACED, null,
                        SpiritFeatures.SPIRIT_POOL_PLACED, SpiritFeatures.SOULWOOD_PLACED, true, true,
                        SpiritFeatures.WISPCAP_GLADE_PLACED, SpiritFeatures.SPIRIT_SPIRE_SPARSE_PLACED))
                .build());
    }

    private static BiomeSpecialEffects effects(int water, int waterFog, int fog, int sky, int grass,
                                               @Nullable ParticleOptions particle, float probability) {
        BiomeSpecialEffects.Builder builder = new BiomeSpecialEffects.Builder()
                .waterColor(water)
                .waterFogColor(waterFog)
                .fogColor(fog)
                // With an overworld sky overhead these drive the dome directly, so the realm reads
                // as a violet dusk that never advances rather than as the End.
                .skyColor(sky)
                .grassColorOverride(grass)
                .foliageColorOverride(FOLIAGE_COLOUR);
        if (particle != null) {
            builder.ambientParticle(new AmbientParticleSettings(particle, probability));
        }
        return builder.build();
    }

    /**
     * Two creatures, and nothing that wants to hurt you.
     *
     * <p>The glimmerfawn is the only thing here with any weight to it; the wispflies are borrowed
     * from the fen because a meadow with motes drifting over it reads as haunted, and one with
     * nothing in the air at all reads as unfinished.</p>
     */
    private static MobSpawnSettings living(int fawnWeight) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        builder.addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(EntityRegistry.GLIMMERFAWN.get(), fawnWeight, 2, 4));
        builder.addSpawn(MobCategory.AMBIENT,
                new MobSpawnSettings.SpawnerData(EntityRegistry.WISPFLY.get(), 15, 2, 4));
        return builder.build();
    }

    /** The barrens get the odd wispfly and nothing else — that is what makes them barren. */
    private static MobSpawnSettings sparse() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        builder.addSpawn(MobCategory.AMBIENT,
                new MobSpawnSettings.SpawnerData(EntityRegistry.WISPFLY.get(), 6, 1, 2));
        return builder.build();
    }

    /** The shared feature order, with the pieces a given biome does not want left out. */
    private static BiomeGenerationSettings generation(HolderGetter<PlacedFeature> features,
                                                      HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                                      ResourceKey<PlacedFeature> floor,
                                                      @Nullable ResourceKey<PlacedFeature> cairns,
                                                      @Nullable ResourceKey<PlacedFeature> landmark,
                                                      @Nullable ResourceKey<PlacedFeature> pool,
                                                      @Nullable ResourceKey<PlacedFeature> trees,
                                                      boolean groundCover,
                                                      boolean flowers,
                                                      ResourceKey<PlacedFeature>... extras) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(features, carvers);

        // The skin goes on at LOCAL_MODIFICATIONS, which runs well before VEGETAL_DECORATION —
        // grass placed on bare end stone would find no soil to stand on and quietly do nothing.
        builder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features.getOrThrow(floor));

        // Stonework next, on ground that now has soil on it and before anything grows through it.
        if (cairns != null) {
            builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, features.getOrThrow(cairns));
        }
        if (landmark != null) {
            builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, features.getOrThrow(landmark));
        }

        // Water after the soil and before anything growing: the pool needs ground to sink into,
        // and it clears whatever is standing where it wants to be.
        //
        // FLUID_SPRINGS rather than the obvious LAKES, and the difference is the whole feature.
        // LAKES runs *before* LOCAL_MODIFICATIONS, so a pool placed there would be asked to dig
        // into an island that is still bare end stone — and BogPoolFeature refuses any origin whose
        // ground is not in #dirt, so it would have declined every single time and written nothing.
        // The failure is completely silent: the biome would simply have had no water in it.
        if (pool != null) {
            builder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, features.getOrThrow(pool));
        }
        if (trees != null) {
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(trees));
        }
        if (groundCover) {
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                    features.getOrThrow(SpiritFeatures.SPIRIT_GRASS_PLACED));
        }
        if (flowers) {
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                    features.getOrThrow(SpiritFeatures.SPIRIT_FLOWERS_PLACED));
        }
        for (ResourceKey<PlacedFeature> extra : extras) {
            GenerationStep.Decoration step = extra.location().getPath().startsWith("spirit_spire")
                    ? GenerationStep.Decoration.SURFACE_STRUCTURES : GenerationStep.Decoration.VEGETAL_DECORATION;
            builder.addFeature(step, features.getOrThrow(extra));
        }
        return builder.build();
    }

    /** Vanilla's sky tint helper is not public; kept here in case the sky needs it. */
    static int calculateSkyColor(float temperature) {
        float scaled = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - scaled * 0.05F, 0.5F + scaled * 0.1F, 1.0F);
    }
}
