package com.reynn.wispwillow.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/**
 * The floating crown: a squashed sphere of leaves, eaten through with holes, trailing wisps.
 *
 * <p>A blob placer builds a solid lump, which is the right shape for an oak and the wrong one for
 * something that is supposed to look half here. This one is built as a spheroid so it has no flat
 * underside — the crown is seen from below far more than a normal tree's is, because it hangs over
 * a gap you can walk under — and then deliberately holed, so the sky shows through it and it reads
 * as a cloud of leaves rather than a solid green ball.</p>
 *
 * <p>The wisps hanging off the underside are the one nod to the fen willows across the worlds: a
 * few short strands, not a curtain, because the silhouette here is the floating mass and anything
 * longer starts to look like a rope holding it up.</p>
 */
public class SoulwoodFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<SoulwoodFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).and(instance.group(
                    IntProvider.codec(1, 5).fieldOf("height").forGetter(p -> p.height),
                    IntProvider.codec(0, 6).fieldOf("wisp_length").forGetter(p -> p.wispLength),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("wisp_chance").forGetter(p -> p.wispChance),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("hole_chance").forGetter(p -> p.holeChance)
            )).apply(instance, SoulwoodFoliagePlacer::new));

    private final IntProvider height;
    private final IntProvider wispLength;
    private final float wispChance;
    private final float holeChance;

    public SoulwoodFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider height,
                                 IntProvider wispLength, float wispChance, float holeChance) {
        super(radius, offset);
        this.height = height;
        this.wispLength = wispLength;
        this.wispChance = wispChance;
        this.holeChance = holeChance;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return MainRegistry.SOULWOOD_FOLIAGE.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
                                 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {
        boolean large = attachment.doubleTrunk();
        BlockPos centre = attachment.pos().above(offset);
        int radius = Math.max(foliageRadius + attachment.radiusOffset(), 1);

        // Rows from the bottom of the crown to the top, the radius following a circle so the mass
        // closes underneath as well as over. Half-height above and below the centre, which makes it
        // wider than it is tall — a crown, not a ball.
        for (int y = -foliageHeight; y <= foliageHeight; y++) {
            double t = (double) Math.abs(y) / (foliageHeight + 1.0);
            int ring = (int) Math.round(radius * Math.sqrt(Math.max(0.0, 1.0 - t * t)));
            placeLeavesRow(level, setter, random, config, centre, ring, y, large);
        }

        // Wisps off the underside, hung from the widest part of the belly rather than the very
        // bottom, so they trail past the curve of the crown instead of out of a single point.
        int belly = -foliageHeight + 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) < radius - 1) continue;
                if (random.nextFloat() >= wispChance) continue;

                BlockPos start = centre.offset(dx, belly, dz);
                int length = wispLength.sample(random);
                for (int i = 1; i <= length; i++) {
                    if (!tryPlaceLeaf(level, setter, random, config, start.below(i))) break;
                }
            }
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return this.height.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        // Corners off, as every placer does, and then holes punched right through the body of the
        // crown. A solid canopy lit from inside looks like a lantern; a holed one looks haunted.
        // Guarded on range > 0. The crown's top and bottom rows shrink to a radius of zero, and
        // there the corner test reads localX == 0 && localZ == 0 — the centre column — so the cap
        // and the floor of the canopy were being skipped every single time and the crown came out
        // open at both poles.
        if (range > 0 && localX == range && localZ == range) return true;
        if (range > 1 && localX + localZ > range * 2 - 2 && random.nextFloat() < 0.5F) return true;
        return random.nextFloat() < holeChance;
    }
}
