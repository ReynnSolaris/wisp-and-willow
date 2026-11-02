package com.reynn.wispwillow.blocks;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import com.reynn.wispwillow.items.FluxLinkingWandItem;

public class ManaPoolBlock extends BaseEntityBlock {

    /** Fluxia traded for one bucket, in either direction. One bucket is 1,000 mB of fluid. */
    public static final int FLUXIA_PER_BUCKET = 1_000;

    public ManaPoolBlock(Properties props) { super(props); }
    public static final MapCodec<ManaPoolBlock> CODEC = simpleCodec(ManaPoolBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0F, 0.0F, 0.0F, 16.0F, 9.0F, 16.0F);
    }

    @Override public RenderShape getRenderShape(BlockState s){ return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaPoolBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == BlockEntityRegistry.MANA_POOL.get()
                ? (lvl, pos, st, be) -> ManaPoolBlockEntity.tick(lvl, pos, st, (ManaPoolBlockEntity) be)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ManaPoolBlockEntity enty)) return InteractionResult.PASS;

        if (!level.isClientSide) {
            IFluxiaStorage be = enty.getFluxStorage();
            boolean sneak = player.isShiftKeyDown();
            int delta = 50; // flux per click
            var data = player.getData(AttachmentRegistry.FLUXIA.get());
            if (data == null) return InteractionResult.CONSUME;

            if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ItemRegistry.FLUX_LINKING_WAND)) return InteractionResult.PASS;
            /*
            if (sneak) {
                // deposit from player into pool
                int moved = Math.min(delta, data.amount());
                moved = be.insert(moved, false);
                if (moved > 0) data.set(data.amount() - moved);
            } else {
                // withdraw into player
                int moved = be.extract(delta, false);
                if (moved > 0) data.set(Math.min(data.capacity(), data.amount() + moved));
            }
            enty.pushClient(); // sync
            */
            int cur = be.amount();
            int cap = be.capacity();
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Fluxia Pool: " + cur + " / " + cap + " ƒ"),
                    true // action bar (set false if you want chat)
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        if (FluxLinkingWandItem.isWand(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof ManaPoolBlockEntity pool) {
            ItemInteractionResult bucket = tryBucket(stack, level, pos, player, hand, pool);
            if (bucket != null) return bucket;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Fluxia in and out of the pool by the bucket.
     *
     * <p>The pool is the only place in the mod that holds a large, steady reserve of Fluxia,
     * and until now there was no way to get any of it into a <em>container</em> — liquid
     * Fluxia could only be made by a chain of cauldron brews that themselves wanted liquid
     * Fluxia to get started. Dipping a bucket in the pool is the obvious move, and it is the
     * one thing every player tries first.</p>
     *
     * <p>Deliberately symmetrical, and deliberately lossless: a bucket is
     * {@value #FLUXIA_PER_BUCKET} either way, so a pool doubles as the buffer between a
     * generator and anything that wants the fluid.</p>
     *
     * @return the interaction result, or null if the held item was not a bucket we handle
     */
    @Nullable
    private ItemInteractionResult tryBucket(ItemStack stack, Level level, BlockPos pos,
                                            Player player, InteractionHand hand,
                                            ManaPoolBlockEntity pool) {
        IFluxiaStorage tank = pool.getFluxStorage();
        boolean creative = player.getAbilities().instabuild;

        if (stack.is(Items.BUCKET)) {
            if (tank.amount() < FLUXIA_PER_BUCKET) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable(
                            "block.wispwillow.mana_pool.too_thin",
                            FLUXIA_PER_BUCKET, tank.amount()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                // setFlux, not extract: extract clamps to the tank's per-tick out-rate, which
                // is a throughput limit and has nothing to say about a one-off transaction.
                pool.setFlux(tank.amount() - FLUXIA_PER_BUCKET);
                if (!creative) stack.shrink(1);
                giveOrDrop(player, new ItemStack(ItemRegistry.FLUXIA_FLUID_BUCKET.get()));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1.1f);
                pool.pushClient();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ItemRegistry.FLUXIA_FLUID_BUCKET.get())) {
            if (tank.amount() + FLUXIA_PER_BUCKET > tank.capacity()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable(
                            "block.wispwillow.mana_pool.full"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                pool.setFlux(tank.amount() + FLUXIA_PER_BUCKET);
                if (!creative) {
                    stack.shrink(1);
                    giveOrDrop(player, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.1f);
                pool.pushClient();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return null;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
