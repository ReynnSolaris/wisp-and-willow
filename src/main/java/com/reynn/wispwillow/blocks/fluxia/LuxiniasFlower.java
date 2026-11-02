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
import com.reynn.wispwillow.registry.ParticleRegistry;

import javax.annotation.Nullable;

public class LuxiniasFlower extends BaseEntityBlock {
    private static MapCodec<LuxiniasFlower> CODEC = simpleCodec(LuxiniasFlower::new);
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE;
    public LuxiniasFlower(Properties props) {
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
        return new LuxiniasFlowerEntity(pos, state);
    }

    /**
     * Burning flowers throw off sparks. Done client-side from the synced burn state, so a
     * field of flowers costs no packets at all.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof LuxiniasFlowerEntity flower)) return;
        if (!flower.isBurning()) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.55;
        double cz = pos.getZ() + 0.5;

        // A slow spiral rising out of the petals.
        double angle = (level.getGameTime() % 80) / 80.0 * Math.PI * 2.0;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            double r = 0.18 + random.nextDouble() * 0.06;
            level.addParticle(ParticleRegistry.FLUX_SPARK.get(),
                    cx + Math.cos(a) * r,
                    cy + random.nextDouble() * 0.25,
                    cz + Math.sin(a) * r,
                    0.0, 0.015 + random.nextDouble() * 0.01, 0.0);
        }

        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, cx, cy + 0.3, cz,
                    (random.nextDouble() - 0.5) * 0.01, 0.02, (random.nextDouble() - 0.5) * 0.01);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityRegistry.LUXINIAS_FLOWER.get()
                ? (lvl, pos, st, be) -> LuxiniasFlowerEntity.tick(lvl, pos, st, (LuxiniasFlowerEntity) be)
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
