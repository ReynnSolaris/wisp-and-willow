package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.gateway.GatewayRecipe;
import com.reynn.wispwillow.fluxia.*;
import com.reynn.wispwillow.items.SoulPactItem;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * The Soul Gateway's engine: a Fluxia machine that holds a portal open and charges for
 * everything cast through it.
 *
 * <p>Opening costs a lump sum, staying open costs {@link #UPKEEP_PER_TICK} every tick, and
 * each sealed pact costs again on the way in. Run the tank dry and the gateway shuts on its
 * own — which is the point of the block, and the reason the mod needed generators that can
 * sustain a standing load rather than trickle into a pool.</p>
 */
public class GatewayCoreBlockEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {

    /** Roughly a Greater Tablet's worth, so lighting the gateway is a project, not a click. */
    public static final int CAPACITY = 100_000;
    /** Generous, so a pool can refill the core faster than it burns. */
    private static final int IN_RATE = 2_000;
    private static final int OUT_RATE = 2_000;

    /** Paid once, the moment the portal lights. */
    public static final int OPEN_COST = 10_000;
    /** Paid every tick the portal is lit — 200 Fluxia a second. */
    public static final int UPKEEP_PER_TICK = 10;
    /** Paid again for every sealed pact the gateway swallows. */
    public static final int CAST_COST = 25_000;

    /** How often the wall is re-checked for a missing log. */
    private static final int FRAME_CHECK_INTERVAL = 20;
    /** How often the gateway looks for something to swallow. */
    private static final int CATCH_INTERVAL = 5;

    private static final String LINKS_TAG = "FluxLinks";
    private static final String OPEN_TAG = "Open";

    private final SimpleFluxiaStorage tank = new SimpleFluxiaStorage(CAPACITY, IN_RATE, OUT_RATE, 0);
    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private boolean open;

    public GatewayCoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GATEWAY_CORE.get(), pos, state);
    }

    public boolean isOpen() { return open; }

    private Direction facing() {
        return getBlockState().getValue(GatewayCoreBlock.FACING);
    }

    // ------------------------------------------------------------------
    // Opening and closing
    // ------------------------------------------------------------------

    /**
     * The player asked for the gateway to light. Returns the message to show them either
     * way, so the block's use handler stays a one-liner.
     *
     * <p>The wall may stand in either plane through the core; whichever one is finished is
     * taken, and the core turns to face the side {@code player} is standing on, so that is
     * where familiars step out. When neither plane is finished the message says which is
     * closer to done and why it is not: logs missing, or something solid in the opening.</p>
     */
    public Component tryOpen(@Nullable Player player) {
        if (open) return Component.translatable("block.wispwillow.gateway_core.already_open");

        Direction wall = GatewayFrame.findFacing(level, worldPosition, facing(),
                player == null ? null : player.position());
        if (wall == null) {
            Direction nearest = GatewayFrame.nearestFacing(level, worldPosition, facing());
            int missing = GatewayFrame.missingLogs(level, worldPosition, nearest);
            if (missing > 0) {
                return Component.translatable("block.wispwillow.gateway_core.frame_incomplete", missing);
            }
            return Component.translatable("block.wispwillow.gateway_core.opening_blocked",
                    GatewayFrame.blockedCells(level, worldPosition, nearest));
        }
        if (wall != facing()) {
            // Same block, new facing: the block entity survives and picks up the new state.
            level.setBlock(worldPosition, getBlockState().setValue(GatewayCoreBlock.FACING, wall), 3);
        }
        if (tank.amount() < OPEN_COST) {
            return Component.translatable("block.wispwillow.gateway_core.needs_charge",
                    GoggleText.num(OPEN_COST), GoggleText.num(tank.amount()));
        }

        tank.setFlux(tank.amount() - OPEN_COST);
        setOpen(true);

        if (level instanceof ServerLevel sl) {
            sl.playSound(null, worldPosition, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.7f, 1.4f);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                    40, 0.8, 1.0, 0.8, 0.05);
        }
        return Component.translatable("block.wispwillow.gateway_core.opened");
    }

    public void close() {
        if (!open) return;
        setOpen(false);
        if (level instanceof ServerLevel sl) {
            sl.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }

    private void setOpen(boolean value) {
        this.open = value;
        if (level == null) return;

        if (value) GatewayFrame.openPortal(level, worldPosition, facing());
        else GatewayFrame.closePortal(level, worldPosition, facing());

        level.setBlock(worldPosition, getBlockState().setValue(GatewayCoreBlock.OPEN, value), 3);
        setChanged();
        pushClient();
    }

    /** Called from the block's onRemove, so a broken core never leaves its portal behind. */
    public void tearDown() {
        if (level != null) GatewayFrame.closePortal(level, worldPosition, facing());
    }

    // ------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, GatewayCoreBlockEntity core) {
        if (!(level instanceof ServerLevel sl) || !core.open) return;

        // Knock a log out of the wall and the gateway collapses.
        if (level.getGameTime() % FRAME_CHECK_INTERVAL == 0
                && !GatewayFrame.isComplete(level, pos, core.facing())) {
            core.close();
            return;
        }

        // Upkeep. Extracted rather than subtracted, so the tank's own out-rate cap applies.
        if (core.tank.extract(UPKEEP_PER_TICK, false) < UPKEEP_PER_TICK) {
            core.close();
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
            return;
        }
        core.setChanged();

        if (level.getGameTime() % CATCH_INTERVAL == 0) core.catchOfferings(sl);

        // The gauge visibly drains, so push it to watchers about once a second.
        if (level.getGameTime() % 20 == 0) core.pushClient();
    }

    /** Looks for a sealed pact lying in the portal and spends it. */
    private void catchOfferings(ServerLevel level) {
        var box = GatewayFrame.catchBox(worldPosition, facing());
        var items = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.isAlive() && e.getItem().is(ItemRegistry.SEALED_SOUL_PACT.get()));

        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            ResourceLocation soul = SoulPactItem.soulOf(stack);
            // The bonds are recipes now (wispwillow:gateway), so a pack can add or retune one.
            GatewayRecipe recipe = GatewayRecipe.find(level, soul);
            if (recipe == null) continue; // a sealed pact with nothing bonded in it; leave it lying

            if (tank.amount() < recipe.fluxia()) {
                // Not worth a chat message every five ticks; the pact just waits in the light.
                level.sendParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getY() + 0.3, entity.getZ(), 3, 0.15, 0.15, 0.15, 0.0);
                return;
            }

            if (!cast(level, recipe)) continue;

            tank.setFlux(tank.amount() - recipe.fluxia());
            stack.shrink(1);
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);

            setChanged();
            pushClient();
            return; // one per pass, so a pile of pacts arrives as a procession
        }
    }

    /**
     * Delivers what a casting promises: {@code count} of its creature at the exit, then its
     * items on the ground there. Any entity type will do since 2.1.0; a familiar is the
     * usual case, but a pack may bond a soul to anything it likes.
     */
    private boolean cast(ServerLevel level, GatewayRecipe recipe) {
        BlockPos exit = GatewayFrame.exit(worldPosition, facing());

        if (recipe.summons().isPresent()) {
            EntityType<?> type = recipe.summons().get();
            int spawned = 0;
            for (int n = 0; n < recipe.count(); n++) {
                // spawn() runs finalizeSpawn, which is what gives an axolotl familiar its colour.
                Entity e = type.spawn(level, exit, MobSpawnType.MOB_SUMMONED);
                if (e != null) spawned++;
            }
            if (spawned == 0) return false;
        }
        for (var template : recipe.results()) {
            Containers.dropItemStack(level, exit.getX() + 0.5, exit.getY() + 0.5, exit.getZ() + 0.5, template.copy());
        }

        level.playSound(null, exit, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.9f, 1.3f);
        level.sendParticles(ParticleTypes.SOUL,
                exit.getX() + 0.5, exit.getY() + 0.5, exit.getZ() + 0.5, 40, 0.4, 0.5, 0.4, 0.06);
        level.sendParticles(ParticleTypes.FLASH,
                exit.getX() + 0.5, exit.getY() + 0.6, exit.getZ() + 0.5, 1, 0, 0, 0, 0);
        return true;
    }

    // ------------------------------------------------------------------
    // Save / load / sync
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);
        tag.putBoolean(OPEN_TAG, open);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) links.add(LongTag.valueOf(other.asLong()));
        tag.put(LINKS_TAG, links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);
        open = tag.getBoolean(OPEN_TAG);

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

    /** What the gateway is costing per tick. Nothing reads this yet; the hook exists on
     *  IFluxiaNode and the other machines fill it in, so this one does too. */
    @Override
    public int getDesiredFluxPerTick() {
        return open ? UPKEEP_PER_TICK : 0;
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

    // ------------------------------------------------------------------
    // Arcane Goggles
    // ------------------------------------------------------------------

    @Override
    public Component goggleTitle() { return getBlockState().getBlock().getName(); }

    @Override
    public void addGoggleLines(java.util.List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.stored", tank.amount(), tank.capacity(), "ƒ"));
        lines.add(GoggleText.line("goggles.wispwillow.gateway_state",
                open ? GoggleText.good("open") : GoggleText.warn("closed")));
        if (!open && level != null) {
            Direction nearest = GatewayFrame.nearestFacing(level, worldPosition, facing());
            int missing = GatewayFrame.missingLogs(level, worldPosition, nearest);
            int blocked = GatewayFrame.blockedCells(level, worldPosition, nearest);
            Component frame = missing > 0
                    ? GoggleText.warn((GatewayFrame.LOG_COUNT - missing) + " / " + GatewayFrame.LOG_COUNT + " logs")
                    : blocked > 0 ? GoggleText.warn("opening blocked") : GoggleText.good("complete");
            lines.add(GoggleText.line("goggles.wispwillow.gateway_frame", frame));
        }
        lines.add(GoggleText.line("goggles.wispwillow.upkeep",
                GoggleText.num(UPKEEP_PER_TICK * 20) + " ƒ/s"));
        lines.add(GoggleText.line("goggles.wispwillow.cast_cost", GoggleText.num(CAST_COST) + " ƒ"));

        if (open) {
            lines.add(GoggleText.line("goggles.wispwillow.burns_for",
                    (tank.amount() / (UPKEEP_PER_TICK * 20)) + "s"));
        }
    }

    @Override
    public float goggleBar() {
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
