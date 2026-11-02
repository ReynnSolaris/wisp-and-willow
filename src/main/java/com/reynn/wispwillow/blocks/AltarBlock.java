package com.reynn.wispwillow.blocks;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import com.reynn.wispwillow.items.FluxLinkingWandItem;
import com.reynn.wispwillow.items.OmniChalkItem;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.registry.EntityRegistry;
import com.reynn.wispwillow.entity.familiar.Developers;
import com.reynn.wispwillow.entity.RavenFamiliarEntity;

public class AltarBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<AltarBlock> codec = simpleCodec(AltarBlock::new);
    public AltarBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
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
        return new AltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityRegistry.ALTAR.get()
                ? (lvl, pos, st, be) -> AltarBlockEntity.tick(lvl, pos, st, (AltarBlockEntity) be)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            altar.forceRecompute();
            var result = altar.tryPerformRitual(player);
            player.displayClientMessage(result, true);
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
        // Omni Chalk draws on the altar's circle rather than asking the altar to perform
        // it; without this the block's own click handler would cast the rite instead.
        if (stack.getItem() instanceof OmniChalkItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (tryCallRaven(stack, level, pos, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * The developer familiar, called by offering a Soul Wisp to an altar.
     *
     * <p>The gate is on the interaction rather than on a recipe because that is the only
     * gate Minecraft actually enforces: a recipe can be hidden from the recipe book per
     * player, but every external viewer still lists it and anyone who knows the shape can
     * still make it. Here there is nothing to find — a player who is not on the list gets
     * the ordinary altar interaction and no hint that anything else was possible.</p>
     */
    private static boolean tryCallRaven(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!stack.is(ItemRegistry.SOUL_WISP.get())) return false;
        if (!Developers.is(player)) return false;
        if (level.isClientSide) return true;
        if (!(level instanceof ServerLevel server)) return false;

        RavenFamiliarEntity raven = EntityRegistry.RAVEN_FAMILIAR.get().create(server);
        if (raven == null) return false;

        raven.moveTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                player.getYRot(), 0.0f);
        raven.finalizeSpawn(server, level.getCurrentDifficultyAt(pos),
                MobSpawnType.MOB_SUMMONED, null);
        raven.bindTo(pos);
        server.addFreshEntity(raven);

        if (!player.getAbilities().instabuild) stack.shrink(1);

        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 0.7f);
        server.sendParticles(ParticleTypes.SOUL,
                pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.05);
        return true;
    }
}
