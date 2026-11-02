package com.reynn.wispwillow.blocks;

import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.dev.CreativeManaPoolBlock;
import com.reynn.wispwillow.infusion.InfusionRecipe;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.*;
import com.reynn.wispwillow.items.FluxiaTabletItem;
import com.reynn.wispwillow.items.SoulPactItem;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManaPoolBlockEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {
    private final SimpleFluxiaStorage tank;
    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private static final String LINKS_TAG = "FluxLinks";
    public static final int PER_TARGET_LIMIT = 800;
    /**
     * The creative pool's per-target rate: effectively "as fast as the target will take
     * it", so a machine on a creative pool runs at its own intake ceiling.
     */
    public static final int CREATIVE_PER_TARGET_LIMIT = BottomlessFluxiaStorage.CAPACITY;

    public ManaPoolBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MANA_POOL.get(), pos, state);
        // The Creative Fluxia Pool shares this block entity -- same ticking, same renderer,
        // same links, same buckets -- and differs in exactly one thing: its reserve is
        // bottomless. Decided here, from the state the constructor is handed, so the
        // tank is right from the first tick and after every reload.
        this.tank = CreativeManaPoolBlock.is(state)
                ? new BottomlessFluxiaStorage(BottomlessFluxiaStorage.CAPACITY)
                : new SimpleFluxiaStorage(40000, 40000, 40000, 0);
    }

    /** Whether this is the creative pool: never empties, never fills. */
    public boolean isBottomless() {
        return tank instanceof BottomlessFluxiaStorage;
    }

    public int perTargetLimit() {
        return isBottomless() ? CREATIVE_PER_TARGET_LIMIT : PER_TARGET_LIMIT;
    }

    /** How often the pool looks for tablets lying in it. */
    private static final int TABLET_SCAN_INTERVAL = 5;

    public static void tick(Level lvl, BlockPos pos, BlockState state, ManaPoolBlockEntity be) {
        if (lvl.isClientSide) return;
        long now = lvl.getGameTime();

        // --- 1) Suck up tablets and move flux into the pool ---
        // An AABB entity query every tick for every pool in the world is not free;
        // draining a tablet four times a second is still effectively instant.
        if (now % TABLET_SCAN_INTERVAL == 0 && be.tank.amount() < be.tank.capacity()) {
            AABB box = new AABB(pos).inflate(0.5, 1.0, 0.5).move(0, 0.5, 0);
            var items = lvl.getEntitiesOfClass(ItemEntity.class, box,
                    e -> e.getItem().getItem() instanceof FluxiaTabletItem);
            for (var it : items) {
                var stack = it.getItem();

                var tabStore = FluxiaTabletItem.readStorage(stack);
                int moved = FluxiaTransfers.move(tabStore, be, 800, false);
                if (moved > 0) {
                    FluxiaTabletItem.writeStorage(stack, tabStore);
                    be.setChanged();
                    be.pushClient();

                    it.setCustomName(Component.translatable(
                            "tooltip.wispwillow.tablet_charge", tabStore.amount(), tabStore.capacity()));
                    it.setCustomNameVisible(true);

                    if (now % 10 == 0) lvl.levelEvent(2005, pos, 0);
                }
            }
        }

        // --- 2) Fluxia Infusion: transmute items floating in the pool ---
        if (lvl instanceof ServerLevel infusionLevel && now % INFUSION_INTERVAL == 0) {
            tryInfuse(infusionLevel, pos, be);
        }

        // --- 3) Only run network distribution once per second ---
        if (!(lvl instanceof ServerLevel sl)) return;
        if (now % 20 != 0) return;

        IFluxiaStorage poolStorage = be.getFluxStorage();
        if (poolStorage == null || poolStorage.amount() <= 0) return;

        // Global network distribution FROM THIS POOL across its entire graph
        FluxNetworkHelper.distributeFromSource(sl, pos, be.perTargetLimit());
    }

    // =========================
    // Fluxia Infusion
    // =========================

    /** How often the pool looks for a matching infusion. */
    private static final int INFUSION_INTERVAL = 20;

    /** Items must be within this box above the pool to count as "in" it. */
    private static AABB infusionBox(BlockPos pos) {
        return new AABB(pos).inflate(0.35, 0.75, 0.35).move(0, 0.4, 0);
    }

    /**
     * Attempts one infusion.
     *
     * <p>Matching is exact — the floating stacks must be precisely the recipe's inputs,
     * nothing more — so a pool with junk in it never quietly consumes the wrong thing.</p>
     */
    private static void tryInfuse(ServerLevel level, BlockPos pos, ManaPoolBlockEntity be) {
        List<ItemEntity> floating = level.getEntitiesOfClass(
                ItemEntity.class, infusionBox(pos), ItemEntity::isAlive);
        if (floating.isEmpty()) return;

        // Tablets take part like anything else. This used to bail out the moment it saw one,
        // to stop the draining path above and an infusion fighting over the same item -- but
        // the Greater and Grand Tablet infusions both take a tablet as an input, so they
        // could never fire. Matching is strict enough to make the guard unnecessary: a lone
        // tablet satisfies no recipe, so it still just charges the pool.
        List<ItemStack> stacks = new ArrayList<>(floating.size());
        for (ItemEntity e : floating) {
            stacks.add(e.getItem());
        }

        for (RecipeHolder<InfusionRecipe> holder : level.getRecipeManager().getAllRecipesFor(InfusionRecipe.TYPE)) {
            InfusionRecipe recipe = holder.value();

            // The plan says how many to take from each stack: a recipe wanting four ingots
            // takes four out of one dropped stack of four, rather than demanding four
            // separate item entities that the game would have merged anyway.
            int[] taken = recipe.match(stacks);
            if (taken == null) continue;

            if (be.tank.amount() < recipe.fluxia()) {
                // Right ingredients, not enough charge: show it is waiting rather than idle.
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.0);
                return;
            }

            // A recipe result is a fixed stack, so anything the inputs were carrying would be
            // lost in the transmutation. Sealing a soul pact is exactly that case -- and it
            // has to happen before the inputs are consumed, or the pact has already been
            // emptied by the time we read it.
            ItemStack output = recipe.result().copy();
            SoulPactItem.carrySoul(stacks, output);

            for (int i = 0; i < floating.size(); i++) {
                if (taken[i] <= 0) continue;
                ItemEntity e = floating.get(i);
                ItemStack stack = e.getItem();
                stack.shrink(taken[i]);
                if (stack.isEmpty()) e.discard();
                else e.setItem(stack);
            }

            be.tank.setFlux(be.tank.amount() - recipe.fluxia());
            be.setChanged();
            be.pushClient();

            ItemEntity result = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    output);
            result.setDeltaMovement(0, 0.15, 0);
            level.addFreshEntity(result);

            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 30, 0.25, 0.2, 0.25, 0.05);
            level.sendParticles(ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1.5f);
            return;
        }
    }

    // =========================
    // Save / Load
    // =========================

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
            ListTag links = tag.getList(LINKS_TAG, 4);
            for (int i = 0; i < links.size(); i++) {
                long packed = ((LongTag) links.get(i)).getAsLong();
                linkedNodes.add(BlockPos.of(packed));
            }
        }
    }

    /**
     * Sets the reserve outright.
     *
     * <p>For one-off transactions — a bucket dipped in or poured out — which are not
     * throughput and so must not be clamped by the tank's per-tick rate the way
     * {@code extract} and {@code insert} are.</p>
     */
    public void setFlux(int amount) {
        tank.setFlux(amount);
        setChanged();
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

    public void onDataPacket(Connection net,
                             ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    // =========================
    // Fluxia Node API
    // =========================

    @Override
    public FluxiaNodeType getFluxRole() {
        return FluxiaNodeType.POOL;
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

    // =========================
    // Arcane Goggles
    // =========================

    @Override
    public Component goggleTitle() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void addGoggleLines(java.util.List<Component> lines) {
        if (isBottomless()) {
            lines.add(GoggleText.line("goggles.wispwillow.stored",
                    Component.translatable("goggles.wispwillow.bottomless").withStyle(ChatFormatting.LIGHT_PURPLE)));
        } else {
            lines.add(GoggleText.amount("goggles.wispwillow.stored", tank.amount(), tank.capacity(), "ƒ"));
        }
        lines.add(GoggleText.line("goggles.wispwillow.links",
                String.valueOf(linkedNodes.size())));
        lines.add(GoggleText.line("goggles.wispwillow.output", isBottomless()
                ? "as fast as each target takes it"
                : GoggleText.num(PER_TARGET_LIMIT) + " ƒ/s per target"));
    }

    @Override
    public float goggleBar() {
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
