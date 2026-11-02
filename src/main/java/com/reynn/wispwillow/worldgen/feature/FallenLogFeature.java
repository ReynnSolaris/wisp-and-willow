package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree that came down a long time ago: a run of logs lying on their side, with moss and
 * mushrooms growing along the top.
 *
 * <p>Every segment has to rest on something sturdy, so a log never bridges a gap or hangs off
 * a bank. Shallow water is fine, which is how a fen gets its half-sunk trunks.</p>
 */
public class FallenLogFeature extends Feature<FallenLogFeature.Config> {

    public record Config(BlockStateProvider log, IntProvider length, BlockStateProvider decoration,
                         float decorationChance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("log").forGetter(Config::log),
                IntProvider.codec(1, 16).fieldOf("length").forGetter(Config::length),
                BlockStateProvider.CODEC.fieldOf("decoration").forGetter(Config::decoration),
                Codec.floatRange(0.0F, 1.0F).fieldOf("decoration_chance").forGetter(Config::decorationChance)
        ).apply(instance, Config::new));
    }

    public FallenLogFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        int length = config.length().sample(random);

        // Swamp ground is rarely level for six blocks in one given direction. Try all four, starting
        // from a random one, and lie down along the first that has room for at least a short trunk.
        List<BlockPos> run = List.of();
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int turn = 0; turn < 4 && run.size() < MIN_LENGTH; turn++) {
            direction = direction.getClockWise();
            run = clearRun(level, origin, direction, length);
        }
        if (run.size() < MIN_LENGTH) return false;

        for (BlockPos pos : run) {
            clearPlant(level, pos);
            BlockState log = config.log().getState(random, pos);
            if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                log = log.setValue(RotatedPillarBlock.AXIS, direction.getAxis());
            }
            level.setBlock(pos, log, 2);

            BlockPos above = pos.above();
            if (random.nextFloat() < config.decorationChance() && level.isEmptyBlock(above)) {
                BlockState decoration = config.decoration().getState(random, above);
                if (decoration.canSurvive(level, above)) {
                    level.setBlock(above, decoration, 2);
                }
            }
        }
        return true;
    }

    private static final int MIN_LENGTH = 3;

    /** How far a trunk can run from the origin before it would hang in the air or hit something. */
    private static List<BlockPos> clearRun(WorldGenLevel level, BlockPos origin, Direction direction, int length) {
        List<BlockPos> run = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            BlockPos pos = origin.relative(direction, i);
            BlockPos below = pos.below();
            if (!isClear(level.getBlockState(pos)) || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                break;
            }
            run.add(pos);
        }
        return run;
    }

    /** Air, water, or anything a player could build over — including water plants like seagrass. */
    private static boolean isClear(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.canBeReplaced();
    }

    /** Removing only the foot of a tall plant would leave its head floating. */
    private static void clearPlant(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoublePlantBlock && level.getBlockState(pos.above()).is(state.getBlock())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
