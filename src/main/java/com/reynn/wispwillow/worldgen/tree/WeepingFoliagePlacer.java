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
 * The curtain: a shallow cap of leaves with long strands trailing from its rim.
 *
 * <p>Vanilla's cherry placer hangs leaves one or two blocks below the canopy, which reads as a
 * fringe. A willow's whole silhouette is the trailing, so the strands here run from the rim
 * down toward the ground — longest at the outside edge, where the limb has reached furthest —
 * and stop early against anything in the way.</p>
 */
public class WeepingFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<WeepingFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).and(instance.group(
                    IntProvider.codec(0, 6).fieldOf("height").forGetter(p -> p.height),
                    IntProvider.codec(1, 16).fieldOf("strand_length").forGetter(p -> p.strandLength),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(p -> p.strandChance)
            )).apply(instance, WeepingFoliagePlacer::new));

    private final IntProvider height;
    private final IntProvider strandLength;
    private final float strandChance;

    public WeepingFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider height,
                                IntProvider strandLength, float strandChance) {
        super(radius, offset);
        this.height = height;
        this.strandLength = strandLength;
        this.strandChance = strandChance;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return MainRegistry.WEEPING_FOLIAGE.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
                                 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {
        boolean large = attachment.doubleTrunk();
        BlockPos centre = attachment.pos().above(offset);
        int radius = Math.max(foliageRadius + attachment.radiusOffset(), 1);

        // A cap that is wider than it is tall, and deliberately thin: the willow's shape lives in
        // the trailing strands below, so the crown only needs enough leaf to read as a canopy.
        placeLeavesRow(level, setter, random, config, centre, radius - 2, 1, large);
        for (int y = 0; y >= -foliageHeight; y--) {
            placeLeavesRow(level, setter, random, config, centre, radius, y, large);
        }
        placeLeavesRow(level, setter, random, config, centre, radius - 2, -foliageHeight - 1, large);

        // The trailing strands, hung from the outermost ring only. Hanging them from the two
        // outer rings meant most of the cap was trailing something, and the tree came out as a
        // solid green curtain: one willow measured over a thousand leaf blocks.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                if (ring < radius || random.nextFloat() >= strandChance) continue;

                // Longest at the outer edge, shorter where the strand starts further in.
                int length = strandLength.sample(random) - (radius - ring);
                BlockPos start = centre.offset(dx, -foliageHeight - 1, dz);
                for (int i = 0; i < length; i++) {
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
        // Round the corners off, harder at the top so the cap domes, and punch holes along the
        // rim so light comes through the canopy instead of it reading as a solid green lid.
        if (localX == range && localZ == range) return true;
        if (localX == range || localZ == range) return random.nextFloat() < 0.4F;
        return localX + localZ > range * 2 - 2 && random.nextFloat() < (localY > 0 ? 0.75F : 0.25F);
    }
}
