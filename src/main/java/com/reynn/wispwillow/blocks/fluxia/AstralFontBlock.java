package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Wants nothing but an open sky, and pays best at night. See {@link AstralFontEntity}. */
public class AstralFontBlock extends BaseEntityBlock {

    public static final MapCodec<AstralFontBlock> CODEC = simpleCodec(AstralFontBlock::new);

    public AstralFontBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AstralFontEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == BlockEntityRegistry.ASTRAL_FONT.get()
                ? (lvl, pos, st, be) -> AbstractFluxiaGenerator.tick(lvl, pos, st, (AstralFontEntity) be)
                : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.canSeeSky(pos.above()) || random.nextInt(4) != 0) return;
        // Motes falling in, rather than rising out: it is drinking, not burning.
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                pos.getY() + 1.6,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                0.0, -0.04, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            FluxLinks.onNodeRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
