package com.reynn.wispwillow.blocks.fen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cluster of small glowing mushrooms.
 *
 * <p>Deliberately not a {@code MushroomBlock}: those spread in the dark and pop when the light
 * rises, and a decorative light source that wanders off or vanishes is no use to a builder.
 * Wispcaps stay exactly where they were put, on soil or on a log.</p>
 */
public class WispcapBlock extends BushBlock {
    public static final MapCodec<WispcapBlock> CODEC = simpleCodec(WispcapBlock::new);
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public WispcapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LOGS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) return;
        Vec3 offset = state.getOffset(level, pos);
        level.addParticle(ParticleTypes.GLOW,
                pos.getX() + 0.3 + random.nextDouble() * 0.4 + offset.x,
                pos.getY() + 0.45,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4 + offset.z,
                (random.nextDouble() - 0.5) * 0.01, 0.015, (random.nextDouble() - 0.5) * 0.01);
    }
}
