package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A flower that spends Fluxia to do work. One class covers all three blooms; the
 * behaviour lives in the {@link FunctionalBloomEntity} the factory produces.
 */
public class FunctionalBloomBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);

    private final BiFunction<BlockPos, BlockState, FunctionalBloomEntity> factory;
    private final Supplier<? extends BlockEntityType<?>> type;
    private final int particleColor;

    public FunctionalBloomBlock(Properties props,
                                BiFunction<BlockPos, BlockState, FunctionalBloomEntity> factory,
                                Supplier<? extends BlockEntityType<?>> type,
                                int particleColor) {
        super(props);
        this.factory = factory;
        this.type = type;
        this.particleColor = particleColor;
    }

    // simpleCodec needs a no-arg-ish constructor; these blocks are never serialised
    // from a codec because they are only ever placed from the registry.
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC_UNSUPPORTED;
    }

    private static final MapCodec<FunctionalBloomBlock> CODEC_UNSUPPORTED =
            MapCodec.unit(() -> {
                throw new UnsupportedOperationException("Functional blooms are registry-only");
            });

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> given) {
        if (level.isClientSide) return null;
        return given == type.get()
                ? (lvl, pos, st, be) -> FunctionalBloomEntity.tick(lvl, pos, st, (FunctionalBloomEntity) be)
                : null;
    }

    /** A working bloom hums; a starved one sits dark. Purely client-side. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FunctionalBloomEntity bloom)) return;
        if (!bloom.isWorking()) return;

        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.75, cz = pos.getZ() + 0.5;

        if (random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.15 + random.nextDouble() * 0.1;
            level.addParticle(ParticleTypes.END_ROD,
                    cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r,
                    0.0, 0.01 + random.nextDouble() * 0.015, 0.0);
        }

        // A faint ring marking the edge of the bloom's reach, so its area is legible.
        if (random.nextInt(6) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            int rad = bloom.radius();
            level.addParticle(
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(
                                    ((particleColor >> 16) & 0xFF) / 255f,
                                    ((particleColor >> 8) & 0xFF) / 255f,
                                    (particleColor & 0xFF) / 255f),
                            1.0f),
                    cx + Math.cos(a) * rad, pos.getY() + 0.3, cz + Math.sin(a) * rad,
                    0, 0.01, 0);
        }
    }

    /**
     * Links are stored only on the source node, so breaking this one would otherwise leave
     * whoever pointed at it holding a dead position — drawn by the goggles as a live link.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            FluxLinks.onNodeRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
