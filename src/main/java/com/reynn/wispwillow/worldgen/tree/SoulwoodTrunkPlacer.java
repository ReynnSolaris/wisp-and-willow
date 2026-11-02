package com.reynn.wispwillow.worldgen.tree;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
 * The soulwood's trunk: pale wood that rises the whole way into its crown, with buttress
 * roots at the foot and one of three habits above them.
 *
 * <ul>
 *   <li><b>Straight</b> — a single bole to the crown, with a short side branch under it
 *       carrying a small crown of its own, so no two trees are the same silhouette.</li>
 *   <li><b>Forked</b> — the bole climbs a little past half height and splits into two or
 *       three limbs that lean apart, each ending in a crown. The ancient groves are mostly
 *       these.</li>
 *   <li><b>Bent</b> — the bole steps sideways once or twice on the way up, as if leaned on by
 *       a wind that never stops blowing here, and carries one crown at the top.</li>
 * </ul>
 *
 * <p>Every log is continuous with the ground: a limb that moves sideways lays a horizontal log
 * along its direction of travel before it climbs again, so the wood reads as grown, not
 * stacked. The thick variant (ancient trees) grows a 2x2 bole and roots two blocks tall.</p>
 */
public class SoulwoodTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<SoulwoodTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            trunkPlacerParts(instance).and(instance.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("fork_chance").forGetter(p -> p.forkChance),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("bend_chance").forGetter(p -> p.bendChance),
                    Codec.BOOL.fieldOf("thick").forGetter(p -> p.thick)
            )).apply(instance, SoulwoodTrunkPlacer::new));

    /** Eight compass directions, for roots and limbs. */
    private static final int[][] SPOKES = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };

    private final float forkChance;
    private final float bendChance;
    private final boolean thick;

    public SoulwoodTrunkPlacer(int baseHeight, int heightRandA, int heightRandB,
                               float forkChance, float bendChance, boolean thick) {
        super(baseHeight, heightRandA, heightRandB);
        this.forkChance = forkChance;
        this.bendChance = bendChance;
        this.thick = thick;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return MainRegistry.SOULWOOD_TRUNK.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level,
                                                           BiConsumer<BlockPos, BlockState> blockSetter,
                                                           RandomSource random, int freeTreeHeight,
                                                           BlockPos pos, TreeConfiguration config) {
        List<FoliagePlacer.FoliageAttachment> crowns = new ArrayList<>();
        roots(level, blockSetter, random, pos, config);

        float roll = random.nextFloat();
        if (roll < forkChance) {
            forked(level, blockSetter, random, freeTreeHeight, pos, config, crowns);
        } else if (roll < forkChance + bendChance) {
            bent(level, blockSetter, random, freeTreeHeight, pos, config, crowns);
        } else {
            straight(level, blockSetter, random, freeTreeHeight, pos, config, crowns);
        }
        return ImmutableList.copyOf(crowns);
    }

    // ---- the three habits -----------------------------------------------------------------

    private void straight(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                          int height, BlockPos pos, TreeConfiguration config, List<FoliagePlacer.FoliageAttachment> crowns) {
        bole(level, setter, random, pos, 0, height, config);
        crowns.add(new FoliagePlacer.FoliageAttachment(pos.above(height), thick ? 1 : 0, thick));
        // one side branch a few blocks under the crown, carrying a small crown of its own
        if (height >= 6) {
            int[] dir = SPOKES[random.nextInt(SPOKES.length)];
            int at = height - 3 - random.nextInt(2);
            BlockPos tip = limb(level, setter, random, pos.above(at), dir, 2 + random.nextInt(2), 1, config);
            crowns.add(new FoliagePlacer.FoliageAttachment(tip, -2, false));
        }
    }

    private void forked(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                        int height, BlockPos pos, TreeConfiguration config, List<FoliagePlacer.FoliageAttachment> crowns) {
        int split = Math.max(3, (int) Math.round(height * (0.5 + random.nextFloat() * 0.15)));
        bole(level, setter, random, pos, 0, split, config);
        int limbs = 2 + (random.nextFloat() < 0.4F ? 1 : 0);
        int spoke = random.nextInt(SPOKES.length);
        BlockPos top = pos.above(split - 1);
        for (int i = 0; i < limbs; i++) {
            spoke = (spoke + (limbs == 2 ? 4 : 3) + random.nextInt(limbs == 2 ? 1 : 2)) % SPOKES.length;
            int[] dir = SPOKES[spoke];
            int rise = height - split + random.nextInt(2);
            int reach = 2 + random.nextInt(2);
            BlockPos start = thick ? top.offset(Math.max(0, dir[0]), 0, Math.max(0, dir[1])) : top;
            BlockPos tip = limb(level, setter, random, start, dir, reach, rise, config);
            crowns.add(new FoliagePlacer.FoliageAttachment(tip, thick ? 0 : -1, false));
        }
    }

    private void bent(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                      int height, BlockPos pos, TreeConfiguration config, List<FoliagePlacer.FoliageAttachment> crowns) {
        int first = Math.max(2, (int) Math.round(height * 0.4));
        int second = Math.max(first + 2, (int) Math.round(height * 0.75));
        int[] dir = SPOKES[random.nextInt(SPOKES.length) & 6];       // a cardinal direction
        bole(level, setter, random, pos, 0, first, config);
        BlockPos p = step(level, setter, random, pos.above(first - 1), dir, config);
        bole(level, setter, random, p, 1, second - first + 1, config);
        p = p.above(second - first);
        if (random.nextFloat() < 0.5F) {
            // lean back the other way, or carry on into the wind
            int[] back = random.nextBoolean() ? new int[]{-dir[0], -dir[1]} : dir;
            p = step(level, setter, random, p, back, config);
            bole(level, setter, random, p, 1, height - second, config);
            p = p.above(height - second - 1);
        } else {
            bole(level, setter, random, p, 1, height - second, config);
            p = p.above(height - second);
        }
        crowns.add(new FoliagePlacer.FoliageAttachment(p.above(), thick ? 1 : 0, thick));
    }

    // ---- pieces -------------------------------------------------------------------------------

    /** Logs from {@code from} (inclusive) for {@code count} blocks straight up; 2x2 when thick. */
    private void bole(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                      BlockPos base, int from, int count, TreeConfiguration config) {
        for (int y = from; y < from + count; y++) {
            placeLog(level, setter, random, base.above(y), config);
            if (thick) {
                placeLog(level, setter, random, base.offset(1, y, 0), config);
                placeLog(level, setter, random, base.offset(0, y, 1), config);
                placeLog(level, setter, random, base.offset(1, y, 1), config);
            }
        }
    }

    /**
     * A sideways step of the bole: a horizontal log laid in the direction of travel from the top
     * of the column, so the next column stands on wood. Returns the base of the next column.
     */
    private BlockPos step(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                          BlockPos top, int[] dir, TreeConfiguration config) {
        BlockPos next = top.offset(dir[0], 0, dir[1]);
        placeLog(level, setter, random, next, config, axis(dir));
        if (thick) {
            placeLog(level, setter, random, next.offset(1, 0, 0), config, axis(dir));
            placeLog(level, setter, random, next.offset(0, 0, 1), config, axis(dir));
            placeLog(level, setter, random, next.offset(1, 0, 1), config, axis(dir));
        }
        return next;
    }

    /**
     * A limb leaving the bole at {@code from} in direction {@code dir}: it moves out {@code reach}
     * blocks while climbing {@code rise}, alternating sideways and upward logs so the limb is
     * continuous and reads as a branch. Returns the tip, where a crown attaches.
     */
    private BlockPos limb(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                          BlockPos from, int[] dir, int reach, int rise, TreeConfiguration config) {
        BlockPos p = from;
        int out = 0, up = 0;
        while (out < reach || up < rise) {
            boolean goOut = out < reach && (up >= rise || random.nextFloat() < (float) reach / (reach + rise));
            if (goOut) {
                p = p.offset(dir[0], 0, dir[1]);
                placeLog(level, setter, random, p, config, axis(dir));
                out++;
            } else {
                p = p.above();
                placeLog(level, setter, random, p, config);
                up++;
            }
        }
        return p;
    }

    /** Buttress roots: logs spreading from the foot, two high on the thick trees. */
    private void roots(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
                       BlockPos pos, TreeConfiguration config) {
        setDirtAt(level, setter, random, pos.below(), config);
        if (thick) {
            setDirtAt(level, setter, random, pos.offset(1, -1, 0), config);
            setDirtAt(level, setter, random, pos.offset(0, -1, 1), config);
            setDirtAt(level, setter, random, pos.offset(1, -1, 1), config);
        }
        for (int[] spoke : SPOKES) {
            if (random.nextFloat() >= (thick ? 0.6F : 0.35F)) continue;
            BlockPos foot = pos.offset(spoke[0] > 0 && thick ? 2 : spoke[0], 0, spoke[1] > 0 && thick ? 2 : spoke[1]);
            placeLog(level, setter, random, foot, config);
            if (thick && random.nextFloat() < 0.5F) placeLog(level, setter, random, foot.above(), config);
        }
    }

    private static java.util.function.Function<BlockState, BlockState> axis(int[] dir) {
        return state -> state.hasProperty(RotatedPillarBlock.AXIS)
                ? state.setValue(RotatedPillarBlock.AXIS,
                dir[0] != 0 && dir[1] == 0 ? Direction.Axis.X : dir[1] != 0 && dir[0] == 0 ? Direction.Axis.Z : Direction.Axis.Y)
                : state;
    }
}
