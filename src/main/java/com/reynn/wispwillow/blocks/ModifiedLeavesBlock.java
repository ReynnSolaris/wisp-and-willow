package com.reynn.wispwillow.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.IShearable;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public class ModifiedLeavesBlock extends Block implements SimpleWaterloggedBlock, IShearable {
    public static final int DECAY_DISTANCE = 14;
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, DECAY_DISTANCE);
    public static final BooleanProperty PERSISTENT;
    public static final BooleanProperty WATERLOGGED;
    public static MapCodec<ModifiedLeavesBlock> CODEC = simpleCodec(ModifiedLeavesBlock::new);

    public ModifiedLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, DECAY_DISTANCE).setValue(PERSISTENT, false).setValue(WATERLOGGED, false));
    }

    @Override
    public @NotNull MapCodec<ModifiedLeavesBlock> codec() {
        return CODEC;
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.decaying(state)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }

    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(DISTANCE) == DECAY_DISTANCE && !state.getValue(PERSISTENT);
    }

    protected boolean decaying(BlockState state) {
        return !(Boolean)state.getValue(PERSISTENT) && state.getValue(DISTANCE) == DECAY_DISTANCE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PERSISTENT, DISTANCE, WATERLOGGED);
    }

    private static BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        int i = DECAY_DISTANCE;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(Direction direction : Direction.values()) {
            blockpos$mutableblockpos.setWithOffset(pos, direction);
            i = Math.min(i, getDistanceAt(level.getBlockState(blockpos$mutableblockpos)) + 1);
            if (i == 1) {
                break;
            }
        }

        return state.setValue(DISTANCE, i);
    }


    private static int getDistanceAt(BlockState neighbor) {
        return getOptionalDistanceAt(neighbor).orElse(DECAY_DISTANCE);
    }

    public static OptionalInt getOptionalDistanceAt(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return OptionalInt.of(0);
        } else {
            return state.hasProperty(DISTANCE) ? OptionalInt.of(state.getValue(DISTANCE)) : OptionalInt.empty();
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, updateDistance(state, level, pos), 3);
    }

    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return Shapes.empty();
    }


    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;
    }

    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        int i = getDistanceAt(facingState) + 1;
        if (i != 1 || (Integer)state.getValue(DISTANCE) != i) {
            level.scheduleTick(currentPos, this, 1);
        }

        return state;
    }


    protected FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above()) && random.nextInt(15) == 1) {
            BlockPos blockpos = pos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            if (!blockstate.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, Direction.UP)) {
                ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_WATER);
            }
        }

    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        BlockState blockstate = (BlockState)((BlockState)this.defaultBlockState().setValue(PERSISTENT, true)).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
        return updateDistance(blockstate, context.getLevel(), context.getClickedPos());
    }

    static {
        PERSISTENT = BlockStateProperties.PERSISTENT;
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
    }
}
