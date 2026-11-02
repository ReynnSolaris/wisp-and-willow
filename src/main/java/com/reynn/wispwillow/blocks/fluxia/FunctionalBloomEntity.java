package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.FluxiaNodeType;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base for the functional flora: flowers that <em>spend</em> Fluxia to do work in an area,
 * as opposed to the generating flora which make it.
 *
 * <p>Between them these are the sink the mod was missing — before this, Fluxia had nowhere
 * to go except rituals, so a working generator array had nothing to do most of the time.</p>
 */
public abstract class FunctionalBloomEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {

    private static final String LINKS_TAG = "FluxLinks";

    protected final SimpleFluxiaStorage tank;
    private final Set<BlockPos> linkedNodes = new HashSet<>();

    /** Set true whenever the bloom actually did something, for the renderer and goggles. */
    private boolean working;

    protected FunctionalBloomEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                    int capacity, int rate) {
        super(type, pos, state);
        this.tank = new SimpleFluxiaStorage(capacity, rate, rate, 0);
    }

    // ---- subclass contract ------------------------------------------------

    /** How often this bloom attempts its work, in ticks. */
    protected abstract int interval();

    /** Fluxia drawn per successful action. */
    protected abstract int costPerAction();

    /** Radius of effect, in blocks. */
    public abstract int radius();

    /**
     * Do the work. Return true if anything actually happened — only then is Fluxia spent,
     * so an idle bloom over barren ground costs nothing.
     */
    protected abstract boolean performWork(ServerLevel level, BlockPos pos);

    /** Short description for the goggle panel. */
    protected abstract Component describeWork();

    // ---- ticking ----------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, FunctionalBloomEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        if (level.getGameTime() % be.interval() != 0) return;

        int cost = be.costPerAction();
        boolean wasWorking = be.working;

        if (be.tank.amount() < cost) {
            be.working = false;
        } else {
            be.working = be.performWork(server, pos);
            if (be.working) {
                be.tank.setFlux(be.tank.amount() - cost);
                be.setChanged();
            }
        }

        if (wasWorking != be.working) {
            server.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public boolean isWorking() { return working; }

    // ---- Fluxia node ------------------------------------------------------

    @Override public FluxiaNodeType getFluxRole() { return FluxiaNodeType.MACHINE; }
    @Override public IFluxiaStorage getFluxStorage() { return tank; }
    @Override public boolean canConnectFlux(Direction side) { return false; }
    @Override public Set<BlockPos> getLinkedNodes() { return linkedNodes; }

    @Override
    public int getDesiredFluxPerTick() {
        return Math.max(1, costPerAction() / Math.max(1, interval()));
    }

    @Override
    public void addLinkedNode(BlockPos other) {
        if (other == null || other.equals(worldPosition)) return;
        if (linkedNodes.add(other)) { setChanged(); pushClient(); }
    }

    @Override
    public void removeLinkedNode(BlockPos other) {
        if (other == null) return;
        if (linkedNodes.remove(other)) { setChanged(); pushClient(); }
    }

    public void pushClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- persistence ------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);
        tag.putBoolean("Working", working);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) links.add(LongTag.valueOf(other.asLong()));
        tag.put(LINKS_TAG, links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);
        working = tag.getBoolean("Working");

        linkedNodes.clear();
        if (tag.contains(LINKS_TAG)) {
            ListTag links = tag.getList(LINKS_TAG, LongTag.TAG_LONG);
            for (int i = 0; i < links.size(); i++) {
                linkedNodes.add(BlockPos.of(((LongTag) links.get(i)).getAsLong()));
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

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag, lookupProvider);
    }

    // ---- goggles ----------------------------------------------------------

    @Override
    public Component goggleTitle() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void addGoggleLines(List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.buffer", tank.amount(), tank.capacity(), "ƒ"));
        lines.add(GoggleText.line("goggles.wispwillow.effect", describeWork()));
        lines.add(GoggleText.line("goggles.wispwillow.radius", radius() + " blocks"));
        lines.add(GoggleText.line("goggles.wispwillow.status",
                tank.amount() < costPerAction()
                        ? GoggleText.warn("starved")
                        : working ? GoggleText.good("working") : GoggleText.warn("idle")));
    }

    @Override
    public float goggleBar() {
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
