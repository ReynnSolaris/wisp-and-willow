package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
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
 * Lays the fen's own ground over the top of whatever the world generated.
 *
 * <p>A biome's surface blocks come from the noise settings' surface rules, and those are baked
 * into the vanilla datapack where a mod cannot reach them without replacing the file wholesale.
 * Painting the top layer as a feature gets the same result from the outside: soft, dark, wet
 * ground in ragged patches instead of an even sheet of grass.</p>
 *
 * <p>Only the block at the surface is touched, and only where it is the ordinary grass or dirt
 * the generator laid down, so stone outcrops, water and anything already placed stay as they
 * are.</p>
 */
public class FenFloorFeature extends Feature<FenFloorFeature.Config> {

    public record Config(BlockStateProvider top, BlockStateProvider under, IntProvider radius,
                         float chance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("top").forGetter(Config::top),
                BlockStateProvider.CODEC.fieldOf("under").forGetter(Config::under),
                IntProvider.codec(1, 12).fieldOf("radius").forGetter(Config::radius),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Config::chance)
        ).apply(instance, Config::new));
    }

    public FenFloorFeature(Codec<Config> codec) {
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
                if (falloff > 1.0 || random.nextFloat() > config.chance() * (1.0 - falloff * 0.6)) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                BlockPos surface = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
                BlockState here = level.getBlockState(surface);
                if (!here.is(Blocks.GRASS_BLOCK) && !here.is(Blocks.DIRT) && !here.is(BlockTags.SAND)) continue;

                level.setBlock(surface, config.top().getState(random, surface), 2);
                if (random.nextFloat() < 0.4F) {
                    BlockPos below = surface.below();
                    if (level.getBlockState(below).is(BlockTags.DIRT)) {
                        level.setBlock(below, config.under().getState(random, below), 2);
                    }
                }
                painted = true;
            }
        }
        return painted;
    }
}
