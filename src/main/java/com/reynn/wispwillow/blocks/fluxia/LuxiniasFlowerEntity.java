package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.*;
import net.minecraft.network.chat.Component;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class LuxiniasFlowerEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {
    private int burnTime;
    private static final int FLUXIA_PER_SECOND = 40;
    private static final int MAX_FLUXIA = 4_000;
    private static final int THROUGHPUT = 400;
    /** Fallback only. A coal's real burn time is used when the item declares one. */
    private static final int FUEL_BURN_TICKS = 20 * 30;
    private final SimpleFluxiaStorage fluxiaStorage =
            new SimpleFluxiaStorage(MAX_FLUXIA, THROUGHPUT, THROUGHPUT, FLUXIA_PER_SECOND);

    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private static final String LINKS_TAG = "FluxLinks";
    public LuxiniasFlowerEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.LUXINIAS_FLOWER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LuxiniasFlowerEntity flower) {
        if (level.isClientSide) return;

        boolean dirty = false;

        // --- 1) Burning timer always advances ---
        if (flower.burnTime > 0) {
            flower.burnTime--;
        }

        // --- 2) Once per second: generate Fluxia and push into the network ---
        if (level.getGameTime() % 20 == 0) {
            // 2a) Generate FLUXIA_PER_SECOND while burning, clamped to capacity
            if (flower.burnTime > 0) {
                int space = flower.fluxiaStorage.capacity() - flower.fluxiaStorage.amount();
                if (space > 0) {
                    int toGenerate = Math.min(FLUXIA_PER_SECOND, space);
                    int inserted   = flower.fluxiaStorage.insert(toGenerate, false);
                    // The block's animateTick draws the burning sparks client-side,
                    // so nothing needs to be pushed over the network here.
                    if (inserted > 0) dirty = true;
                }
            }

            // 2b) Try to distribute Fluxia from the flower into the network
            if (level instanceof ServerLevel sl && flower.fluxiaStorage.amount() > 0) {
                // per-target limit: tweak as you like (e.g. 40, 80, etc.)
                FluxNetworkHelper.distributeFromSource(sl, pos, THROUGHPUT);
                dirty = true;
            }
        }

        // --- 3) If not burning, try to eat one Amethyst Coal as new fuel ---
        if (flower.burnTime <= 0) {
            // Don't waste fuel if completely full
            if (flower.fluxiaStorage.amount() < flower.fluxiaStorage.capacity()
                    && level instanceof ServerLevel sl) {

                // Small pickup box above the flower, similar to your pool
                AABB box = new AABB(pos).inflate(0.5, 0.75, 0.5).move(0, 0.5, 0);
                var items = sl.getEntitiesOfClass(ItemEntity.class, box, e -> !e.getItem().isEmpty());

                for (ItemEntity itemEntity : items) {
                    ItemStack stack = itemEntity.getItem();
                    if (!stack.is(ItemRegistry.AMETHYST_COAL.get())) continue;

                    // Read the burn time first: a shrunk-to-empty stack reports 0 and the last
                    // coal of every stack fell back to the flat 600 ticks.
                    int declared = stack.getBurnTime(null);

                    // Consume exactly 1 Amethyst Coal
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(stack);
                    }

                    // A flat 30 seconds ignored how long the fuel actually burns, so an
                    // Amethyst Coal gave a fifth of what it gives a furnace.
                    flower.burnTime = declared > 0 ? declared : FUEL_BURN_TICKS;
                    dirty = true;

                    sl.sendBlockUpdated(pos, state, state, 3);
                    // "Fuel eaten" effect
                    sl.levelEvent(2005, pos, 0);

                    break; // only one per tick
                }
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
        return 10;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluxiaStorage.save(tag);
        tag.putInt("burnTime", burnTime);

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
        burnTime = tag.getInt("burnTime");
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
        lines.add(GoggleText.line("goggles.wispwillow.fuel",
                burnTime > 0
                        ? GoggleText.good(GoggleText.seconds(burnTime) + " left")
                        : GoggleText.warn("idle")));
        lines.add(GoggleText.line("goggles.wispwillow.output",
                burnTime > 0 ? FLUXIA_PER_SECOND + " ƒ/s" : "0 ƒ/s"));
    }

    @Override
    public float goggleBar() {
        return (float) fluxiaStorage.amount() / fluxiaStorage.capacity();
    }

    /** Used by the renderer to know whether to show the burning glow. */
    public boolean isBurning() { return burnTime > 0; }
}
