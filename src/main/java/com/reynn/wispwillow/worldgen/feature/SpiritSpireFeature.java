package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A spire of spiritstone: a tapering natural pillar that rises off an island, banded with the
 * brick of the old ruins near its base and crowned with something that glows.
 *
 * <p>The islands are flat-topped by construction and the ruins are low, so the realm had no
 * verticals at all; a few spires per island give the skyline a silhouette and give a wanderer
 * something to steer by across the void. Spires grow from the ground up and stop at the first
 * block they cannot replace, so one that starts under a tree simply becomes a stump inside it.</p>
 */
public class SpiritSpireFeature extends Feature<SpiritSpireFeature.Config> {

    public record Config(BlockStateProvider stone, BlockStateProvider band, BlockStateProvider crown,
                         BlockStateProvider moss, IntProvider height, IntProvider radius)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("stone").forGetter(Config::stone),
                BlockStateProvider.CODEC.fieldOf("band").forGetter(Config::band),
                BlockStateProvider.CODEC.fieldOf("crown").forGetter(Config::crown),
                BlockStateProvider.CODEC.fieldOf("moss").forGetter(Config::moss),
                IntProvider.codec(4, 40).fieldOf("height").forGetter(Config::height),
                IntProvider.codec(1, 4).fieldOf("radius").forGetter(Config::radius)
        ).apply(instance, Config::new));
    }

    public SpiritSpireFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        // Needs solid footing under the whole base, or it is a spire over the void.
        int radius = config.radius().sample(random);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) continue;
                if (!level.getBlockState(origin.offset(dx, -1, dz)).isSolid()) return false;
            }
        }
        int height = config.height().sample(random);
        // A slight lean, so no two spires stand the same way.
        double leanX = (random.nextDouble() - 0.5) * 0.12;
        double leanZ = (random.nextDouble() - 0.5) * 0.12;
        int bandAt = 2 + random.nextInt(3);
        int placed = 0;
        int topY = origin.getY();
        for (int y = 0; y < height; y++) {
            // taper: full radius for the bottom third, then narrowing to a single block
            double t = y / (double) height;
            double r = radius * (t < 0.33 ? 1.0 : 1.0 - (t - 0.33) / 0.67) + 0.3;
            int cx = origin.getX() + (int) Math.round(leanX * y);
            int cz = origin.getZ() + (int) Math.round(leanZ * y);
            boolean any = false;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz) + (random.nextDouble() - 0.5) * 0.6;
                    if (d > r) continue;
                    BlockPos at = new BlockPos(cx + dx, origin.getY() + y, cz + dz);
                    if (!level.getBlockState(at).canBeReplaced()) continue;
                    BlockState state = (y >= bandAt && y < bandAt + 2 && d > r - 1.2)
                            ? config.band().getState(random, at)
                            : config.stone().getState(random, at);
                    level.setBlock(at, state, 2);
                    any = true;
                    placed++;
                    topY = at.getY();
                }
            }
            if (!any) break;
        }
        if (placed < 4) return false;
        // Moss on the lee side near the ground, and a glowing crown on the tip.
        for (int i = 0; i < radius * 3; i++) {
            BlockPos at = origin.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(3), random.nextInt(radius * 2 + 1) - radius);
            if (level.getBlockState(at).isSolid() && level.getBlockState(at.above()).isAir() && random.nextBoolean()) {
                level.setBlock(at.above(), config.moss().getState(random, at.above()), 2);
            }
        }
        BlockPos tip = new BlockPos(origin.getX() + (int) Math.round(leanX * (topY - origin.getY())), topY + 1,
                origin.getZ() + (int) Math.round(leanZ * (topY - origin.getY())));
        if (level.getBlockState(tip).isAir() && level.getBlockState(tip.below()).isSolid()) {
            level.setBlock(tip, config.crown().getState(random, tip), 2);
        }
        return true;
    }
}
