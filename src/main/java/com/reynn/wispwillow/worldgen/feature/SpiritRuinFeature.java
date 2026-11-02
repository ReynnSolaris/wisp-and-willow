package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * A ring of standing stones, most of them broken, with lintels still bridging a few.
 *
 * <p>The Spirit Realm reads as empty because nothing in it has an edge or a vertical: island, grass,
 * the odd tree, void. These give the eye something to find at a distance and something to stand
 * inside when it gets there, and because the same feature builds both the small roadside cairns and
 * the large landmarks — the only difference is radius and height — the realm's ruins all look like
 * they were left by the same hands.</p>
 *
 * <p>Built rather than pasted from a structure template on purpose. Floating islands have no
 * guaranteed flat ground and no guaranteed extent, so a fixed template would hang off an edge as
 * often as not. Every pillar here finds its own footing, and any that comes up over the void is
 * simply not built — a ruin that runs off the side of an island stops at the side of the island.</p>
 */
public class SpiritRuinFeature extends Feature<SpiritRuinFeature.Config> {

    public record Config(BlockStateProvider stone, BlockStateProvider accent, BlockStateProvider crown,
                         IntProvider radius, IntProvider height, float brokenChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("stone").forGetter(Config::stone),
                BlockStateProvider.CODEC.fieldOf("accent").forGetter(Config::accent),
                BlockStateProvider.CODEC.fieldOf("crown").forGetter(Config::crown),
                IntProvider.codec(2, 12).fieldOf("radius").forGetter(Config::radius),
                IntProvider.codec(2, 16).fieldOf("height").forGetter(Config::height),
                Codec.floatRange(0.0F, 1.0F).fieldOf("broken_chance").forGetter(Config::brokenChance)
        ).apply(instance, Config::new));
    }

    /** Below this many pillars actually standing, it reads as litter rather than as a ruin. */
    private static final int MIN_PILLARS = 3;

    public SpiritRuinFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        int radius = config.radius().sample(random);
        int count = Math.max(4, radius + 2 + random.nextInt(3));

        int[] px = new int[count];
        int[] pz = new int[count];
        int[] py = new int[count];
        int[] ph = new int[count];
        boolean[] standing = new boolean[count];
        int raised = 0;

        // Lay the ring out first and only build once it is clear enough of it will stand. A half
        // built ring that then fails its own threshold would leave stumps behind with no way to
        // take them back out.
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count + random.nextDouble() * 0.15;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);

            px[i] = x;
            pz[i] = z;
            py[i] = y;

            // Over the void the heightmap bottoms out; there is nothing here to stand a stone on.
            if (y <= level.getMinBuildHeight() + 1) continue;
            if (!level.getBlockState(new BlockPos(x, y - 1, z)).isSolid()) continue;
            // Too far below the ring's own floor means this one is down a cliff, not on the island.
            if (Math.abs(y - origin.getY()) > 4) continue;

            int h = config.height().sample(random);
            if (random.nextFloat() < config.brokenChance()) {
                h = Math.max(1, h - 1 - random.nextInt(Math.max(1, h)));
            }
            ph[i] = h;
            standing[i] = true;
            if (h >= 2) raised++;
        }

        if (raised < MIN_PILLARS) return false;

        for (int i = 0; i < count; i++) {
            if (!standing[i]) continue;
            BlockPos foot = new BlockPos(px[i], py[i], pz[i]);
            for (int dy = 0; dy < ph[i]; dy++) {
                BlockPos at = foot.above(dy);
                if (!level.getBlockState(at).canBeReplaced()) break;
                level.setBlock(at, config.stone().getState(random, at), 2);
            }
        }

        // Lintels. Only between neighbours that both reached full height and are close enough to
        // span, so what survives looks like the parts of a colonnade that happened not to fall.
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            if (!standing[i] || !standing[j]) continue;
            if (ph[i] < 3 || ph[j] < 3) continue;
            if (py[i] + ph[i] != py[j] + ph[j]) continue;
            if (random.nextFloat() < 0.45F) continue;

            int dx = px[j] - px[i];
            int dz = pz[j] - pz[i];
            int span = Math.max(Math.abs(dx), Math.abs(dz));
            if (span > 4) continue;

            int topY = py[i] + ph[i];
            for (int s = 0; s <= span; s++) {
                int x = px[i] + Math.round((float) dx * s / span);
                int z = pz[i] + Math.round((float) dz * s / span);
                BlockPos at = new BlockPos(x, topY, z);
                if (!level.getBlockState(at).canBeReplaced()) continue;
                level.setBlock(at, config.stone().getState(random, at), 2);
            }
        }

        // The middle: a small floor of the accent, and one growing thing on it. The flower is what
        // makes a ruin read as tended rather than as rubble.
        int floorY = origin.getY();
        boolean planted = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos at = new BlockPos(origin.getX() + dx, floorY - 1, origin.getZ() + dz);
                if (level.getBlockState(at).isAir()) continue;
                level.setBlock(at, config.accent().getState(random, at), 2);

                BlockPos above = at.above();
                if (!planted && dx == 0 && dz == 0 && level.getBlockState(above).canBeReplaced()) {
                    BlockState crown = config.crown().getState(random, above);
                    if (crown.canSurvive(level, above)) {
                        level.setBlock(above, crown, 2);
                        planted = true;
                    }
                }
            }
        }
        return true;
    }

    /** Kept out of the ring loop: a pillar footing may legitimately be moss or grass, not stone. */
    @SuppressWarnings("unused")
    private static boolean isFooting(BlockState state) {
        return state.isSolid() && !state.is(Blocks.WATER);
    }
}
