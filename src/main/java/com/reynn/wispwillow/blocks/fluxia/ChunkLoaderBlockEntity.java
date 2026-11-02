package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.registry.BlockEntityRegistry;
import com.reynn.wispwillow.util.WWChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkLoaderBlockEntity extends BlockEntity {

    private static final int MAX_RADIUS = 4; // 0 = just its own chunk, 4 = 9x9

    /**
     * Tickets expire after {@link WWChunkLoading#TICKET_TIMEOUT_TICKS}; refreshing well
     * inside that window keeps chunks loaded without re-adding up to 81 tickets every
     * single tick, which is what the old code did.
     */
    private static final int REFRESH_INTERVAL = 10;

    private int radius = 1;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CHUNK_LOADER_BE.get(), pos, state);
    }

    /**
     * Persistent tickets through NeoForge's {@link net.neoforged.neoforge.common.world.chunk.TicketController}:
     * they are saved with the level and re-issued on load, so a loader keeps working across a
     * restart. The old timeout tickets needed this block entity to tick to refresh them, and a
     * block entity in an unloaded chunk never ticks, so every loader died with the server.
     */
    private boolean forced = false;
    private boolean unloading = false;

    // Called from block ticker on server: assert the tickets once after the entity is in the world.
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
        if (!(level instanceof ServerLevel server) || be.forced) return;
        be.force(server, be.radius, true);
        be.forced = true;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int newRadius) {
        newRadius = Mth.clamp(newRadius, 0, MAX_RADIUS);
        if (newRadius == this.radius) return;

        int oldRadius = this.radius;
        this.radius = newRadius;
        setChanged();

        if (level instanceof ServerLevel server) {
            // Shrinking has to actually let go of the chunks we no longer cover.
            force(server, oldRadius, false);
            force(server, newRadius, true);
            forced = true;
        }
    }

    private void force(ServerLevel server, int r, boolean add) {
        var controller = WWChunkLoading.CHUNK_LOADER_CONTROLLER;
        if (controller == null) return;
        ChunkPos center = new ChunkPos(worldPosition);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                controller.forceChunk(server, worldPosition, center.x + dx, center.z + dz, add, true);
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        // A chunk unload also calls setRemoved; the tickets must survive that or a restart loses them.
        unloading = true;
        super.onChunkUnloaded();
    }

    /** When the block is broken, release the chunks. */
    @Override
    public void setRemoved() {
        if (!unloading && level instanceof ServerLevel server) {
            force(server, radius, false);
        }
        super.setRemoved();
    }

    // ---- persistence ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Radius", radius);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        radius = Mth.clamp(tag.getInt("Radius"), 0, MAX_RADIUS);
    }
}
