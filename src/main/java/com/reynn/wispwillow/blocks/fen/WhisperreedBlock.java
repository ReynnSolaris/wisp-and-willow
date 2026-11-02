package com.reynn.wispwillow.blocks.fen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/**
 * A two-tall reed that is happy with its feet in the water.
 *
 * <p>Vanilla's tall plants cannot be waterlogged, which is exactly where a reed wants to be, so
 * the lower half carries {@link #WATERLOGGED}. The head always has to be in open air: a reed
 * stands in shallow water, it does not grow under it, and that one rule is what keeps them to
 * the shoreline instead of carpeting the bottom of every swamp pool.</p>
 */
public class WhisperreedBlock extends DoublePlantBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<WhisperreedBlock> CODEC = simpleCodec(WhisperreedBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public WhisperreedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // BlockTags.DIRT already covers mud, moss, grass and peat.
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (!level.getFluidState(pos.above()).isEmpty()) return null;

        BlockState state = super.getStateForPlacement(context);
        return state == null ? null
                : state.setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                     LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        BlockState updated = super.updateShape(state, facing, facingState, level, pos, facingPos);
        // Losing the head should leave the water the foot was standing in, not a hole in the pool.
        return updated.isAir() && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : updated;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, WATERLOGGED);
    }
}
