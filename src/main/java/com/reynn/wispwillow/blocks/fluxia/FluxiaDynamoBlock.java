package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

/** Drop fuel on top; it burns it for Fluxia. See {@link FluxiaDynamoEntity}. */
public class FluxiaDynamoBlock extends BaseEntityBlock {

    public static final MapCodec<FluxiaDynamoBlock> CODEC = simpleCodec(FluxiaDynamoBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FluxiaDynamoBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxiaDynamoEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == BlockEntityRegistry.FLUXIA_DYNAMO.get()
                ? (lvl, pos, st, be) -> AbstractFluxiaGenerator.tick(lvl, pos, st, (FluxiaDynamoEntity) be)
                : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(2) != 0) return;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 1.0,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0.0, 0.03, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            FluxLinks.onNodeRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
