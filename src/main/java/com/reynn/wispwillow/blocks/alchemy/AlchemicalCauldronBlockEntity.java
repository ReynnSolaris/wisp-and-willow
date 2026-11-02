package com.reynn.wispwillow.blocks.alchemy;

import com.reynn.wispwillow.alchemy.AlchemyInput;
import com.reynn.wispwillow.alchemy.AlchemyRecipe;
import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.client.menu.AlchemicalCauldronMenu;
import com.reynn.wispwillow.fluxia.FluxiaNodeType;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AlchemicalCauldronBlockEntity extends BlockEntity
        implements MenuProvider, IFluxiaNode, GoggleInformation {

    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 2;
    /** How many upgrades the pot will hold at once, of any mix. */
    public static final int UPGRADE_SLOTS = 3;

    /** Fraction of the run time each Haste upgrade removes. */
    private static final float HASTE_PER_UPGRADE = 0.22f;
    /** Fraction of the fluxia cost each Thrift upgrade removes. */
    private static final float THRIFT_PER_UPGRADE = 0.20f;
    /** Extra tank room, in mB, per Capacity upgrade. */
    private static final int CAPACITY_PER_UPGRADE = 8_000;
    public static final int TANK_CAPACITY = 8_000;         // 8 buckets
    public static final int FLUXIA_CAPACITY = 20_000;
    /**
     * Per-tick transfer ceiling, in and out.
     *
     * <p>This is a cap on a <em>recipe's</em> running cost too: a brew's draw is
     * {@code fluxia / ticks}, and if that exceeds this number the pot can never keep up and
     * will stall forever rather than finish. The steepest shipped brew asks for 20 a tick,
     * so there is a tenfold margin — but a datapack recipe with a large cost and a short
     * duration can fall off the end of it.</p>
     */
    public static final int FLUXIA_IO_RATE = 200;

    /**
     * How often the structure is re-checked from the ticker.
     *
     * <p>Neighbour updates cannot carry this on their own: every block in the pattern is
     * either diagonal to the core or two blocks out, and Minecraft fires no update for
     * either. Two-thirds of a second is fast enough that the ring lighting up still feels
     * like a response to the block you just placed, and slow enough that a wall of
     * cauldrons is not walking a 22-position pattern every tick.</p>
     */
    private static final int REVALIDATE_INTERVAL = 13;

    // ContainerData indices. Kept as constants because the screen reads them by name.
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_DURATION = 1;
    public static final int DATA_FLUXIA = 2;
    public static final int DATA_FLUXIA_MAX = 3;
    public static final int DATA_IN_FLUID = 4;
    public static final int DATA_OUT_FLUID = 5;
    public static final int DATA_COUNT = 6;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); resetIfRecipeLost(); }
    };

    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    /**
     * The upgrade tray.
     *
     * <p>Kept out of the sided item capability entirely: an upgrade is a configuration
     * choice, not a reagent, and a hopper that helpfully filled this with whatever it was
     * carrying would be a very confusing way to lose a stack of ingots. It is reachable only
     * by hand, through the interface.</p>
     */
    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            applyUpgrades();
            syncNow();
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return isUpgrade(stack);
        }
        @Override public int getSlotLimit(int slot) { return 16; }
    };

    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override protected void onContentsChanged() { setChanged(); resetIfRecipeLost(); }
    };

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private final SimpleFluxiaStorage fluxia =
            new SimpleFluxiaStorage(FLUXIA_CAPACITY, FLUXIA_IO_RATE, FLUXIA_IO_RATE, 0);

    /** Wand links, stored one-way on the source node exactly as every other node does. */
    private final Set<BlockPos> linkedNodes = new HashSet<>();

    private int progress;
    private int duration;
    private @Nullable AlchemyRecipe active;

    /**
     * Whether a brew is actually advancing, as opposed to merely sitting in a lit pot.
     *
     * <p>This is the one crafting fact the renderer needs, and the reason it exists as its
     * own field: {@code active} is a recipe object that never crosses to the client, and
     * "the reagent tank has something in it" — the obvious stand-in — is true of a pot that
     * is stalled, jammed, or holding the wrong fluid entirely. Synced only when it flips.</p>
     */
    private boolean crafting;

    // Last state pushed to watching clients. A block update is a full chunk-section packet
    // per recipient; sending one every tick for a progress bar the menu already syncs is
    // how a room of these turns into a bandwidth problem.
    private FluidStack lastSyncedIn = FluidStack.EMPTY;
    private FluidStack lastSyncedOut = FluidStack.EMPTY;
    private boolean lastSyncedCrafting;
    private int lastSyncedFluxia;

    public AlchemicalCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ALCHEMICAL_CAULDRON.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public IItemHandler inputs()      { return inputs; }
    public IItemHandler outputs()     { return outputs; }
    public FluidTank inputTank()      { return inputTank; }
    public FluidTank outputTank()     { return outputTank; }
    public SimpleFluxiaStorage fluxia() { return fluxia; }
    public boolean isCrafting()       { return crafting; }
    public IItemHandler upgrades()    { return upgrades; }

    public static boolean isUpgrade(ItemStack stack) {
        return stack.is(ItemRegistry.UPGRADE_HASTE.get())
                || stack.is(ItemRegistry.UPGRADE_THRIFT.get())
                || stack.is(ItemRegistry.UPGRADE_CAPACITY.get());
    }

    private int countUpgrade(Item type) {
        int total = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (stack.is(type)) total += stack.getCount();
        }
        return total;
    }

    /**
     * Diminishing returns, compounded rather than summed.
     *
     * <p>Each upgrade takes a fixed <em>fraction</em> of what is left, so the first one is
     * worth far more than the tenth and the result can never reach zero. A linear scale would
     * have hit free-and-instant at five upgrades, which is not progression, it is an off
     * switch for the machine's whole cost model.</p>
     */
    private static float compound(float perUpgrade, int count, float floor) {
        float factor = (float) Math.pow(1.0f - perUpgrade, count);
        return Math.max(floor, factor);
    }

    public int hasteCount()    { return countUpgrade(ItemRegistry.UPGRADE_HASTE.get()); }
    public int thriftCount()   { return countUpgrade(ItemRegistry.UPGRADE_THRIFT.get()); }
    public int capacityCount() { return countUpgrade(ItemRegistry.UPGRADE_CAPACITY.get()); }

    /** Run time multiplier, floored so a brew always takes at least a fifth of its time. */
    public float speedFactor() { return compound(HASTE_PER_UPGRADE, hasteCount(), 0.2f); }

    /** Fluxia cost multiplier, floored so a brew is never free. */
    public float costFactor() { return compound(THRIFT_PER_UPGRADE, thriftCount(), 0.25f); }

    public int tankCapacity() {
        return TANK_CAPACITY + capacityCount() * CAPACITY_PER_UPGRADE;
    }

    /**
     * Pushes the tray's effects onto the things that read a fixed number.
     *
     * <p>Only the tanks need this — speed and cost are read fresh every tick, but a
     * {@code FluidTank}'s capacity is a field, and shrinking it under a full tank has to
     * spill rather than silently keep fluid the tank claims it cannot hold.</p>
     */
    private void applyUpgrades() {
        int capacity = tankCapacity();
        inputTank.setCapacity(capacity);
        outputTank.setCapacity(capacity);
        clampTank(inputTank, capacity);
        clampTank(outputTank, capacity);
    }

    private static void clampTank(FluidTank tank, int capacity) {
        if (tank.getFluidAmount() > capacity) {
            FluidStack kept = tank.getFluid().copy();
            kept.setAmount(capacity);
            tank.setFluid(kept);
        }
    }

    public boolean isLit() {
        return getBlockState().hasProperty(AlchemicalCauldronBlock.LIT)
                && getBlockState().getValue(AlchemicalCauldronBlock.LIT);
    }

    /**
     * Per-tick draw, for the running cost of a brew.
     *
     * <p>Deliberately goes through {@code extract}, which clamps to the storage's
     * {@code outRate}. A brew that wants more than the buffer can hand over in one tick
     * <em>should</em> be throttled by it.</p>
     */
    private int drawFluxia(int amount, boolean simulate) {
        return fluxia.extract(amount, simulate);
    }

    /**
     * A one-off lump sum, for lighting the cauldron.
     *
     * <p>This must <em>not</em> go through {@code extract}: that method clamps every call to
     * {@code outRate}, which is a per-tick transfer ceiling, not a per-transaction one. Asking
     * it for the 2,000 to light the pot returns 200 however full the buffer is, so the check
     * "did I get what I asked for" fails forever and the cauldron can never be lit at any
     * charge. The gateway core spends its open and cast costs the same way for the same
     * reason.</p>
     */
    private boolean spendFluxiaLumpSum(int amount) {
        if (fluxia.amount() < amount) return false;
        fluxia.setFlux(fluxia.amount() - amount);
        return true;
    }

    public boolean tryIgnite() {
        if (!spendFluxiaLumpSum(AlchemicalCauldronBlock.IGNITE_COST)) return false;
        setChanged();
        syncNow();
        if (level instanceof ServerLevel sl) onIgnited(sl, getBlockPos());
        return true;
    }

    // ------------------------------------------------------------------ crafting

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalCauldronBlockEntity be) {
        // Diagonals and the plinth never send a neighbour update, so the structure has to
        // be re-read on a timer or a finished ring would sit there refusing to acknowledge
        // itself until something adjacent happened to change.
        // Offset by position so a field of cauldrons does not all revalidate on one tick.
        if (Math.floorMod(level.getGameTime() + pos.asLong(), REVALIDATE_INTERVAL) == 0) {
            CauldronPattern.revalidate(level, pos, state);
            state = level.getBlockState(pos);
        }

        if (!state.hasProperty(AlchemicalCauldronBlock.FORMED)) return;

        if (!state.getValue(AlchemicalCauldronBlock.FORMED) || !state.getValue(AlchemicalCauldronBlock.LIT)) {
            be.abandonBrew();
            be.syncIfChanged();
            return;
        }

        if (be.active == null) {
            be.active = be.findRecipe(level).orElse(null);
            if (be.active == null) {
                be.abandonBrew();
                // Lit and waiting is a state worth showing: a cold-looking pot that is
                // actually ready reads as broken.
                if (level instanceof ServerLevel sl && level.getGameTime() % 17 == 0) {
                    be.idleShimmer(sl, pos);
                }
                be.syncIfChanged();
                return;
            }
            be.duration = Math.max(1, Math.round(be.active.ticks() * be.speedFactor()));
            be.progress = 0;
        }

        AlchemyRecipe recipe = be.active;

        // Still valid? Ingredients can be pulled out mid-craft by a pipe.
        if (!recipe.matches(be.snapshot(), level)) {
            be.abandonBrew();
            be.syncIfChanged();
            return;
        }

        // Pay per tick rather than up front, so a starved cauldron stalls visibly
        // instead of silently refusing to start.
        // Cost is spread over the run the pot is actually going to take, so Haste makes a
        // brew finish sooner without also making it cheaper — the fluxia bill per brew is
        // Thrift's job alone, and the two upgrades stay independent choices.
        int total = Math.round(recipe.fluxia() * be.costFactor());
        int perTick = total <= 0 ? 0 : Math.max(1, total / Math.max(1, be.duration));

        if (perTick > 0 && be.drawFluxia(perTick, true) < perTick) {
            be.setCrafting(false);            // stalled: keep progress, wait for power
            be.syncIfChanged();
            return;
        }

        if (!be.canFitResults(recipe)) {
            be.setCrafting(false);            // output backed up: also a stall, not a reset
            be.syncIfChanged();
            return;
        }

        if (perTick > 0) be.drawFluxia(perTick, false);
        be.setCrafting(true);
        be.progress++;

        if (be.progress >= be.duration) {
            be.assemble(recipe);
            be.progress = 0;
            be.active = null;
            be.setCrafting(false);
            be.celebrate(level, pos);
        } else if (level instanceof ServerLevel sl && be.progress % 5 == 0) {
            be.simmer(sl, pos, be.progress);
        }

        be.setChanged();
        be.syncIfChanged();
    }

    /** Stop whatever was brewing without consuming anything. */
    private void abandonBrew() {
        if (active == null && progress == 0 && !crafting) return;
        active = null;
        progress = 0;
        setCrafting(false);
        setChanged();
    }

    private void setCrafting(boolean value) {
        crafting = value;
    }

    private Optional<AlchemyRecipe> findRecipe(Level level) {
        AlchemyInput input = snapshot();
        // An empty pot cannot match anything, and this is the common case for a cauldron
        // that has been left lit — worth one field read to skip walking the recipe list
        // twenty times a second.
        if (input.isEmpty()) return Optional.empty();

        // Not getRecipeFor: that returns the first match in whatever order the manager
        // happens to hold, and several brews can legitimately match one pot. Take the most
        // demanding instead — see AlchemyRecipe#specificity.
        AlchemyRecipe best = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMY_TYPE.get())) {
            AlchemyRecipe candidate = holder.value();
            if (!candidate.matches(input, level)) continue;
            if (best == null || candidate.specificity() > best.specificity()) best = candidate;
        }
        return Optional.ofNullable(best);
    }

    private AlchemyInput snapshot() {
        return new AlchemyInput(inputs, inputTank.getFluid());
    }

    private void resetIfRecipeLost() {
        if (level == null || level.isClientSide || active == null) return;
        if (!active.matches(snapshot(), level)) {
            active = null;
            progress = 0;
            setCrafting(false);
        }
    }

    private boolean canFitResults(AlchemyRecipe recipe) {
        ItemStack result = recipe.result();
        if (!result.isEmpty() && !insertOutput(result.copy(), true).isEmpty()) return false;

        FluidStack fluidResult = recipe.fluidResult();
        if (!fluidResult.isEmpty()
                && outputTank.fill(fluidResult.copy(), IFluidHandler.FluidAction.SIMULATE) < fluidResult.getAmount()) {
            return false;
        }
        return true;
    }

    private void assemble(AlchemyRecipe recipe) {
        recipe.consume(inputs, inputTank);

        if (!recipe.result().isEmpty()) insertOutput(recipe.result().copy(), false);
        if (!recipe.fluidResult().isEmpty()) {
            outputTank.fill(recipe.fluidResult().copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < outputs.getSlots() && !remaining.isEmpty(); i++) {
            remaining = outputs.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    // ------------------------------------------------------------------ theatre

    /**
     * One enchanting glyph drifting in from a pillar cap toward the mouth.
     *
     * <p>Vanilla's enchanting table draws exactly this, from its bookshelves; the cauldron's
     * four caps are its bookshelves. Borrowing the effect rather than inventing one means the
     * player already knows what it means before they have read a word about the machine.</p>
     *
     * <p>The count of 0 is load-bearing. With a positive count the three deltas are a random
     * <em>spread</em> and every particle would sit in a cloud over the pot; with 0 the server
     * sends a single particle and the deltas become its velocity, which is the only way to
     * aim one. Vanilla passes the offset to the bookshelf here, so passing the offset to the
     * cap makes the glyph fly cap-to-pot.</p>
     */
    private void glyphFromPillar(ServerLevel level, BlockPos pos, RandomSource rng) {
        BlockPos[] caps = CauldronPattern.pillarCaps(pos);
        BlockPos cap = caps[rng.nextInt(caps.length)];
        double mx = pos.getX() + 0.5, my = pos.getY() + 1.05, mz = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.ENCHANT, mx, my, mz, 0,
                cap.getX() + 0.5 - mx, cap.getY() + 0.2 - my, cap.getZ() + 0.5 - mz, 1.0);
    }

    /** Lit but idle: the odd glyph, so a waiting cauldron still looks awake. */
    private void idleShimmer(ServerLevel level, BlockPos pos) {
        glyphFromPillar(level, pos, level.random);
    }

    /** Running: glyphs pouring in, the surface working, embers on the caps. */
    private void simmer(ServerLevel level, BlockPos pos, int tick) {
        RandomSource rng = level.random;
        glyphFromPillar(level, pos, rng);

        if (tick % 10 == 0) {
            level.sendParticles(ParticleTypes.WITCH,
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    2, 0.18, 0.05, 0.18, 0.01);
        }
        if (tick % 20 == 0) {
            BlockPos cap = CauldronPattern.pillarCaps(pos)[rng.nextInt(4)];
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    cap.getX() + 0.5, cap.getY() + 1.05, cap.getZ() + 0.5,
                    1, 0.12, 0.02, 0.12, 0.0);
        }
    }

    /** The moment it lights: a sweep of glyphs around the circle and a flash over the mouth. */
    private void onIgnited(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5 + Math.cos(a) * CauldronPattern.RADIUS,
                    pos.getY() + 0.1,
                    pos.getZ() + 0.5 + Math.sin(a) * CauldronPattern.RADIUS,
                    1, 0.0, 0.35, 0.0, 0.01);
        }
        for (int i = 0; i < 12; i++) glyphFromPillar(level, pos, level.random);
        level.sendParticles(ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
    }

    /** A brew lands: a bright pop out of the mouth. */
    private void celebrate(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1.3f);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 1.6f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    14, 0.22, 0.15, 0.22, 0.04);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    10, 0.25, 0.35, 0.25, 0.02);
            sl.sendParticles(ParticleTypes.FLASH,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ fluid by hand

    /**
     * Bucket or tank in hand, routed by which way the fluid is going.
     *
     * <p>Which tank to talk to has to be decided from what the player is holding, not by
     * trying each in turn: {@code interactWithFluidHandler} attempts to <em>fill</em> the
     * handler before it tries to drain it, so offering it the decanting tank first meant a
     * full bucket poured the reagent straight into the output side — the one place the
     * player can never brew from.</p>
     *
     * <p>So: anything with fluid in it pours into the reagent tank, and only there. An empty
     * container takes from the decanting tank first, since collecting the product is the
     * common case, and falls back to the reagent tank so a mis-poured reagent is recoverable
     * without resorting to the dump button.</p>
     */
    public boolean tryFluidInteraction(Player player, InteractionHand hand) {
        if (level == null) return false;

        boolean pouringIn = !FluidUtil.getFluidContained(player.getItemInHand(hand))
                .orElse(FluidStack.EMPTY).isEmpty();

        if (pouringIn) {
            if (FluidUtil.interactWithFluidHandler(player, hand, inputTank)) {
                setChanged(); syncNow(); return true;
            }
            return false;
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, outputTank)) { setChanged(); syncNow(); return true; }
        if (FluidUtil.interactWithFluidHandler(player, hand, inputTank))  { setChanged(); syncNow(); return true; }
        return false;
    }

    /**
     * Pours the decanting tank back into the reagent tank.
     *
     * <p>The cauldron's own condensing brews turn a solid into a fluid that a later brew then
     * wants as an input, and without this the only route between the two tanks was out through
     * a bucket and back in again. {@code fill} refuses a mismatched fluid on its own, so a pot
     * part-way through a different reagent simply will not take it.</p>
     *
     * @return true if anything actually moved
     */
    public boolean recirculate() {
        if (outputTank.isEmpty()) return false;

        int room = inputTank.getCapacity() - inputTank.getFluidAmount();
        if (room <= 0) return false;

        FluidStack offer = outputTank.drain(room, IFluidHandler.FluidAction.SIMULATE);
        int moved = inputTank.fill(offer, IFluidHandler.FluidAction.EXECUTE);
        if (moved <= 0) return false;

        outputTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
        syncNow();
        return true;
    }

    /** Called by the GUI's dump buttons. Voids a tank outright — no recovery, by design. */
    public void dumpTank(boolean output) {
        (output ? outputTank : inputTank).setFluid(FluidStack.EMPTY);
        setChanged();
        syncNow();
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer bag = new SimpleContainer(INPUT_SLOTS + OUTPUT_SLOTS + UPGRADE_SLOTS);
        for (int i = 0; i < inputs.getSlots(); i++) bag.setItem(i, inputs.getStackInSlot(i));
        for (int i = 0; i < outputs.getSlots(); i++) bag.setItem(INPUT_SLOTS + i, outputs.getStackInSlot(i));
        for (int i = 0; i < upgrades.getSlots(); i++) {
            bag.setItem(INPUT_SLOTS + OUTPUT_SLOTS + i, upgrades.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, bag);
    }

    // ------------------------------------------------------------------ capabilities

    /**
     * Sided views for pipes from other mods.
     *   top and sides -> insert into the input slots only
     *   bottom        -> extract from the output slots only
     * Splitting it this way means an importer on the side and an exporter underneath
     * "just work" without any filter config, which is the behaviour people expect from
     * every other machine they have ever automated.
     */
    public @Nullable IItemHandler itemHandlerFor(@Nullable Direction side) {
        if (side == null) return new CombinedCauldronHandler(inputs, outputs);
        if (side == Direction.DOWN) return new SidedView(outputs, false, true);
        return new SidedView(inputs, true, false);
    }

    /**
     * The same split for fluid. The tanks are handed out wrapped rather than raw: an
     * unwrapped {@code FluidTank} is both fillable and drainable, so a pipe on the side
     * could quietly siphon the reagents back out of a pot it was supposed to be feeding.
     */
    public @Nullable IFluidHandler fluidHandlerFor(@Nullable Direction side) {
        if (side == null) return new CombinedTank(inputTank, outputTank);
        if (side == Direction.DOWN) return new SidedTank(outputTank, false, true);
        return new SidedTank(inputTank, true, false);
    }

    // ------------------------------------------------------------------ fluxia node

    @Override public FluxiaNodeType getFluxRole() { return FluxiaNodeType.MACHINE; }
    @Override public IFluxiaStorage getFluxStorage() { return fluxia; }

    /** Wand-linked only, the same as every other node in the mod. */
    @Override public boolean canConnectFlux(Direction side) { return false; }

    @Override public Set<BlockPos> getLinkedNodes() { return linkedNodes; }

    /**
     * What the cauldron would spend this tick. Reported honestly rather than as a flat
     * rating: an idle pot asking for its brewing rate would out-bid a gateway that is
     * actually burning fluxia.
     */
    @Override
    public int getDesiredFluxPerTick() {
        if (!isLit()) return 0;
        if (active == null) return 0;
        return Math.max(1, active.fluxia() / Math.max(1, active.ticks()));
    }

    @Override
    public void addLinkedNode(BlockPos other) {
        if (other == null || other.equals(worldPosition)) return;
        if (linkedNodes.add(other)) { setChanged(); syncNow(); }
    }

    @Override
    public void removeLinkedNode(BlockPos other) {
        if (other == null) return;
        if (linkedNodes.remove(other)) { setChanged(); syncNow(); }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public Component goggleTitle() { return getBlockState().getBlock().getName(); }

    @Override
    public void addGoggleLines(List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.stored", fluxia.amount(), fluxia.capacity(), "ƒ"));

        boolean formed = getBlockState().hasProperty(AlchemicalCauldronBlock.FORMED)
                && getBlockState().getValue(AlchemicalCauldronBlock.FORMED);
        lines.add(GoggleText.line("goggles.wispwillow.cauldron_structure",
                formed ? GoggleText.good("closed") : GoggleText.warn("incomplete")));
        lines.add(GoggleText.line("goggles.wispwillow.cauldron_fire",
                isLit() ? GoggleText.good("lit") : GoggleText.warn("cold")));

        lines.add(GoggleText.line("goggles.wispwillow.cauldron_reagent", tankLine(inputTank)));
        lines.add(GoggleText.line("goggles.wispwillow.cauldron_decant", tankLine(outputTank)));

        int fitted = hasteCount() + thriftCount() + capacityCount();
        if (fitted > 0) {
            lines.add(GoggleText.line("goggles.wispwillow.upgrades",
                    Math.round((1f - speedFactor()) * 100) + "% faster, "
                            + Math.round((1f - costFactor()) * 100) + "% cheaper"));
        }

        if (duration > 0 && progress > 0) {
            lines.add(GoggleText.line("goggles.wispwillow.progress",
                    Math.round(100f * progress / duration) + "%"
                            + (crafting ? "" : " (stalled)")));
        }
    }

    private static String tankLine(FluidTank tank) {
        if (tank.isEmpty()) return "empty";
        return GoggleText.num(tank.getFluidAmount()) + " mB "
                + tank.getFluid().getHoverName().getString();
    }

    @Override
    public float goggleBar() {
        return fluxia.capacity() <= 0 ? -1f : (float) fluxia.amount() / fluxia.capacity();
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.wispwillow.alchemical_cauldron");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AlchemicalCauldronMenu(id, inv, this, dataAccess);
    }

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case DATA_PROGRESS   -> progress;
                case DATA_DURATION   -> duration;
                case DATA_FLUXIA     -> fluxia.amount();
                case DATA_FLUXIA_MAX -> fluxia.capacity();
                case DATA_IN_FLUID   -> inputTank.getFluidAmount();
                case DATA_OUT_FLUID  -> outputTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case DATA_PROGRESS -> progress = v;
                case DATA_DURATION -> duration = v;
                default -> { }
            }
        }
        @Override public int getCount() { return DATA_COUNT; }
    };

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.put("Upgrades", upgrades.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Fluxia", fluxia.amount());
        tag.putInt("Progress", progress);
        tag.putInt("Duration", duration);
        tag.putBoolean("Crafting", crafting);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) links.add(LongTag.valueOf(other.asLong()));
        tag.put("FluxLinks", links);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        upgrades.deserializeNBT(registries, tag.getCompound("Upgrades"));
        // Capacity has to be restored before the tanks are read, or a tank saved above the
        // base capacity would be clipped on the way back in.
        applyUpgrades();
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        fluxia.set(tag.getInt("Fluxia"));
        progress = tag.getInt("Progress");
        duration = tag.getInt("Duration");
        crafting = tag.getBoolean("Crafting");

        linkedNodes.clear();
        if (tag.contains("FluxLinks")) {
            ListTag links = tag.getList("FluxLinks", Tag.TAG_LONG);
            for (int i = 0; i < links.size(); i++) {
                linkedNodes.add(BlockPos.of(((LongTag) links.get(i)).getAsLong()));
            }
        }

        lastSyncedIn = inputTank.getFluid().copy();
        lastSyncedOut = outputTank.getFluid().copy();
        lastSyncedCrafting = crafting;
        lastSyncedFluxia = fluxia.amount();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Push a block update only when something a watching client can actually see has
     * changed: the two tanks (the GUI and the brew surface read them) and whether a brew
     * is advancing (the renderer reads it). Progress itself travels over the menu's
     * ContainerData, which is why it is deliberately absent from this test.
     *
     * <p>The fluxia buffer moves every single tick while a brew runs and is read by the
     * goggle gauge, so it gets the once-a-second cadence the dynamo and the gateway core
     * already use — visibly live, without a chunk-section packet per tick per viewer.</p>
     */
    private void syncIfChanged() {
        if (level == null || level.isClientSide) return;

        if (crafting != lastSyncedCrafting
                || !FluidStack.matches(inputTank.getFluid(), lastSyncedIn)
                || !FluidStack.matches(outputTank.getFluid(), lastSyncedOut)) {
            syncNow();
            return;
        }

        if (fluxia.amount() != lastSyncedFluxia && level.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    private void syncNow() {
        setChanged();
        if (level == null || level.isClientSide) return;
        lastSyncedIn = inputTank.getFluid().copy();
        lastSyncedOut = outputTank.getFluid().copy();
        lastSyncedCrafting = crafting;
        lastSyncedFluxia = fluxia.amount();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    // ------------------------------------------------------------------ wrappers

    /** An IItemHandler view that can be insert-only or extract-only. */
    private record SidedView(ItemStackHandler backing, boolean allowInsert, boolean allowExtract)
            implements IItemHandler {
        @Override public int getSlots() { return backing.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return backing.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
            return allowInsert ? backing.insertItem(slot, stack, sim) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean sim) {
            return allowExtract ? backing.extractItem(slot, amount, sim) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return backing.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && backing.isItemValid(slot, stack);
        }
    }

    /** The fluid counterpart to {@link SidedView}. */
    private record SidedTank(FluidTank backing, boolean allowFill, boolean allowDrain)
            implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int t) { return backing.getFluid(); }
        @Override public int getTankCapacity(int t) { return backing.getCapacity(); }
        @Override public boolean isFluidValid(int t, FluidStack stack) {
            return allowFill && backing.isFluidValid(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return allowFill ? backing.fill(resource, action) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return allowDrain ? backing.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return allowDrain ? backing.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    /** Inputs then outputs, for the null-side (block-in-hand) view. */
    private record CombinedCauldronHandler(ItemStackHandler in, ItemStackHandler out) implements IItemHandler {
        @Override public int getSlots() { return in.getSlots() + out.getSlots(); }
        private boolean isOut(int slot) { return slot >= in.getSlots(); }
        private int local(int slot) { return isOut(slot) ? slot - in.getSlots() : slot; }
        @Override public ItemStack getStackInSlot(int slot) {
            return (isOut(slot) ? out : in).getStackInSlot(local(slot));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
            return isOut(slot) ? stack : in.insertItem(slot, stack, sim);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean sim) {
            return isOut(slot) ? out.extractItem(local(slot), amount, sim) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) {
            return (isOut(slot) ? out : in).getSlotLimit(local(slot));
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return !isOut(slot); }
    }

    /** Tank 0 is input (fillable), tank 1 is output (drainable). */
    private record CombinedTank(FluidTank in, FluidTank out) implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int t) {
            return t == 0 ? in.getFluid() : out.getFluid();
        }
        @Override public int getTankCapacity(int t) { return in.getCapacity(); }
        @Override public boolean isFluidValid(int t, FluidStack stack) { return t == 0; }
        @Override public int fill(FluidStack resource, FluidAction action) { return in.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return out.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return out.drain(maxDrain, action); }
    }
}
