package com.reynn.wispwillow.worldgen.tree;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The frame of a real willow: a short, stout bole that breaks into limbs arching up and out.
 *
 * <p>A willow is not a lollipop on a stick. Its branches leave the trunk low — around half its
 * height — and climb steeply at first, then flatten as they reach out, so the crown ends up
 * far wider than the tree is tall. Each limb ends in its own foliage attachment, which is what
 * lets {@link WeepingFoliagePlacer} hang a separate curtain off every one of them rather than a
 * single ring around the top.</p>
 */
public class WeepingWillowTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<WeepingWillowTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            trunkPlacerParts(instance).and(instance.group(
                    IntProvider.codec(1, 8).fieldOf("branch_count").forGetter(p -> p.branchCount),
                    IntProvider.codec(1, 10).fieldOf("branch_length").forGetter(p -> p.branchLength)
            )).apply(instance, WeepingWillowTrunkPlacer::new));

    /** Eight compass directions, so limbs do not all lie on the axes. */
    private static final int[][] SPOKES = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };

    private final IntProvider branchCount;
    private final IntProvider branchLength;

    public WeepingWillowTrunkPlacer(int baseHeight, int heightRandA, int heightRandB,
                                    IntProvider branchCount, IntProvider branchLength) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
        this.branchLength = branchLength;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return MainRegistry.WEEPING_WILLOW_TRUNK.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level,
                                                            BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int freeTreeHeight,
                                                            BlockPos pos, TreeConfiguration config) {
        setDirtAt(level, blockSetter, random, pos.below(), config);

        for (int y = 0; y < freeTreeHeight; y++) {
            placeLog(level, blockSetter, random, pos.above(y), config);
        }
        // A little weight at the foot, the way an old willow thickens where it meets the water.
        for (int[] spoke : SPOKES) {
            if (random.nextFloat() < 0.35F) {
                placeLog(level, blockSetter, random, pos.offset(spoke[0], 0, spoke[1]), config);
            }
        }

        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
        attachments.add(new FoliagePlacer.FoliageAttachment(pos.above(freeTreeHeight), 0, false));

        int branches = branchCount.sample(random);
        int lowest = Math.max(1, freeTreeHeight / 2);

        // Deal the limbs evenly around the compass instead of stepping two-to-four spokes at a
        // time. The old walk could advance by two twice running and put three limbs inside a
        // quarter turn, leaving the opposite side of the tree bare — which is what made the crown
        // look lopsided and clumped. Dividing the circle by the branch count guarantees every
        // quarter gets its share, and the jitter keeps them off a perfect star.
        int spokeStep = Math.max(1, SPOKES.length / Math.max(1, branches));
        int spoke = random.nextInt(SPOKES.length);
        for (int i = 0; i < branches; i++) {
            spoke = (spoke + spokeStep + random.nextInt(2)) % SPOKES.length;
            int[] dir = SPOKES[spoke];
            // Stagger the take-off heights across the usable trunk rather than drawing each one
            // independently, so limbs leave the bole at different levels instead of bunching.
            int span = Math.max(1, freeTreeHeight - lowest);
            int start = lowest + (i * span / Math.max(1, branches) + random.nextInt(2)) % span;
            int length = branchLength.sample(random);

            BlockPos tip = pos.above(start);
            for (int step = 1; step <= length; step++) {
                // Steep at first, flattening out: the arch a willow limb makes. Shallower than it
                // was, so a limb reaches further out for the height it gains and the crown ends up
                // wider than the tree is tall, which is the whole silhouette of a willow.
                int rise = (int) Math.round(Math.sqrt(step) * 0.75);
                BlockPos limb = pos.offset(dir[0] * step, start + rise, dir[1] * step);
                Direction.Axis axis = step == length ? Direction.Axis.Y
                        : dir[0] != 0 && dir[1] != 0 ? Direction.Axis.Y
                        : dir[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
                if (!placeLog(level, blockSetter, random, limb, config, state -> state.hasProperty(RotatedPillarBlock.AXIS)
                        ? state.setValue(RotatedPillarBlock.AXIS, axis) : state)) {
                    break;
                }
                tip = limb;
            }
            // fall through to the drape below
            if (!tip.equals(pos.above(start))) {
                attachments.add(new FoliagePlacer.FoliageAttachment(tip, 0, false));
                drape(level, blockSetter, random, tip, dir, config);
            }
        }
        return ImmutableList.copyOf(attachments);
    }

    /**
     * Short woody branches hanging down out of a limb tip, through the curtain it carries.
     *
     * <p>These are structural, not decorative. A leaf dies when it is more than six blocks from
     * the nearest log, and a willow's whole silhouette is leaves trailing well past that — so
     * without wood inside the curtain the ends of every strand simply decayed away a few minutes
     * after the tree generated, and the weeping went with them.</p>
     *
     * <p>Two or three of them per limb, offset around the tip rather than all on its axis, so the
     * curtain has wood scattered through it instead of one pole down the middle.</p>
     */
    private void drape(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                       RandomSource random, BlockPos tip, int[] dir, TreeConfiguration config) {
        int strands = 2 + random.nextInt(2);
        for (int s = 0; s < strands; s++) {
            // Spread along the limb and across it, so the wood sits inside the leaf curtain.
            int ox = s == 0 ? 0 : (random.nextInt(3) - 1) + dir[0];
            int oz = s == 0 ? 0 : (random.nextInt(3) - 1) + dir[1];
            BlockPos head = tip.offset(ox, 0, oz);

            // Deep enough to reach down inside the curtain. The leaves on a fen weeping tree are a
            // plain vanilla LeavesBlock, which dies past six blocks from wood, and its strands hang
            // further than that — so a token two-block stub left the ends dying anyway.
            int drop = 3 + random.nextInt(4);
            for (int d = 1; d <= drop; d++) {
                if (!placeLog(level, blockSetter, random, head.below(d), config,
                        state -> state.hasProperty(RotatedPillarBlock.AXIS)
                                ? state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : state)) {
                    break;
                }
            }
        }
    }
}
