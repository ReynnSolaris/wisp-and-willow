package com.reynn.wispwillow.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A small sunken pool cut into flat ground, lined with a soft bed and fringed with plants.
 *
 * <p>Vanilla's lake feature is deprecated and happily spills over a slope. This one only keeps
 * the cells it can actually contain: any cell on the edge of the pool whose neighbour is not
 * solid ground is dropped, a few passes over, so what is left always holds its liquid and
 * nothing is ever left flowing down a hillside.</p>
 */
public class BogPoolFeature extends Feature<BogPoolFeature.Config> {

    public record Config(BlockStateProvider fluid, BlockStateProvider bed, BlockStateProvider shorePlant,
                         float plantChance, IntProvider radius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("fluid").forGetter(Config::fluid),
                BlockStateProvider.CODEC.fieldOf("bed").forGetter(Config::bed),
                BlockStateProvider.CODEC.fieldOf("shore_plant").forGetter(Config::shorePlant),
                Codec.floatRange(0.0F, 1.0F).fieldOf("plant_chance").forGetter(Config::plantChance),
                IntProvider.codec(1, 6).fieldOf("radius").forGetter(Config::radius)
        ).apply(instance, Config::new));
    }

    private static final int MIN_CELLS = 4;
    private static final double DEEP_BELOW = 0.45;

    public BogPoolFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int groundY = origin.getY() - 1;

        if (!isSoil(level.getBlockState(origin.below()))) return false;

        int rx = config.radius().sample(random);
        int rz = config.radius().sample(random);
        int r = Math.max(rx, rz) + 1;
        int size = 2 * r + 1;
        double[][] depth = new double[size][size];
        boolean[][] mask = new boolean[size][size];

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double nx = dx / (rx + 0.5);
                double nz = dz / (rz + 0.5);
                double d = nx * nx + nz * nz + random.nextDouble() * 0.3;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (d > 1.0) continue;
                // The ground has to be level with the centre: soil here, open air (or a plant) above.
                // Checked block by block rather than off a heightmap, so /place feature works too.
                BlockPos ground = new BlockPos(x, groundY, z);
                if (!isSoil(level.getBlockState(ground)) || !isOpen(level.getBlockState(ground.above()))) continue;
                mask[dx + r][dz + r] = true;
                depth[dx + r][dz + r] = d;
            }
        }

        // Erode anything that would leak. Each pass can expose a new edge, so repeat.
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (!mask[i][j]) continue;
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int ni = i + dir.getStepX();
                        int nj = j + dir.getStepZ();
                        if (ni >= 0 && nj >= 0 && ni < size && nj < size && mask[ni][nj]) continue;
                        BlockState wall = level.getBlockState(new BlockPos(origin.getX() + ni - r, groundY, origin.getZ() + nj - r));
                        if (!wall.isSolid() || !wall.getFluidState().isEmpty()) {
                            mask[i][j] = false;
                            break;
                        }
                    }
                }
            }
        }

        int cells = 0;
        for (boolean[] row : mask) for (boolean cell : row) if (cell) cells++;
        if (cells < MIN_CELLS) return false;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!mask[i][j]) continue;
                BlockPos top = new BlockPos(origin.getX() + i - r, groundY, origin.getZ() + j - r);
                clearAbove(level, top.above());
                BlockState fluid = config.fluid().getState(random, top);
                level.setBlock(top, fluid, 2);
                if (depth[i][j] < DEEP_BELOW) {
                    level.setBlock(top.below(), fluid, 2);
                    level.setBlock(top.below(2), config.bed().getState(random, top.below(2)), 2);
                } else {
                    level.setBlock(top.below(), config.bed().getState(random, top.below()), 2);
                }
            }
        }

        // A muddy, reedy shore where the pool meets grass.
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!mask[i][j]) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int ni = i + dir.getStepX();
                    int nj = j + dir.getStepZ();
                    if (ni >= 0 && nj >= 0 && ni < size && nj < size && mask[ni][nj]) continue;
                    BlockPos shore = new BlockPos(origin.getX() + ni - r, groundY, origin.getZ() + nj - r);
                    if (level.getBlockState(shore).is(Blocks.GRASS_BLOCK) && random.nextBoolean()) {
                        level.setBlock(shore, config.bed().getState(random, shore), 2);
                    }
                    plantShore(level, random, config, shore.above());
                }
            }
        }
        return true;
    }

    private static void plantShore(WorldGenLevel level, RandomSource random, Config config, BlockPos pos) {
        if (random.nextFloat() >= config.plantChance() || !level.isEmptyBlock(pos)) return;
        BlockState plant = config.shorePlant().getState(random, pos);
        if (!plant.canSurvive(level, pos)) return;
        if (plant.getBlock() instanceof DoublePlantBlock) {
            if (level.isEmptyBlock(pos.above())) DoublePlantBlock.placeAt(level, plant, pos, 2);
        } else {
            level.setBlock(pos, plant, 2);
        }
    }

    private static void clearAbove(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.canBeReplaced()) return;
        if (state.getBlock() instanceof DoublePlantBlock && level.getBlockState(pos.above()).is(state.getBlock())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(BlockTags.DIRT);
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }
}
