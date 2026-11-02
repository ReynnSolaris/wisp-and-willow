package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ChunkLoaderBlock extends Block implements EntityBlock {

    public ChunkLoaderBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // no client ticker
        }

        // Only tick our own block entity type
        return type == BlockEntityRegistry.CHUNK_LOADER_BE.get()
                ? (lvl, pos, st, be) -> ChunkLoaderBlockEntity.serverTick(
                lvl, pos, st, (ChunkLoaderBlockEntity) be
        )
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChunkLoaderBlockEntity loader) {
            int current = loader.getRadius();
            int next = (current + 1) % 5; // 0..4
            loader.setRadius(next);
            player.displayClientMessage(
                    Component.literal("Chunk loader radius: " + next + " (chunks)"),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }

    // Clean removal path
    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChunkLoaderBlockEntity loader) {
                loader.setRemoved(); // ensure tickets are removed
            }
            super.onRemove(oldState, level, pos, newState, isMoving);
        } else {
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }
}
