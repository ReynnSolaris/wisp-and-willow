package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.*;
import net.minecraft.network.chat.Component;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class SolrosaFlowerEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {
    // Was 1 every 60 ticks despite the name, i.e. a third of a Fluxia a second, and a
    // 200 buffer it could never fill fast enough to matter. It is the starter passive.
    private static final int FLUXIA_PER_SECOND = 4;
    private static final int MAX_FLUXIA = 2_000;
    private static final int THROUGHPUT = 200;
    private final SimpleFluxiaStorage fluxiaStorage =
            new SimpleFluxiaStorage(MAX_FLUXIA, THROUGHPUT, THROUGHPUT, 0);

    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private static final String LINKS_TAG = "FluxLinks";
    public SolrosaFlowerEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.SOLROSA_FLOWER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolrosaFlowerEntity flower) {
        if (level.isClientSide) return;

        boolean dirty = false;
        // --- 2) Once per second: generate Fluxia and push into the network ---
        if (level.getGameTime() % 20 == 0) {
            // 2a) Generate FLUXIA_PER_SECOND while burning, clamped to capacity
            int space = flower.fluxiaStorage.capacity() - flower.fluxiaStorage.amount();
            if (space > 0) {
                int toGenerate = Math.min(FLUXIA_PER_SECOND, space);
                int inserted   = flower.fluxiaStorage.insert(toGenerate, false);
                // SolrosaFlower.animateTick handles the shimmer client-side.
                if (inserted > 0) dirty = true;
            }

            // 2b) Try to distribute Fluxia from the flower into the network
            if (level instanceof ServerLevel sl && flower.fluxiaStorage.amount() > 0) {
                // per-target limit: tweak as you like (e.g. 40, 80, etc.)
                FluxNetworkHelper.distributeFromSource(sl, pos, THROUGHPUT);
                dirty = true;
            }
        }

        if (dirty) {
            flower.setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    public FluxiaNodeType getFluxRole() {
        return FluxiaNodeType.MACHINE;
    }

    @Override
    public IFluxiaStorage getFluxStorage() {
        return fluxiaStorage;
    }

    @Override
    public boolean canConnectFlux(Direction side) {
        return false;
    }

    public void pushClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Set<BlockPos> getLinkedNodes() {
        return linkedNodes;
    }

    @Override
    public void addLinkedNode(BlockPos other) {
        if (other == null || other.equals(this.worldPosition)) return;
        if (linkedNodes.add(other)) {
            setChanged();
            pushClient();
        }
    }

    @Override
    public void removeLinkedNode(BlockPos other) {
        if (other == null) return;
        if (linkedNodes.remove(other)) {
            setChanged();
            pushClient();
        }
    }

    @Override
    public int getDesiredFluxPerTick() {
        // Used by network heuristics if you want; 10 per second = 0.5/tick rounded up
        return 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluxiaStorage.save(tag);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) {
            links.add(LongTag.valueOf(other.asLong()));
        }
        tag.put(LINKS_TAG, links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluxiaStorage.load(tag);

        linkedNodes.clear();
        if (tag.contains(LINKS_TAG)) {
            ListTag links = tag.getList(LINKS_TAG, 4);
            for (int i = 0; i < links.size(); i++) {
                long packed = ((LongTag) links.get(i)).getAsLong();
                linkedNodes.add(BlockPos.of(packed));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net,
                             ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    // =========================
    // Arcane Goggles
    // =========================

    @Override
    public Component goggleTitle() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void addGoggleLines(java.util.List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.buffer",
                fluxiaStorage.amount(), fluxiaStorage.capacity(), "ƒ"));
        lines.add(GoggleText.line("goggles.wispwillow.output", "1 ƒ / 3s"));
    }

    @Override
    public float goggleBar() {
        return (float) fluxiaStorage.amount() / fluxiaStorage.capacity();
    }
}
