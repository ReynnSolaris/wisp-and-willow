package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.*;
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
import java.util.Set;

/**
 * Everything a Fluxia generator does that is not "make Fluxia".
 *
 * <p>The Luxinias and Solrosa flowers each hand-rolled their own copy of the storage, the
 * link set, the NBT, the client sync and the once-a-second push into the network, and had
 * already drifted apart in the details. New generators subclass this and implement
 * {@link #generatePerSecond} — a Solrosa is one line, a fuel burner is a dozen.</p>
 */
public abstract class AbstractFluxiaGenerator extends BlockEntity implements IFluxiaNode, GoggleInformation {

    private static final String LINKS_TAG = "FluxLinks";

    protected final SimpleFluxiaStorage tank;
    private final int pushPerTarget;
    private final Set<BlockPos> linkedNodes = new HashSet<>();

    protected AbstractFluxiaGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      int capacity, int throughput, int pushPerTarget) {
        super(type, pos, state);
        this.tank = new SimpleFluxiaStorage(capacity, throughput, throughput, 0);
        this.pushPerTarget = pushPerTarget;
    }

    /**
     * How much Fluxia this generator makes this second, or zero if it is idle. Called once a
     * second on the server; the tank's own intake cap still applies on top.
     */
    protected abstract int generatePerSecond(ServerLevel level, BlockPos pos, BlockState state);

    /** Per-tick work — fuel timers and the like. Server side only. Optional. */
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {}

    /** True while the generator is actually producing, for the goggles and the model. */
    public abstract boolean isRunning();

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractFluxiaGenerator gen) {
        if (!(level instanceof ServerLevel sl)) return;

        gen.serverTick(sl, pos, state);
        if (level.getGameTime() % 20 != 0) return;

        boolean dirty = false;

        int made = gen.generatePerSecond(sl, pos, state);
        if (made > 0 && gen.tank.insert(made, false) > 0) dirty = true;

        if (gen.tank.amount() > 0) {
            FluxNetworkHelper.distributeFromSource(sl, pos, gen.pushPerTarget);
            dirty = true;
        }

        if (dirty) {
            gen.setChanged();
            gen.pushClient();
        }
    }

    // ------------------------------------------------------------------
    // Save / load / sync
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) links.add(LongTag.valueOf(other.asLong()));
        tag.put(LINKS_TAG, links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);

        linkedNodes.clear();
        if (tag.contains(LINKS_TAG)) {
            ListTag links = tag.getList(LINKS_TAG, 4);
            for (int i = 0; i < links.size(); i++) {
                linkedNodes.add(BlockPos.of(((LongTag) links.get(i)).getAsLong()));
            }
        }
    }

    public void pushClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    // ------------------------------------------------------------------
    // Fluxia node
    // ------------------------------------------------------------------

    @Override public FluxiaNodeType getFluxRole() { return FluxiaNodeType.MACHINE; }
    @Override public IFluxiaStorage getFluxStorage() { return tank; }
    @Override public boolean canConnectFlux(Direction side) { return false; }
    @Override public Set<BlockPos> getLinkedNodes() { return linkedNodes; }

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

    // ------------------------------------------------------------------
    // Arcane Goggles
    // ------------------------------------------------------------------

    @Override
    public Component goggleTitle() { return getBlockState().getBlock().getName(); }

    @Override
    public void addGoggleLines(java.util.List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.stored", tank.amount(), tank.capacity(), "ƒ"));
        lines.add(GoggleText.line("goggles.wispwillow.output",
                isRunning()
                        ? GoggleText.good(GoggleText.num(currentRate()) + " ƒ/s")
                        : GoggleText.warn("idle")));
        addExtraGoggleLines(lines);
        lines.add(GoggleText.line("goggles.wispwillow.links", String.valueOf(linkedNodes.size())));
    }

    /** What the goggles should say the output is right now. */
    protected abstract int currentRate();

    /** Anything else worth showing — fuel left, sky access. Optional. */
    protected void addExtraGoggleLines(java.util.List<Component> lines) {}

    @Override
    public float goggleBar() {
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
