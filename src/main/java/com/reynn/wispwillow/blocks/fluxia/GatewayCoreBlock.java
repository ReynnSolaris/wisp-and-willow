package com.reynn.wispwillow.blocks.fluxia;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import com.reynn.wispwillow.items.FluxLinkingWandItem;

/**
 * The keystone of a Soul Gateway.
 *
 * <p>Place it, build the willow-log wall described in {@link GatewayFrame} around it, feed it
 * Fluxia, then right-click to light the portal. Right-click again to shut it. The wall stands
 * across the direction the core faces, so the face you placed it against is the face you walk
 * through.</p>
 */
public class GatewayCoreBlock extends BaseEntityBlock {

    public static final MapCodec<GatewayCoreBlock> CODEC = simpleCodec(GatewayCoreBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Lit or dark. Drives the model and the block light. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public GatewayCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == BlockEntityRegistry.GATEWAY_CORE.get()
                ? (lvl, pos, st, be) -> GatewayCoreBlockEntity.tick(lvl, pos, st, (GatewayCoreBlockEntity) be)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GatewayCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (core.isOpen()) {
                core.close();
                player.displayClientMessage(
                        net.minecraft.network.chat.Component
                                .translatable("block.wispwillow.gateway_core.closed"), true);
            } else {
                player.displayClientMessage(core.tryOpen(player), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Breaking the core takes its portal down with it, and drops any link pointing here —
     * the same stale-link problem every other Fluxia node has.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof GatewayCoreBlockEntity core) core.tearDown();
            FluxLinks.onNodeRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Let the linking wand through.
     *
     * <p>The block's own right-click handler runs before {@code ItemStack#useOn}, and runs even with
     * a full hand, so without this the block's own right-click behaviour eats every wand
     * click and the node can never be linked.</p>
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return FluxLinkingWandItem.isWand(stack)
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
