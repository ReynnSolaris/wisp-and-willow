package com.reynn.wispwillow.blocks;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.registry.FluidRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;

public class AmethystSugarCaneBlock extends Block {
    private static MapCodec<AmethystSugarCaneBlock> CODEC = simpleCodec(AmethystSugarCaneBlock::new);
    public static final IntegerProperty AGE;
    private static final float AABB_OFFSET = 6.0F;
    private static final VoxelShape SHAPE;

    public AmethystSugarCaneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));

    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public MapCodec<AmethystSugarCaneBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }

    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above())) {
            int i;
            for(i = 1; level.getBlockState(pos.below(i)).is(this); ++i) {
            }

            if (i < 3) {
                int j = state.getValue(AGE);
                if (CommonHooks.canCropGrow(level, pos, state, true)) {
                    if (j == 15) {
                        level.setBlockAndUpdate(pos.above(), this.defaultBlockState());
                        CommonHooks.fireCropGrowPost(level, pos.above(), this.defaultBlockState());
                        level.setBlock(pos, state.setValue(AGE, 0), 4);
                    } else {
                        level.setBlock(pos, state.setValue(AGE, j + 1), 4);
                    }
                }
            }
        }

    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (!state.canSurvive(level, currentPos)) {
            level.scheduleTick(currentPos, this, 1);
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.below());
        if (blockstate.is(this)) {
            return true;
        } else {
            TriState soilDecision = blockstate.canSustainPlant(level, pos.below(), Direction.UP, state);
            if (!soilDecision.isDefault()) {
                return soilDecision.isTrue();
            } else {
                if (blockstate.is(BlockTags.DIRT) || blockstate.is(BlockTags.SAND)) {
                    BlockPos blockpos = pos.below();

                    for(Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState blockstate1 = level.getBlockState(blockpos.relative(direction));
                        FluidState fluidstate = level.getFluidState(blockpos.relative(direction));
                        if (fluidstate.is(FluidRegistry.FLUXIA_FLUID.get())) {
                            if (state.canBeHydrated(level, pos, fluidstate, blockpos.relative(direction)) || blockstate1.is(Blocks.FROSTED_ICE)) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    static {
        AGE = BlockStateProperties.AGE_15;
        SHAPE = Block.box(2.0F, 0.0F, 2.0F, 14.0F, 16.0F, 14.0F);
    }
}
