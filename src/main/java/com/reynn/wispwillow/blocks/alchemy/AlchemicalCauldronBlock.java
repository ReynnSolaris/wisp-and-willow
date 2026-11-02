package com.reynn.wispwillow.blocks.alchemy;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.items.FluxLinkingWandItem;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The core of the Alchemical Cauldron multiblock.
 *
 * Three states the player can be in, and the block reports which one plainly:
 *  - unformed: an ordinary cauldron. Right-click tells you the first missing block.
 *  - formed, unlit: the ring is complete. Right-click spends fluxia to light it.
 *  - formed and lit: right-click opens the interface.
 */
public class AlchemicalCauldronBlock extends BaseEntityBlock {

    public static final MapCodec<AlchemicalCauldronBlock> CODEC = simpleCodec(AlchemicalCauldronBlock::new);

    /** Structure is complete. Drives the model swap. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Structure is complete AND has been paid for. Drives light and the renderer. */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /** One-off cost to light the cauldron. Paid from the core's own buffer. */
    public static final int IGNITE_COST = 2_000;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 3, 0, 2, 16, 16),
            Block.box(14, 3, 0, 16, 16, 16),
            Block.box(2, 3, 0, 14, 16, 2),
            Block.box(2, 3, 14, 14, 16, 16));

    public AlchemicalCauldronBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FORMED, false)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Formed or not, the basin keeps the same collision box. The ring is decoration around
     * it, so nothing the player is standing on can vanish when a neighbour is placed.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Placed into an already-built ring? Then it is formed the moment it lands.
        boolean formed = CauldronPattern.matches(ctx.getLevel(), ctx.getClickedPos());
        return defaultBlockState().setValue(FORMED, formed);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemicalCauldronBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, BlockEntityRegistry.ALCHEMICAL_CAULDRON.get(),
                AlchemicalCauldronBlockEntity::serverTick);
    }

    /**
     * Re-validate whenever a face neighbour changes.
     *
     * <p>This is not sufficient on its own, and is not meant to be: every structural block
     * in the pattern is either diagonal to the core or two blocks away, and neither of those
     * fires a neighbour update. The ticker re-checks on a slow cadence for exactly that
     * reason; this hook only buys a same-tick answer for the cases it can actually see.</p>
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        CauldronPattern.revalidate(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        CauldronPattern.revalidate(level, pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Let the linking wand through, the way every other Fluxia node does.
        //
        // The block's own right-click runs before ItemStack#useOn and runs even with a full
        // hand, so a lit cauldron would open its interface on every wand click and the wand
        // would never fire. It is the only way to feed the pot, so without this the whole
        // machine is unpowerable.
        if (FluxLinkingWandItem.isWand(held)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof AlchemicalCauldronBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // A held bucket or tank always means "move fluid", at any stage. Doing this before
        // the formed check is what makes the cauldron usable as a plain basin while you build.
        if (!held.isEmpty() && be.tryFluidInteraction(player, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AlchemicalCauldronBlockEntity be)) {
            return InteractionResult.PASS;
        }

        if (!state.getValue(FORMED)) {
            if (!level.isClientSide) {
                // Answer from a fresh check rather than the stored flag: the player may have
                // closed the ring since the last revalidation, and "you are still missing X"
                // when they are not missing anything is the one message that must never show.
                CauldronPattern.revalidate(level, pos, state);
                CauldronPattern.Slot missing = CauldronPattern.firstFailure(level, pos);
                if (missing != null) {
                    BlockPos at = pos.offset(missing.offset());
                    player.displayClientMessage(Component.translatable(
                            "block.wispwillow.alchemical_cauldron.incomplete",
                            missing.describe(), at.getX(), at.getY(), at.getZ()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!state.getValue(LIT)) {
            if (!level.isClientSide) {
                if (!be.tryIgnite()) {
                    player.displayClientMessage(Component.translatable(
                            "block.wispwillow.alchemical_cauldron.no_fluxia",
                            IGNITE_COST, be.fluxia().amount()), true);
                    return InteractionResult.CONSUME;
                }
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.4f);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 0.8f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Sneaking on a lit pot puts it out. There was no way to do this at all before: the
        // only route from lit back to unlit was breaking a block out of the ring, which is a
        // silly thing to have to do to a cauldron you simply want to stop feeding. The
        // gateway toggles the same way.
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.1f);
                player.displayClientMessage(Component.translatable(
                        "block.wispwillow.alchemical_cauldron.doused", IGNITE_COST), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && be instanceof MenuProvider provider) {
            player.openMenu(provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AlchemicalCauldronBlockEntity be) {
                be.dropContents(level, pos);
            }
            // The cauldron is a Fluxia node, so anything wand-linked to it has to be told.
            FluxLinks.onNodeRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Lit cauldrons light the room; unlit ones do not. Wire this up at registration. */
    public static int lightEmission(BlockState state) {
        return state.getValue(LIT) ? 11 : 0;
    }
}
