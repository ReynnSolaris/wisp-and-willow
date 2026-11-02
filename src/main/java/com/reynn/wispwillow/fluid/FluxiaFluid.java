package com.reynn.wispwillow.fluid;

import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.FluidRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FluxiaFluid extends FlowingFluid {

    protected FluxiaFluid() {
        super();
    }

    @Override
    public Item getBucket() {
        return ItemRegistry.FLUXIA_FLUID_BUCKET.get();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockGetter, BlockPos blockPos, Fluid fluid, Direction direction) {
        return fluidState.getHeight(blockGetter, blockPos) >= 0.44444445F && fluid.is(FluidTags.WATER);
    }

    @Override
    public int getTickDelay(LevelReader worldIn) {
        return 30;
    }

    @Override
    protected float getExplosionResistance() {
        return 100f;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (!state.isSource() && !state.getValue(FALLING)) {
            if (random.nextInt(64) == 0) {
                level.playLocalSound(
                        (double)pos.getX() + 0.5,
                        (double)pos.getY() + 0.5,
                        (double)pos.getZ() + 0.5,
                        SoundEvents.WATER_AMBIENT,
                        SoundSource.BLOCKS,
                        random.nextFloat() * 0.25F + 0.75F,
                        random.nextFloat() + 0.5F,
                        false
                );
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(
                    ParticleTypes.UNDERWATER,
                    (double)pos.getX() + random.nextDouble(),
                    (double)pos.getY() + random.nextDouble(),
                    (double)pos.getZ() + random.nextDouble(),
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return BlockRegistry.FLUXIA_FLUID_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public Fluid getFlowing() {
        return FluidRegistry.FLOWING_FLUXIA_FLUID.get();
    }

    @Override
    public FluidType getFluidType() {
        return FluidRegistry.FLUXIA_FLUID_TYPE.get();
    }

    @Override
    public Fluid getSource() {
        return FluidRegistry.FLUXIA_FLUID.get();
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
    }

    @Override
    public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return canConvertToSource(level);
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockentity);
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == FluidRegistry.FLOWING_FLUXIA_FLUID.get() || fluid == FluidRegistry.FLUXIA_FLUID.get();
    }

    @Override
    protected int getSlopeFindDistance(LevelReader worldIn) {
        return 1;
    }

    @Override
    protected int getDropOff(LevelReader worldIn) {
        return 1;
    }

    @Override
    protected @Nullable ParticleOptions getDripParticle() {
        return super.getDripParticle();
    }

    public static class Flowing extends FluxiaFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends FluxiaFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
