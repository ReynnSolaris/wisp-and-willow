package com.reynn.wispwillow.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class WillowTorchBlock extends TorchBlock {
    private final Supplier<? extends ParticleOptions> particleSupplier;

    public WillowTorchBlock(Supplier<? extends ParticleOptions> particleSupplier, Properties props) {
        // super needs *some* particle; we override animateTick anyway
        super(ParticleTypes.FLAME, props);
        this.particleSupplier = particleSupplier;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        double d0 = (double)pos.getX() + (double)0.5F;
        double d1 = (double)pos.getY() + 0.7;
        double d2 = (double)pos.getZ() + (double)0.5F;
        level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, (double)0.0F, (double)0.0F, (double)0.0F);
        ParticleOptions type = particleSupplier.get();
        level.addParticle(type, d0, d1, d2, 0.0D, 0.0D, 0.0D);
    }
}
