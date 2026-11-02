package com.reynn.wispwillow.worldgen.spirit;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.stream.Stream;

/**
 * Picks which of the realm's four biomes a column belongs to.
 *
 * <p>This exists because the obvious answer does not work. {@code MultiNoiseBiomeSource} selects on
 * climate, and climate comes from the chunk generator's noise router — but the router behind
 * {@code NoiseGeneratorSettings.END}, which is what gives this dimension its islands, is almost
 * entirely {@code zero()}. Only erosion carries a real signal there, and it is the island-shape
 * function itself. Four biomes keyed on that would not produce four regions; they would produce
 * concentric rings on every single island, the same arrangement everywhere, because the only thing
 * varying is how far into an island you are standing.</p>
 *
 * <p>So the realm samples its own field instead. Two layers of simplex at different scales: one
 * decides the broad character of a region, the other is consulted only for water, so the shallows
 * can cut across the other three rather than occupying a band between two of them.</p>
 *
 * <p>The seed is fixed rather than drawn from the world seed. The biome source is constructed from
 * datapack JSON, which has no access to the world's seed, and the alternative — threading one
 * through the codec — would bake a number into the saved dimension and make every world that used
 * it identical anyway. The practical effect is that the arrangement of regions is the same from one
 * world to the next, while the islands themselves, which come from the generator and <em>do</em>
 * see the world seed, are not.</p>
 */
public class SpiritBiomeSource extends BiomeSource {
    public static final MapCodec<SpiritBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("meadow").forGetter(s -> s.meadow),
                    Biome.CODEC.fieldOf("grove").forGetter(s -> s.grove),
                    Biome.CODEC.fieldOf("barrens").forGetter(s -> s.barrens),
                    Biome.CODEC.fieldOf("shallows").forGetter(s -> s.shallows)
            ).apply(instance, SpiritBiomeSource::new));

    /** Region size, in blocks, for the broad layer and for the water layer. */
    private static final double REGION_SCALE = 360.0;
    private static final double WATER_SCALE = 220.0;

    /** Above this the water layer wins outright; the shallows are meant to be the rare one. */
    private static final double WATER_THRESHOLD = 0.52;
    private static final double BARRENS_THRESHOLD = -0.30;
    private static final double GROVE_THRESHOLD = 0.28;

    private static final long SEED = 0xF53A_5091L;

    private final Holder<Biome> meadow;
    private final Holder<Biome> grove;
    private final Holder<Biome> barrens;
    private final Holder<Biome> shallows;

    private final SimplexNoise region;
    private final SimplexNoise water;

    public SpiritBiomeSource(Holder<Biome> meadow, Holder<Biome> grove,
                             Holder<Biome> barrens, Holder<Biome> shallows) {
        this.meadow = meadow;
        this.grove = grove;
        this.barrens = barrens;
        this.shallows = shallows;

        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(SEED));
        this.region = new SimplexNoise(random);
        this.water = new SimplexNoise(random);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return MainRegistry.SPIRIT_BIOME_SOURCE.get();
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(meadow, grove, barrens, shallows);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Biome coordinates arrive in quarts; the noise is scaled in blocks so the region sizes
        // above mean what they say.
        double bx = QuartPos.toBlock(x);
        double bz = QuartPos.toBlock(z);

        if (water.getValue(bx / WATER_SCALE, bz / WATER_SCALE) > WATER_THRESHOLD) {
            return shallows;
        }

        double r = region.getValue(bx / REGION_SCALE, bz / REGION_SCALE);
        if (r < BARRENS_THRESHOLD) return barrens;
        if (r > GROVE_THRESHOLD) return grove;
        return meadow;
    }
}
