package com.reynn.wispwillow.fluxia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Link hygiene for the Fluxia graph.
 *
 * <p>Wand links are stored one-way, on the source node only: {@code A -> B} lives entirely
 * in A's {@code linkedNodes}. That is cheap, but it means breaking B leaves A holding a
 * position that no longer has a node in it — a dead link the goggle overlay happily drew
 * and the goggle tooltip happily counted, so a broken network still looked wired up.</p>
 *
 * <p>Rather than add a second, separately-serialised set of back-links to every node, links
 * are cleaned up from the other end: when a node block is removed it asks its neighbourhood
 * who was pointing at it and tells them to forget. The wand caps a link at 16 blocks, so
 * the search is bounded to the chunks that could possibly hold a source.</p>
 */
public final class FluxLinks {

    private FluxLinks() {}

    /**
     * Longest link the wand will create. Anything pointing at a node from further away than
     * this cannot have been made legitimately, so the inbound sweep need not look past it.
     */
    public static final int MAX_LINK_DISTANCE = 16;

    /** True if {@code pos} currently holds a live Fluxia node. */
    public static boolean isNode(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IFluxiaNode;
    }

    /**
     * True if we can say anything at all about {@code pos} — i.e. its chunk is loaded on
     * this side. Callers that want to hide a link must check this first, or a link whose
     * far end is simply out of view reads as broken.
     */
    public static boolean isLoaded(Level level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /**
     * Drop every link on {@code node} whose target is loaded but no longer a node. Targets in
     * unloaded chunks are left alone — absent is not the same as gone.
     *
     * @return true if anything was removed
     */
    public static boolean pruneStale(Level level, IFluxiaNode node) {
        List<BlockPos> dead = null;
        for (BlockPos target : node.getLinkedNodes()) {
            if (!isLoaded(level, target) || isNode(level, target)) continue;
            if (dead == null) dead = new ArrayList<>();
            dead.add(target);
        }
        if (dead == null) return false;

        // Copied out first: removeLinkedNode mutates the set we were just iterating.
        for (BlockPos target : dead) node.removeLinkedNode(target);
        return true;
    }

    /**
     * A node at {@code pos} is going away. Tell everything within wand range that links to it
     * to drop that link, so no stale ribbon survives the break.
     *
     * <p>Call this from the block's {@code onRemove}, which covers every way a block can
     * leave the world — mined, exploded, pushed, {@code /setblock}.</p>
     */
    public static void onNodeRemoved(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        int chunkR = (MAX_LINK_DISTANCE >> 4) + 1;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        for (int dx = -chunkR; dx <= chunkR; dx++) {
            for (int dz = -chunkR; dz <= chunkR; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;

                // Snapshot: removeLinkedNode marks block entities dirty and sends updates,
                // which is not something to do while walking the chunk's live map.
                List<BlockEntity> sources = null;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (e.getKey().equals(pos)) continue;
                    if (e.getValue() instanceof IFluxiaNode node && node.getLinkedNodes().contains(pos)) {
                        if (sources == null) sources = new ArrayList<>();
                        sources.add(e.getValue());
                    }
                }
                if (sources == null) continue;

                for (BlockEntity be : sources) ((IFluxiaNode) be).removeLinkedNode(pos);
            }
        }
    }
}
