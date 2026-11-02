package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Lays soil and pale grass over the crown of an End island.
 *
 * <p>The realm generates on the End's noise, so every surface it produces is end stone. This is
 * what makes it the Spirit Realm rather than the End: the island tops get a skin of dirt and grass,
 * and because the biome overrides the grass colour, that grass comes out the washed ash-green of
 * the place instead of overworld green.</p>
 *
 * <p>Only the top block of a column is touched, and only where it is still end stone, so anything
 * already placed — and the bare cliff faces down the sides of an island — stays as it is. Columns
 * with nothing under them are skipped outright: over the void the heightmap answers with the
 * minimum build height, and painting there would hang grass in mid-air.</p>
 */
public class SpiritFloorFeature extends Feature<SpiritFloorFeature.Config> {

    public record Config(BlockStateProvider top, BlockStateProvider under, IntProvider radius,
                         float chance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("top").forGetter(Config::top),
                BlockStateProvider.CODEC.fieldOf("under").forGetter(Config::under),
                IntProvider.codec(1, 16).fieldOf("radius").forGetter(Config::radius),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Config::chance)
        ).apply(instance, Config::new));
    }

    public SpiritFloorFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        int radius = config.radius().sample(random);
        boolean painted = false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double falloff = Math.sqrt(dx * dx + dz * dz) / (radius + 0.5);
                if (falloff > 1.0 || random.nextFloat() > config.chance() * (1.0 - falloff * 0.5)) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                // Nothing in this column: the heightmap bottoms out over the void.
                if (top <= level.getMinBuildHeight() + 1) continue;

                BlockPos surface = new BlockPos(x, top - 1, z);
                // Spiritstone, which is what the realm's generator lays down. This test is the
                // reason changing the dimension's default block is never a one-line job: paint only
                // goes onto the block named here, so a mismatch leaves every island bare and says
                // nothing about why.
                if (!level.getBlockState(surface).is(BlockRegistry.SPIRITSTONE.get())) continue;
                // Needs open sky directly above, or we are painting the roof of an overhang.
                if (!level.getBlockState(surface.above()).isAir()) continue;

                level.setBlock(surface, config.top().getState(random, surface), 2);

                // A little depth under the skin, so a broken block shows soil rather than
                // bare stone one layer down.
                BlockPos below = surface.below();
                for (int d = 0; d < 2 && level.getBlockState(below).is(BlockRegistry.SPIRITSTONE.get()); d++) {
                    if (random.nextFloat() < 0.75F) {
                        level.setBlock(below, config.under().getState(random, below), 2);
                    }
                    below = below.below();
                }
                painted = true;
            }
        }
        return painted;
    }
}
