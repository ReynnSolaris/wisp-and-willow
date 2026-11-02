package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The sheet inside an open Soul Gateway.
 *
 * <p>Placed and removed by {@link GatewayCoreBlockEntity}, never by a player — it has no item
 * and no loot. Like a nether portal it has no collision, so a thrown pact falls through it
 * into the core's catch box rather than bouncing off.</p>
 *
 * <p>It has no model at all. The surface is drawn by
 * {@link com.reynn.wispwillow.client.renderers.GatewayPortalRenderer}, using the same
 * screen-projected starfield vanilla gives an end portal — which is both what makes the
 * background drift as you move and what makes the nine blocks of the opening read as one
 * continuous window. A block model could do neither.</p>
 */
public class GatewayPortalBlock extends Block implements EntityBlock {

    public static final MapCodec<GatewayPortalBlock> CODEC = simpleCodec(GatewayPortalBlock::new);

    /** Which way the sheet lies, so the model faces across the opening. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;


    public GatewayPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS);
    }

    /** Nothing to bake: the renderer draws it. */
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayPortalBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Never drops, and cannot be picked. The core owns these. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                       net.minecraft.world.level.LevelReader level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player) {
        return ItemStack.EMPTY;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0, 0.04 + random.nextDouble() * 0.04, 0.0);
    }
}
