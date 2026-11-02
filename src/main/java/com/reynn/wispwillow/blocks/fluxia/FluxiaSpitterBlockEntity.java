package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.*;
import com.reynn.wispwillow.fluxia.net.FluxArcS2C;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FluxiaSpitterBlockEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {
    private final SimpleFluxiaStorage tank = new SimpleFluxiaStorage(5000, 5000, 5000, 0);

    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private static final String LINKS_TAG = "FluxLinks";
    private Direction facing = Direction.NORTH;

    public FluxiaSpitterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FLUXIA_SPITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluxiaSpitterBlockEntity spitter) {
        // Server-side only, once per second
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;

        IFluxiaStorage src = spitter.getFluxStorage();
        if (src == null || src.amount() <= 0) return;

        // Try to send to ANY linked IFluxiaNode that has room
        for (BlockPos targetPos : spitter.getLinkedNodes()) {
            BlockEntity be = level.getBlockEntity(targetPos);
            if (!(be instanceof IFluxiaNode targetNode)) continue;

            IFluxiaStorage dst = targetNode.getFluxStorage();
            if (dst == null) continue;
            if (dst.amount() >= dst.capacity()) continue; // target full

            // Use shared transfer helper to honor per-tick limits
            int moved = FluxiaTransfers.move(src, targetNode, 40, false); // 40ƒ per second from spitter
            if (moved > 0) {
                if (level instanceof ServerLevel serverLevel) {
                    FluxArcS2C.send(serverLevel, pos, targetPos, moved);
                }
                spitter.setChanged();
                if (be instanceof BlockEntity tBe && level instanceof ServerLevel sl) {
                    tBe.setChanged();
                    sl.sendBlockUpdated(tBe.getBlockPos(), tBe.getBlockState(), tBe.getBlockState(), 3);
                }
                break; // only fire at one target per tick
            }
        }
    }

    public void setFacingDirection(Direction dir) {
        this.facing = dir;
        this.level.setBlock(worldPosition, getBlockState().setValue(FluxiaSpitter.FACING, dir), 3);
        setChanged();
    }

    // ---------------- NBT ----------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) {
            links.add(LongTag.valueOf(other.asLong()));
        }
        tag.put(LINKS_TAG, links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);

        linkedNodes.clear();
        if (tag.contains(LINKS_TAG)) {
            ListTag links = tag.getList(LINKS_TAG, 4); // 4 = TAG_LONG
            for (int i = 0; i < links.size(); i++) {
                long packed = ((LongTag) links.get(i)).getAsLong();
                linkedNodes.add(BlockPos.of(packed));
            }
        }
    }

    // The spitter is the only node that never shipped its state to the client, so its
    // links existed server-side only: the goggles read an always-empty set and reported
    // every spitter as "unlinked", and no link ribbon was ever drawn from one.

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

    // ------------- IFluxiaNode -------------

    @Override
    public FluxiaNodeType getFluxRole() {
        return FluxiaNodeType.MACHINE;
    }

    @Override
    public IFluxiaStorage getFluxStorage() {
        return tank;
    }

    @Override
    public boolean canConnectFlux(Direction side) {
        return false;
    }

    @Override
    public Set<BlockPos> getLinkedNodes() {
        return Collections.unmodifiableSet(linkedNodes);
    }

    @Override
    public void addLinkedNode(BlockPos other) {
        if (other == null || other.equals(this.worldPosition)) return;
        if (!linkedNodes.contains(other) && linkedNodes.add(other)) {
            setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void removeLinkedNode(BlockPos other) {
        if (other == null) return;
        if (linkedNodes.contains(other) && linkedNodes.remove(other)) {
            setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void clearOutgoingLinks() {
        if (linkedNodes.isEmpty()) return;
        linkedNodes.clear();
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        lines.add(GoggleText.amount("goggles.wispwillow.charge", tank.amount(), tank.capacity(), "ƒ"));
        BlockPos target = linkedNodes.isEmpty() ? null : linkedNodes.iterator().next();
        lines.add(GoggleText.line("goggles.wispwillow.target",
                target == null
                        ? GoggleText.warn("unlinked")
                        : GoggleText.good(target.getX() + ", " + target.getY() + ", " + target.getZ())));
        lines.add(GoggleText.line("goggles.wispwillow.output", "40 ƒ/s"));
    }

    @Override
    public float goggleBar() {
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
