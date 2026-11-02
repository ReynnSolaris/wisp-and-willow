package com.reynn.wispwillow.blocks.fen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Moss that hangs in long drapes from branches and ledges.
 *
 * <p>A drape is a column of these, each hanging from the one above. {@link #TIP} marks the
 * bottom block so it can wear the frayed end texture; it is recomputed whenever the block
 * underneath changes, so trimming a drape re-tips it rather than leaving a blunt cut.</p>
 */
public class FenMossBlock extends Block {
    public static final MapCodec<FenMossBlock> CODEC = simpleCodec(FenMossBlock::new);
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public FenMossBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        return above.is(this) || above.is(BlockTags.LEAVES) || above.isFaceSturdy(level, abovePos, Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(TIP, !context.getLevel().getBlockState(pos.below()).is(this));
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                     LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (facing == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (facing == Direction.DOWN) {
            return state.setValue(TIP, !facingState.is(this));
        }
        return state;
    }
}
