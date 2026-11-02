package com.reynn.wispwillow.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class WillowWallTorchBlock extends WallTorchBlock {
    private final Supplier<? extends ParticleOptions> particleSupplier;

    public WillowWallTorchBlock(Supplier<? extends ParticleOptions> particleSupplier, Properties props) {
        super(ParticleTypes.FLAME, props);
        this.particleSupplier = particleSupplier;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        Direction dir = state.getValue(FACING);
        double d0 = (double)pos.getX() + (double)0.5F;
        double d1 = (double)pos.getY() + 0.7;
        double d2 = (double)pos.getZ() + (double)0.5F;
        double d3 = 0.22;
        double d4 = 0.27;
        Direction direction1 = dir.getOpposite();
        level.addParticle(ParticleTypes.SMOKE, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), (double)0.0F, (double)0.0F, (double)0.0F);
        ParticleOptions type = particleSupplier.get();
        level.addParticle(type, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), (double)0.0F, (double)0.0F, (double)0.0F);
    }
}
