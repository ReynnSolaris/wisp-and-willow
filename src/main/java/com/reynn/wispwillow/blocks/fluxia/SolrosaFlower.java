package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
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

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

public class SolrosaFlower extends BaseEntityBlock {
    private static MapCodec<SolrosaFlower> CODEC = simpleCodec(SolrosaFlower::new);
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE;
    public SolrosaFlower(Properties props) {
        super(props);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 vec3 = state.getOffset(level, pos);
        return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    static {
        SHAPE = Block.box((double)5.0F, (double)0.0F, (double)5.0F, (double)11.0F, (double)10.0F, (double)11.0F);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolrosaFlowerEntity(pos, state);
    }

    /** A quiet shimmer: Solrosa always produces, so it always glimmers a little. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.ENCHANT,
                pos.getX() + 0.3 + random.nextDouble() * 0.4,
                pos.getY() + 0.8 + random.nextDouble() * 0.3,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                (random.nextDouble() - 0.5) * 0.02, -0.02, (random.nextDouble() - 0.5) * 0.02);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityRegistry.SOLROSA_FLOWER.get()
                ? (lvl, pos, st, be) -> SolrosaFlowerEntity.tick(lvl, pos, st, (SolrosaFlowerEntity) be)
                : null;
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
