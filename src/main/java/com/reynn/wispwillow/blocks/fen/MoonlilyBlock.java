package com.reynn.wispwillow.blocks.fen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lily pad with a pale flower on it that holds a little light.
 *
 * <p>It behaves like a vanilla lily pad in every respect but two: it glows faintly, which is
 * what makes a fen pool readable at night, and after dark it lets off the occasional mote.
 * {@code WaterlilyBlock} pins its codec's generic to itself, so this is a sibling of it rather
 * than a subclass.</p>
 */
public class MoonlilyBlock extends BushBlock {
    public static final MapCodec<MoonlilyBlock> CODEC = simpleCodec(MoonlilyBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    public MoonlilyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level instanceof ServerLevel && entity instanceof Boat) {
            level.destroyBlock(new BlockPos(pos), true, entity);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        boolean water = level.getFluidState(pos).getType() == Fluids.WATER || state.getBlock() instanceof IceBlock;
        return water && level.getFluidState(pos.above()).getType() == Fluids.EMPTY;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isNight() || random.nextInt(18) != 0) return;
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + 0.35 + random.nextDouble() * 0.3,
                pos.getY() + 0.2,
                pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                0.0, 0.012, 0.0);
    }
}
