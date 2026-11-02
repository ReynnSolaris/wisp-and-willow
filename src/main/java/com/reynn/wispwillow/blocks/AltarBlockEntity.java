package com.reynn.wispwillow.blocks;

import com.reynn.wispwillow.altar.AltarTier;
import com.reynn.wispwillow.client.goggles.GoggleInformation;
import com.reynn.wispwillow.entity.familiar.FamiliarBoon;
import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.FluxiaNodeType;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import com.reynn.wispwillow.registry.ParticleRegistry;
import com.reynn.wispwillow.ritual.RitualRecipe;
import com.reynn.wispwillow.ritual.Vigil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import com.reynn.wispwillow.advancements.WWTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import com.reynn.wispwillow.items.Awakening;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.*;

public class AltarBlockEntity extends BlockEntity implements IFluxiaNode, GoggleInformation {

    // ---- NBT keys ----
    private static final String LINKS_TAG      = "FluxLinks";
    private static final String MATCH_TAG      = "MatchId";
    private static final String MISSING_TAG    = "Missing";
    private static final String WRONG_TYPE_TAG = "WrongType";
    private static final String HAS_FLUXIA_TAG = "HasFluxia";
    private static final String TIER_TAG       = "Tier";
    private static final String RUNES_TAG      = "Runes";
    private static final String EFFICIENCY_TAG = "Efficiency";
    private static final String YIELD_TAG      = "Yield";
    private static final String MISPLACED_TAG  = "Misplaced";
    private static final String VIGIL_TAG      = "Vigil";
    private static final String VIGIL_RITE_TAG = "VigilRite";
    private static final String VIGIL_AGE_TAG  = "VigilAge";

    private static final int RECHECK_INTERVAL = 20;

    /** How often a burning vigil re-checks that its own circle is still on the ground. */
    private static final int VIGIL_CHECK_INTERVAL = 20;

    /** Computed on the server, mirrored to clients so the renderer has something to draw. */
    private RitualRecipe cachedMatch;
    /** Client-side: the id we were told about, resolved lazily against the synced recipe manager. */
    private ResourceLocation clientMatchId;

    private final List<MissingCell> missing = new ArrayList<>();
    private final List<BlockPos> wrongTypeSpots = new ArrayList<>();
    private final Set<BlockPos> linkedNodes = new HashSet<>();
    private final SimpleFluxiaStorage tank = new SimpleFluxiaStorage(20000, 20000, 20000, 0);

    private int recheckCooldown = 0;
    public boolean hasSufficientFluxia = false;

    /** The rune foundation beneath us; recomputed with the circle, mirrored to clients. */
    private AltarTier.Foundation foundation = AltarTier.Foundation.BARE;

    // Boons contributed by familiars currently in this altar's service.
    private float familiarEfficiency;
    private float familiarYield;
    private int familiarCapacity;
    private final List<Component> familiarNames = new ArrayList<>();
    /** How many familiars are bound here, including any the altar has no room for. */
    private int familiarsBound;
    /** How many it can actually put to work: one per tier. */
    private int familiarSlots = 1;

    // ---- Vigil ----
    /**
     * The vigil this altar is keeping, or null. Held as a value rather than as a reference
     * back to its rite so a datapack reload mid-burn cannot retune or strand something the
     * player has already paid for.
     */
    private Vigil activeVigil;
    /** Which rite lit it — needed to re-check the circle, and to name it in chat. */
    private ResourceLocation vigilRiteId;
    private int vigilAge;
    private int vigilCheckCooldown = VIGIL_CHECK_INTERVAL;

    // ---- The strike ----
    /**
     * Game time of the last completed rite, synced to the client so the renderer can draw the
     * bolt from the sky that answers it. Not persisted: a strike belongs to the moment.
     */
    private long lastStrike = Long.MIN_VALUE;
    private static final String STRIKE_TAG = "Strike";

    /** Ticks since the last completed rite as the client sees it, or -1 if there was none. */
    public float strikeAge(float partialTick) {
        if (level == null || lastStrike == Long.MIN_VALUE) return -1f;
        return (float) (level.getGameTime() - lastStrike) + partialTick;
    }

    // Recipe list + global radius are stable between datapack reloads; cache them per manager.
    private RecipeManager cachedManager;
    private List<RecipeHolder<RitualRecipe>> cachedRecipes = List.of();
    private int cachedGlobalRadius = 4;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ALTAR.get(), pos, state);
    }

    public record MissingCell(int wx, int wy, int wz,
                              Optional<SigilType> type,
                              Optional<DyeColor> color) {}

    // =====================================================================
    // Fluxia node
    // =====================================================================

    @Override public FluxiaNodeType getFluxRole() { return FluxiaNodeType.MACHINE; }
    @Override public IFluxiaStorage getFluxStorage() { return tank; }
    @Override public boolean canConnectFlux(Direction side) { return false; }
    @Override public Set<BlockPos> getLinkedNodes() { return linkedNodes; }

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

    public void pushClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =====================================================================
    // Ticking
    // =====================================================================

    public static void tick(Level level, BlockPos pos, BlockState state, AltarBlockEntity be) {
        if (level.isClientSide) {
            if ((level.getGameTime() & 0x3) == 0) be.spawnWrongTypeParticles(level);
            return;
        }
        if (be.activeVigil != null) be.tickVigil(level, pos);

        if (be.recheckCooldown-- <= 0) {
            be.recheckCooldown = RECHECK_INTERVAL;
            be.recompute(level, pos);
        }
    }

    public void forceRecompute() {
        if (level == null || level.isClientSide) return;
        recompute(level, worldPosition);
        recheckCooldown = RECHECK_INTERVAL;
    }

    /** Widened so the ghost-glyph overlay for a large circle isn't frustum-culled. */
    public AABB getRenderBoundingBox() {
        int r = (cachedMatchOrClient() != null) ? cachedMatchOrClient().maxRadius() : 4;
        return new AABB(worldPosition).inflate(r + 0.5);
    }

    private void spawnWrongTypeParticles(Level lvl) {
        if (wrongTypeSpots.isEmpty()) return;

        for (BlockPos p : wrongTypeSpots) {
            // Get precise top so the particle sits on the surface
            double top = 1.0;
            BlockState st = lvl.getBlockState(p);
            VoxelShape shape = st.getShape(lvl, p);
            if (!shape.isEmpty()) top = shape.max(Direction.Axis.Y);

            double x = p.getX() + 0.5;
            double y = p.getY() + top + 0.015; // slight lift
            double z = p.getZ() + 0.5;

            // Slight random motion so it feels "alive"
            double dx = (lvl.random.nextDouble() - 0.5) * 0.02;
            double dy = 0.01 + lvl.random.nextDouble() * 0.02;
            double dz = (lvl.random.nextDouble() - 0.5) * 0.02;

            DustParticleOptions redDust = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.2f), 1.0f);
            lvl.addParticle(redDust, x, y, z, dx, dy, dz);
        }
    }

    // =====================================================================
    // Recipe matching
    // =====================================================================

    /** Refreshes the per-datapack recipe cache if the server reloaded. */
    private void ensureRecipeCache(Level lvl) {
        RecipeManager mgr = lvl.getRecipeManager();
        if (mgr == cachedManager) return;

        cachedManager = mgr;
        cachedRecipes = List.copyOf(mgr.getAllRecipesFor(RitualRecipe.TYPE));

        int r = 4; // safe default
        for (var h : cachedRecipes) r = Math.max(r, h.value().maxRadius());
        cachedGlobalRadius = r;
    }

    /**
     * One pass over the ground around the altar, recording every sigil we find.
     *
     * <p>Matching used to call {@code getBlockState} once per pattern cell per candidate
     * recipe — for the 17x17 Shrouded Conclave that is thousands of lookups a second.
     * Reading the area once into a map turns the whole recompute into a few hundred
     * lookups plus cheap hash probes.</p>
     */
    private static Long2ObjectOpenHashMap<BlockState> scanSigils(Level lvl, BlockPos center, int radius) {
        Long2ObjectOpenHashMap<BlockState> found = new Long2ObjectOpenHashMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int y = center.getY();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dz == 0) continue;
                m.set(center.getX() + dx, y, center.getZ() + dz);
                BlockState s = lvl.getBlockState(m);
                if (s.getBlock() instanceof SigilBlock) found.put(m.asLong(), s);
            }
        }
        return found;
    }

    /** Chebyshev distance to the furthest sigil the player has actually drawn. */
    private static int observedRadius(Long2ObjectOpenHashMap<BlockState> sigils, BlockPos center) {
        int r = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (long packed : sigils.keySet()) {
            m.set(packed);
            r = Math.max(r, Math.max(Math.abs(m.getX() - center.getX()), Math.abs(m.getZ() - center.getZ())));
        }
        return r;
    }

    private void recompute(Level lvl, BlockPos center) {
        ResourceLocation prevMatch = cachedMatch == null ? null : cachedMatch.getId();
        int prevMissing = missing.size();
        int prevWrong = wrongTypeSpots.size();
        boolean prevFluxia = hasSufficientFluxia;
        float prevFamEff = familiarEfficiency;
        float prevFamYield = familiarYield;
        int prevFamCap = familiarCapacity;
        int prevFamBound = familiarsBound;
        int prevFamSlots = familiarSlots;

        cachedMatch = null;
        clientMatchId = null;
        missing.clear();
        wrongTypeSpots.clear();
        hasSufficientFluxia = false;

        ensureRecipeCache(lvl);
        int globalR = cachedGlobalRadius;

        // The foundation changes rarely but is cheap to read, and the tank's capacity
        // depends on it, so refresh it on the same cadence as the circle.
        int prevTier = foundation.tier();
        float prevFoundEff = foundation.efficiency();
        float prevFoundYield = foundation.yieldChance();
        foundation = AltarTier.scan(lvl, center);
        refreshFamiliars(lvl, center);
        tank.setMax(foundation.totalCapacity() + familiarCapacity);

        // Read the ground and the dropped reagents exactly once each.
        Long2ObjectOpenHashMap<BlockState> sigils = scanSigils(lvl, center, globalR);
        List<ItemEntity> nearbyItems = lvl.getEntitiesOfClass(
                ItemEntity.class,
                AABB.unitCubeFromLowerCorner(center.getCenter()).inflate(globalR),
                ItemEntity::isAlive);

        boolean itemsNearby = !nearbyItems.isEmpty();
        int obsR = observedRadius(sigils, center);

        // Read once: the foundation and the familiars are both settled by this point, and
        // every candidate below is measured against the same discount.
        float reagentDiscount = totalEfficiency();

        RitualRecipe best = null;
        int bestExact = -1, bestCoord = -1, bestMiss = Integer.MAX_VALUE, bestRad = -1, bestFluxia = 0;
        boolean bestHasInputs = false;

        for (var holder : cachedRecipes) {
            RitualRecipe r = holder.value();
            int rad = Math.max(1, r.maxRadius());

            // A ring drawn wider than this recipe needs can't be this recipe.
            if (obsR > 0 && rad < obsR) continue;

            int exact = 0, coord = 0, missCnt = 0;

            for (var c : r.pattern()) {
                if (c.dx() == 0 && c.dz() == 0) continue;
                BlockState s = sigils.get(BlockPos.asLong(center.getX() + c.dx(), center.getY(), center.getZ() + c.dz()));

                boolean isSigil = s != null;
                boolean typeOK  = c.type().isEmpty()  || (isSigil && s.getValue(SigilBlock.TYPE)  == c.type().get());
                boolean colorOK = c.color().isEmpty() || (isSigil && s.getValue(SigilBlock.COLOR) == c.color().get());

                if (isSigil) coord++;
                if (isSigil && typeOK && colorOK) exact++;
                else if (c.required()) missCnt++;
            }

            boolean hasInputs = inputsPresent(nearbyItems, discountInputs(r.inputs(), reagentDiscount));

            boolean better = false;
            if (exact > bestExact) better = true;
            else if (exact == bestExact) {
                if (coord > bestCoord) better = true;
                else if (coord == bestCoord) {
                    if (hasInputs && !bestHasInputs) better = true;
                    else if (hasInputs == bestHasInputs) {
                        if (missCnt < bestMiss) better = true;
                        else if (missCnt == bestMiss && rad > bestRad) better = true;
                    }
                }
            }

            if (better) {
                best = r;
                bestExact = exact;
                bestCoord = coord;
                bestMiss = missCnt;
                bestHasInputs = hasInputs;
                bestRad = rad;
                bestFluxia = r.fluxiaRequired().orElse(0);

                if (missCnt == 0 && hasInputs) break;
            }
        }

        if (best != null && (bestCoord > 0 || itemsNearby)) {
            cachedMatch = best;
            collectDiagnostics(center, sigils, best);
            hasSufficientFluxia = tank.amount() >= effectiveCost(bestFluxia);
        }

        setChanged();

        // Only wake the network up when the picture the client draws actually changed.
        ResourceLocation nowMatch = cachedMatch == null ? null : cachedMatch.getId();
        // Every number the client prices a rite with has to be in here. Familiar state was
        // missing, so an altar that gained a second familiar recomputed on the server and
        // never told the client. The foundation's efficiency and yield were missing for the
        // same reason and are subtler: swapping one rune type for another inside a complete
        // ring changes both without changing the tier, so re-laying a foundation for better
        // runes left the altar quoting the old price until something else forced an update.
        if (!Objects.equals(prevMatch, nowMatch)
                || prevMissing != missing.size()
                || prevWrong != wrongTypeSpots.size()
                || prevFluxia != hasSufficientFluxia
                || prevTier != foundation.tier()
                || prevFoundEff != foundation.efficiency()
                || prevFoundYield != foundation.yieldChance()
                || prevFamEff != familiarEfficiency
                || prevFamYield != familiarYield
                || prevFamCap != familiarCapacity
                || prevFamBound != familiarsBound
                || prevFamSlots != familiarSlots) {
            pushClient();
        }
    }

    /**
     * Collects the boons of the familiars in this altar's service.
     *
     * <p>An altar can only attend to so many at once: one per {@linkplain AltarTier tier},
     * so a bare altar keeps one and a fully founded one keeps five. Familiars beyond that
     * still orbit and still belong to the altar — they simply contribute nothing until the
     * foundation grows under them.</p>
     *
     * <p>Boons stack with the rune foundation, but combined efficiency and yield are still
     * clamped, so no arrangement makes a rite free.</p>
     */
    private void refreshFamiliars(Level lvl, BlockPos center) {
        familiarEfficiency = 0f;
        familiarYield = 0f;
        familiarCapacity = 0;
        familiarNames.clear();

        List<FamiliarEntity> found = new ArrayList<>(lvl.getEntitiesOfClass(FamiliarEntity.class,
                new AABB(center).inflate(FamiliarEntity.SEARCH_RADIUS),
                f -> f.serves(center)));

        familiarsBound = found.size();
        familiarSlots = Math.max(1, foundation.tier());

        // Closest first, with the id as a tiebreak. Entity list order is not stable between
        // ticks, so without this the set of familiars actually in service would shuffle and
        // the altar's reserve would flicker whenever it is over capacity.
        Vec3 middle = Vec3.atCenterOf(center);
        found.sort(Comparator
                .comparingDouble((FamiliarEntity f) -> f.distanceToSqr(middle))
                .thenComparing(FamiliarEntity::getUUID));

        int serving = Math.min(familiarSlots, found.size());
        for (int i = 0; i < serving; i++) {
            FamiliarEntity f = found.get(i);
            FamiliarBoon boon = f.boon();
            familiarEfficiency += boon.efficiency();
            familiarYield += boon.yieldChance();
            familiarCapacity += boon.capacity();
            familiarNames.add(f.getDisplayName());
        }
    }

    /** Total cost reduction from runes and familiars together, capped at 75%. */
    private float totalEfficiency() {
        return Math.min(0.75f, foundation.efficiency() + familiarEfficiency);
    }

    /** Total bonus-output chance from runes and familiars together, capped at 75%. */
    private float totalYield() {
        return Math.min(0.75f, foundation.yieldChance() + familiarYield);
    }

    /** Cost of a rite after every discount that applies to it. */
    /**
     * Boon readouts for the block entity renderer.
     *
     * <p>All of this rides the same sync as the rest of the altar's state, so the floating
     * display picks up a familiar arriving or being banished on the next update rather than
     * showing whatever was true when the altar last changed shape.</p>
     */
    public float boonEfficiency() { return totalEfficiency(); }
    public float boonYield() { return totalYield(); }
    public int familiarsInService() { return familiarNames.size(); }
    public int familiarSlots() { return familiarSlots; }
    public int familiarsBound() { return familiarsBound; }

    public int effectiveCost(int baseCost) {
        return Math.max(0, Math.round(baseCost * (1f - totalEfficiency())));
    }

    // =====================================================================
    // Reagent discount
    // =====================================================================

    /**
     * The reagents a rite actually wants at this altar, after its boons.
     *
     * <p>Efficiency used to buy Fluxia and nothing else, so a Frugal familiar was worth
     * nothing at all to a player whose bottleneck was materials rather than current —
     * and on a rite like Magicka Solidificus the reagents <em>are</em> the cost. The same
     * fraction now comes off the reagent list, which is what makes a familiar felt on the
     * rites you cast in bulk rather than only on the reserve bar.</p>
     *
     * <p>Repeated {@link Ingredient}s are how a recipe expresses quantity, so the discount
     * drops surplus copies rather than rewriting any ingredient.</p>
     */
    public List<Ingredient> effectiveInputs(RitualRecipe r) {
        return discountInputs(r.inputs(), totalEfficiency());
    }

    /**
     * How many of one reagent a rite still wants once {@code efficiency} comes off.
     *
     * <p>Never below one, and never below what the recipe asked for if it asked for one.
     * A rite that wants a nether star goes on wanting a nether star however well appointed
     * the altar is: the discount is meant to make progression cheaper, never to delete a
     * gate the recipe exists to be.</p>
     */
    public static int discountedCount(int required, float efficiency) {
        if (required <= 1 || efficiency <= 0f) return required;
        return Mth.clamp((int) Math.ceil(required * (1f - efficiency)), 1, required);
    }

    /** Applies {@link #discountedCount} across a recipe's flattened ingredient list. */
    public static List<Ingredient> discountInputs(List<Ingredient> raw, float efficiency) {
        if (efficiency <= 0f || raw.size() < 2) return raw;

        Map<String, Integer> required = new LinkedHashMap<>();
        for (Ingredient ing : raw) required.merge(ingredientKey(ing), 1, Integer::sum);

        Map<String, Integer> budget = new HashMap<>(required.size());
        required.forEach((key, count) -> budget.put(key, discountedCount(count, efficiency)));

        List<Ingredient> out = new ArrayList<>(raw.size());
        for (Ingredient ing : raw) {
            String key = ingredientKey(ing);
            int left = budget.getOrDefault(key, 0);
            if (left <= 0) continue;
            budget.put(key, left - 1);
            out.add(ing);
        }
        return out;
    }

    /**
     * Identity used to group equal reagents. {@link Ingredient} gives no dependable
     * {@code equals} across tag- and item-backed entries, so two count as the same
     * reagent when they accept the same set of items. {@code getItems()} caches, so this
     * costs an allocation rather than a registry walk.
     */
    private static String ingredientKey(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        if (items == null || items.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : items) sb.append(BuiltInRegistries.ITEM.getKey(stack.getItem())).append('|');
        return sb.toString();
    }

    /** Fills {@link #missing} and {@link #wrongTypeSpots} for the chosen candidate. */
    private void collectDiagnostics(BlockPos center, Long2ObjectOpenHashMap<BlockState> sigils, RitualRecipe r) {
        for (var c : r.pattern()) {
            if (c.dx() == 0 && c.dz() == 0) continue; // never require the altar tile
            int wx = center.getX() + c.dx(), wy = center.getY(), wz = center.getZ() + c.dz();
            BlockState s = sigils.get(BlockPos.asLong(wx, wy, wz));

            if (s == null) {
                if (c.required()) missing.add(new MissingCell(wx, wy, wz, c.type(), c.color()));
                continue;
            }

            boolean wrongType  = c.type().isPresent()  && s.getValue(SigilBlock.TYPE)  != c.type().get();
            boolean wrongColor = c.color().isPresent() && s.getValue(SigilBlock.COLOR) != c.color().get();

            if ((wrongType || wrongColor) && c.required()) {
                missing.add(new MissingCell(wx, wy, wz, c.type(), c.color()));
            }
            // A sigil of the wrong *type* gets an angry particle; a missing one gets a ghost.
            if (wrongType && c.required()) {
                wrongTypeSpots.add(new BlockPos(wx, wy, wz));
            }
        }
    }

    private static boolean inputsPresent(List<ItemEntity> drops, List<Ingredient> needs) {
        if (needs.isEmpty()) return true;
        if (drops.isEmpty()) return false;

        var remaining = new ArrayList<>(needs);
        for (var ent : drops) {
            var stack = ent.getItem();
            int available = stack.getCount();
            boolean matched;
            do {
                matched = false;
                var it = remaining.iterator();
                while (it.hasNext() && available > 0) {
                    if (it.next().test(stack)) {
                        it.remove();
                        available--;
                        matched = true;
                        break;
                    }
                }
            } while (matched && !remaining.isEmpty() && available > 0);
            if (remaining.isEmpty()) return true;
        }
        return remaining.isEmpty();
    }

    // =====================================================================
    // Performing the rite
    // =====================================================================

    public Component tryPerformRitual(Player player) {
        if (level == null || level.isClientSide) return Component.empty();

        // An altar keeping a vigil is an altar you are not using for anything else. The
        // sneak click is the way back out, and it is the only interaction that gets past
        // this: a player who wants their altar back should not have to break the circle.
        if (activeVigil != null) {
            Component burning = vigilName();
            if (player.isSecondaryUseActive()) {
                if (level instanceof ServerLevel server) endVigil(server, VigilEnd.CANCELLED);
                return Component.translatable("ritual.wispwillow.vigil_cancelled", burning);
            }
            return Component.translatable("ritual.wispwillow.vigil_busy",
                    burning, Vigil.formatTicks(vigilRemaining()));
        }

        if (cachedMatch == null) return Component.translatable("ritual.wispwillow.none");

        Component name = Component.translatable(cachedMatch.getId().toLanguageKey());

        if (!missing.isEmpty()) {
            return Component.translatable("ritual.wispwillow.missing_glyphs", name, missing.size());
        }

        // Firm tier gate: a rite simply will not run on too shallow a foundation.
        if (foundation.tier() < cachedMatch.minTier()) {
            if (foundation.hasMisplacedRunes()) {
                return Component.translatable("ritual.wispwillow.runes_misplaced",
                        foundation.misplaced());
            }
            return Component.translatable("ritual.wispwillow.tier_too_low",
                    name, cachedMatch.minTier(), foundation.tier());
        }

        int cost = effectiveCost(cachedMatch.fluxiaRequired().orElse(0));
        if (tank.amount() < cost) {
            return Component.translatable("ritual.wispwillow.not_enough_fluxia", name, tank.amount(), cost);
        }

        int rr = Math.max(3, cachedMatch.maxRadius());

        // An awakening rite embues what is already lying in the circle, so there has to be
        // something lying there. This is checked before the reagents are taken: otherwise an
        // empty circle would quietly swallow the gold and the Fluxia and do nothing at all.
        List<ItemEntity> toAwaken = awakenableInCircle(level, worldPosition, cachedMatch, rr);
        if (cachedMatch.isAwakening() && toAwaken.isEmpty()) {
            return Component.translatable("ritual.wispwillow.nothing_to_awaken", name);
        }
        // An imbuing is the same bargain: no awakened piece that can take it, no charge.
        if (cachedMatch.isImbuing() && toAwaken.isEmpty()) {
            return Component.translatable("ritual.wispwillow.nothing_to_imbue", name);
        }

        if (!consumeInputsFromGround(level, worldPosition, effectiveInputs(cachedMatch), rr)) {
            return Component.translatable("ritual.wispwillow.missing_reagents", name);
        }

        // Reagents are gone; the Fluxia is spent too. Without this the cost was decorative.
        if (cost > 0) {
            tank.setFlux(tank.amount() - cost);
            hasSufficientFluxia = false;
        }

        // A vigil is struck rather than completed: the reagents and the rite's own cost
        // have just been taken, and what they bought is the burn that starts now.
        if (cachedMatch.isVigil()) {
            Vigil vigil = cachedMatch.vigil().orElseThrow();
            beginVigil(cachedMatch, vigil);
            return Component.translatable("ritual.wispwillow.vigil_begun",
                    name, Vigil.formatTicks(vigil.durationTicks()));
        }

        int awakened = performResults(cachedMatch, toAwaken, player);
        recheckCooldown = 0;
        setChanged();
        pushClient();
        if (cachedMatch.isAwakening()) {
            return Component.translatable("ritual.wispwillow.awakened", name, awakened);
        }
        if (cachedMatch.isImbuing()) {
            return Component.translatable("ritual.wispwillow.imbued", name, awakened,
                    cachedMatch.imbues().orElseThrow().displayName());
        }
        return Component.translatable("ritual.wispwillow.complete", name);
    }

    private static boolean consumeInputsFromGround(Level lvl, BlockPos center, List<Ingredient> needs, int radius) {
        if (needs.isEmpty()) return true;

        var drops = lvl.getEntitiesOfClass(
                ItemEntity.class,
                AABB.unitCubeFromLowerCorner(center.getCenter()).inflate(radius),
                ItemEntity::isAlive
        );
        if (drops.isEmpty()) return false;

        // Phase 1: PLAN consumption without modifying any stacks
        var remaining = new ArrayList<>(needs);
        Map<ItemEntity, Integer> planned = new HashMap<>();

        for (ItemEntity ent : drops) {
            ItemStack stack = ent.getItem();
            int available = stack.getCount();
            if (available <= 0) continue;

            boolean matched;
            do {
                matched = false;
                Iterator<Ingredient> it = remaining.iterator();
                while (it.hasNext() && available > 0) {
                    if (it.next().test(stack)) {
                        available--;
                        planned.merge(ent, 1, Integer::sum);
                        it.remove();
                        matched = true;
                        break;
                    }
                }
            } while (matched && !remaining.isEmpty() && available > 0);

            if (remaining.isEmpty()) break;
        }
        if (!remaining.isEmpty()) return false;

        // Phase 2: apply
        for (var entry : planned.entrySet()) {
            ItemEntity ent = entry.getKey();
            if (!ent.isAlive()) continue;

            int toConsume = entry.getValue();
            ItemStack stack = ent.getItem();
            boolean isBucket = stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET) || stack.is(Items.MILK_BUCKET);

            stack.shrink(toConsume);
            if (stack.isEmpty()) ent.discard();
            else ent.setItem(stack);

            if (isBucket) {
                // Buckets are containers, not consumables: give the empty one back.
                Containers.dropItemStack(lvl,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        new ItemStack(Items.BUCKET, toConsume));
            }
        }
        return true;
    }

    /**
     * @param awakenTargets the drops an awakening rite will embue, already gathered by the
     *                      caller so that the gate it applies and the count it reports back
     *                      describe the same set of items
     * @return how many stacks this rite actually awakened, zero for every other rite
     */
    private int performResults(RitualRecipe r, List<ItemEntity> awakenTargets, Player caster) {
        if (!(level instanceof ServerLevel server)) return 0;
        BlockPos center = worldPosition;

        // Blood runes occasionally double a rite's output. Rolled per cast, not per item,
        // so the bonus is a visible event rather than invisible drip.
        float yield = totalYield();
        boolean bonus = yield > 0f && server.random.nextFloat() < yield;

        r.resultItem().ifPresent(stk -> {
            Containers.dropItemStack(server,
                    center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                    stk.copy());
            if (bonus) {
                Containers.dropItemStack(server,
                        center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                        stk.copy());
            }
        });

        r.resultMobId().ifPresent(id -> {
            var lookup = server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);

            lookup.get(key).ifPresent(holder -> {
                EntityType<?> type = holder.value();
                int count = r.resultMobCount() + (bonus ? 1 : 0);
                for (int i = 0; i < count; i++) {
                    type.spawn(server, null, null, center.above(), MobSpawnType.EVENT, true, true);
                }
            });
        });

        // The empyrite is not a reagent and is never consumed: it stays lying where it
        // was thrown and simply comes up awake, or imbued. Copying the stack is what makes the
        // change reach the client -- ItemEntity syncs through entity data, which ignores a set()
        // whose value is the very instance it already holds.
        int awakened = 0;
        for (ItemEntity ent : awakenTargets) {
            if (!ent.isAlive()) continue;
            ItemStack stack = ent.getItem().copy();
            boolean changed = r.imbues().isPresent()
                    ? Awakening.imbue(stack, r.imbues().get())
                    : Awakening.awaken(stack);
            if (changed) {
                ent.setItem(stack);
                awakened++;
            }
        }
        if (awakened > 0) {
            server.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
            WWTriggers.fire(caster, r.imbues().isPresent() ? WWTriggers.IMBUED : WWTriggers.AWAKENED);
        }

        spawnCompletionBurst(server, center, r.maxRadius());
        lastStrike = server.getGameTime();
        server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (bonus) {
            server.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.4f);
        }
        return awakened;
    }

    /**
     * The drops inside the circle that an awakening or imbuing rite would embue.
     *
     * <p>Empty for every other rite, and empty for one whose circle holds only pieces it has
     * nothing left to do to — already awake, or already carrying this imbuement — so a second
     * casting over finished gear reports that there is nothing to do rather than charging for
     * the privilege.</p>
     */
    private static List<ItemEntity> awakenableInCircle(Level lvl, BlockPos center, RitualRecipe r, int radius) {
        java.util.function.Predicate<ItemStack> takes;
        if (r.isImbuing()) {
            var imbuement = r.imbues().get();
            takes = stack -> Awakening.canImbue(stack, imbuement);
        } else if (r.isAwakening()) {
            TagKey<net.minecraft.world.item.Item> tag = r.awakens().get();
            takes = stack -> stack.is(tag) && Awakening.canAwaken(stack);
        } else {
            return List.of();
        }
        return lvl.getEntitiesOfClass(
                ItemEntity.class,
                AABB.unitCubeFromLowerCorner(center.getCenter()).inflate(radius),
                ent -> ent.isAlive() && takes.test(ent.getItem())
        );
    }

    /**
     * The visual payoff for a completed rite: a ring of sparks out along the circle, a
     * column of light at the altar, and a chime.
     *
     * <p>Each {@code sendParticles} call is a single packet regardless of count, so this
     * is much cheaper than it looks.</p>
     */
    private static void spawnCompletionBurst(ServerLevel server, BlockPos center, int radius) {
        double cx = center.getX() + 0.5, cy = center.getY() + 1.0, cz = center.getZ() + 0.5;

        // Column of light straight up out of the altar.
        server.sendParticles(ParticleRegistry.FLUX_SPARK.get(),
                cx, cy + 0.5, cz, 40, 0.15, 0.8, 0.15, 0.02);
        server.sendParticles(ParticleTypes.END_ROD,
                cx, cy + 0.4, cz, 25, 0.1, 0.5, 0.1, 0.06);

        // A ring that races outward to the edge of the circle the player actually drew.
        int points = Math.max(12, radius * 8);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points;
            double dx = Math.cos(a), dz = Math.sin(a);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    cx + dx * radius, center.getY() + 0.15, cz + dz * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        server.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
        server.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        server.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    // =====================================================================
    // Vigils
    // =====================================================================

    /** Why a vigil stopped. Only {@link #SPENT} is graceful. */
    public enum VigilEnd { SPENT, BROKEN, STARVED, CANCELLED }

    public boolean hasVigil() { return activeVigil != null; }
    public Vigil activeVigil() { return activeVigil; }

    /** Ticks left on the burn; zero when nothing is burning. */
    public int vigilRemaining() {
        return activeVigil == null ? 0 : Math.max(0, activeVigil.durationTicks() - vigilAge);
    }

    /** What this altar's upkeep actually is, after the boons that come off every cost here. */
    public int vigilUpkeep() {
        return activeVigil == null ? 0 : effectiveCost(activeVigil.upkeepPerSecond());
    }

    private Component vigilName() {
        return vigilRiteId == null
                ? Component.translatable("ritual.wispwillow.vigil_unnamed")
                : Component.translatable(vigilRiteId.toLanguageKey());
    }

    /**
     * Lights a vigil. The rite's own cost and reagents have already been taken by the time
     * we get here — that is the price of striking it, and the upkeep is the price of
     * keeping it.
     */
    private void beginVigil(RitualRecipe rite, Vigil vigil) {
        activeVigil = vigil;
        vigilRiteId = rite.getId();
        vigilAge = 0;
        vigilCheckCooldown = VIGIL_CHECK_INTERVAL;
        if (level instanceof ServerLevel server) {
            spawnCompletionBurst(server, worldPosition, rite.maxRadius());
        }
        setChanged();
        pushClient();
    }

    private void tickVigil(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        Vigil vigil = activeVigil;

        // Upkeep before effect: a vigil that cannot pay for this second does not get to
        // act during it, so the last thing a player sees is the altar failing to afford
        // itself rather than one free second of weather.
        if (vigilAge % Vigil.UPKEEP_INTERVAL == 0) {
            int upkeep = vigilUpkeep();
            if (tank.amount() < upkeep) {
                endVigil(server, VigilEnd.STARVED);
                return;
            }
            if (upkeep > 0) {
                tank.setFlux(tank.amount() - upkeep);
                setChanged();
            }
            // The goggle readout is a second-hand view of all of this; a push every five
            // seconds keeps it honest without a block update every twenty ticks.
            if (vigilAge % (Vigil.UPKEEP_INTERVAL * 5) == 0) pushClient();
        }

        if (--vigilCheckCooldown <= 0) {
            vigilCheckCooldown = VIGIL_CHECK_INTERVAL;
            if (!vigilCircleIntact(level, pos)) {
                endVigil(server, VigilEnd.BROKEN);
                return;
            }
        }

        vigil.effect().apply(server, pos, vigilAge);
        vigil.effect().ambient(server, pos, vigilAge);

        if (++vigilAge >= vigil.durationTicks()) endVigil(server, VigilEnd.SPENT);
    }

    /**
     * Re-checks the vigil's own pattern on the ground rather than trusting the cached
     * match: the cache holds the best rite the circle currently describes, which is a
     * different question from whether the rite that was lit is still drawn.
     */
    private boolean vigilCircleIntact(Level level, BlockPos pos) {
        ensureRecipeCache(level);
        for (var holder : cachedRecipes) {
            if (holder.value().getId().equals(vigilRiteId)) {
                return holder.value().matchesAt(level, pos);
            }
        }
        // The rite has left the datapack. There is nothing left to check against, and
        // snuffing the vigil would punish the player for a change they did not make.
        return true;
    }

    private void endVigil(ServerLevel server, VigilEnd why) {
        Vigil vigil = activeVigil;
        Component name = vigilName();

        activeVigil = null;
        vigilRiteId = null;
        vigilAge = 0;
        setChanged();
        pushClient();
        recheckCooldown = 0;

        if (vigil == null) return;

        switch (why) {
            case SPENT -> server.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.8f, 1.0f);
            case BROKEN -> server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS, 1.0f, 0.6f);
            case STARVED -> server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.8f, 0.8f);
            case CANCELLED -> server.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        spawnVigilCollapse(server, vigil);
        // Whatever the vigil was holding, it stops holding it now. Only the Open Way has anything
        // to undo, and it has to run however the burn ended -- a broken circle must not strand
        // anyone in the Spirit Realm.
        vigil.effect().onEnd(server, worldPosition, why == VigilEnd.SPENT);

        // Three of the four ways a vigil ends happen to nobody in particular, so the
        // altar tells whoever is near it rather than whoever last clicked it.
        Component message = switch (why) {
            case SPENT     -> Component.translatable("ritual.wispwillow.vigil_spent", name);
            case BROKEN    -> Component.translatable("ritual.wispwillow.vigil_broken", name);
            case STARVED   -> Component.translatable("ritual.wispwillow.vigil_starved", name);
            case CANCELLED -> Component.translatable("ritual.wispwillow.vigil_cancelled", name);
        };
        for (Player p : server.players()) {
            if (p.blockPosition().distSqr(worldPosition) <= 32 * 32) {
                p.displayClientMessage(message, true);
            }
        }
    }

    /** The light going out: a ring of the vigil's own colour falling in on the altar. */
    private void spawnVigilCollapse(ServerLevel server, Vigil vigil) {
        int argb = vigil.effect().colour();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f), 1.4f);

        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (int i = 0; i < 24; i++) {
            double a = (Math.PI * 2.0 * i) / 24;
            server.sendParticles(dust,
                    cx + Math.cos(a) * 2.0, worldPosition.getY() + 1.1, cz + Math.sin(a) * 2.0,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // =====================================================================
    // Data for the renderer
    // =====================================================================

    /** Server: the computed match. Client: resolved from the synced recipe id. */
    public RitualRecipe getCachedMatch() { return cachedMatchOrClient(); }

    private RitualRecipe cachedMatchOrClient() {
        if (cachedMatch != null) return cachedMatch;
        if (clientMatchId == null || level == null) return null;

        // Recipes are synced to clients, so the id is enough to recover the whole layout.
        for (var holder : level.getRecipeManager().getAllRecipesFor(RitualRecipe.TYPE)) {
            if (holder.value().getId().equals(clientMatchId)) {
                cachedMatch = holder.value();
                return cachedMatch;
            }
        }
        return null;
    }

    public AltarTier.Foundation getFoundation() { return foundation; }

    public List<MissingCell> getMissing() { return Collections.unmodifiableList(missing); }
    public List<BlockPos> getWrongTypeSpots() { return Collections.unmodifiableList(wrongTypeSpots); }

    // =====================================================================
    // Persistence
    // =====================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);

        ListTag links = new ListTag();
        for (BlockPos other : linkedNodes) links.add(LongTag.valueOf(other.asLong()));
        tag.put(LINKS_TAG, links);

        if (activeVigil != null) {
            tag.put(VIGIL_TAG, activeVigil.save());
            tag.putInt(VIGIL_AGE_TAG, vigilAge);
            if (vigilRiteId != null) tag.putString(VIGIL_RITE_TAG, vigilRiteId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);

        linkedNodes.clear();
        if (tag.contains(LINKS_TAG)) {
            ListTag links = tag.getList(LINKS_TAG, LongTag.TAG_LONG);
            for (int i = 0; i < links.size(); i++) {
                linkedNodes.add(BlockPos.of(((LongTag) links.get(i)).getAsLong()));
            }
        }

        activeVigil = tag.contains(VIGIL_TAG) ? Vigil.load(tag.getCompound(VIGIL_TAG)) : null;
        vigilRiteId = tag.contains(VIGIL_RITE_TAG) ? ResourceLocation.tryParse(tag.getString(VIGIL_RITE_TAG)) : null;
        vigilAge = tag.getInt(VIGIL_AGE_TAG);
        vigilCheckCooldown = VIGIL_CHECK_INTERVAL;
        // A burn that was already over when the chunk unloaded should not come back for a
        // final tick; and an unreadable effect (a mod version behind) leaves nothing lit.
        if (activeVigil != null && vigilAge >= activeVigil.durationTicks()) {
            activeVigil = null;
            vigilRiteId = null;
            vigilAge = 0;
        }
    }

    /**
     * The renderer draws ghost glyphs, orbiting reagents and a result preview, all of
     * which live in the match state. None of it was ever sent to clients, so none of it
     * ever appeared; the update tag carries it now.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);

        if (cachedMatch != null) tag.putString(MATCH_TAG, cachedMatch.getId().toString());
        tag.putBoolean(HAS_FLUXIA_TAG, hasSufficientFluxia);
        tag.putInt(TIER_TAG, foundation.tier());
        tag.putInt(RUNES_TAG, foundation.runes());
        tag.putFloat(EFFICIENCY_TAG, foundation.efficiency());
        tag.putFloat(YIELD_TAG, foundation.yieldChance());
        tag.putInt(MISPLACED_TAG, foundation.misplaced());
        tag.putFloat("FamEff", familiarEfficiency);
        tag.putFloat("FamYield", familiarYield);
        tag.putInt("FamCap", familiarCapacity);
        tag.putInt("FamBound", familiarsBound);
        tag.putInt("FamSlots", familiarSlots);
        ListTag names = new ListTag();
        for (Component c : familiarNames) names.add(net.minecraft.nbt.StringTag.valueOf(c.getString()));
        tag.put("FamNames", names);

        ListTag missingList = new ListTag();
        for (MissingCell c : missing) {
            CompoundTag e = new CompoundTag();
            e.putInt("x", c.wx()); e.putInt("y", c.wy()); e.putInt("z", c.wz());
            c.type().ifPresent(t -> e.putString("t", t.getSerializedName()));
            c.color().ifPresent(col -> e.putString("c", col.getName()));
            missingList.add(e);
        }
        tag.put(MISSING_TAG, missingList);

        long[] wrong = new long[wrongTypeSpots.size()];
        for (int i = 0; i < wrong.length; i++) wrong[i] = wrongTypeSpots.get(i).asLong();
        tag.put(WRONG_TYPE_TAG, new LongArrayTag(wrong));
        if (lastStrike != Long.MIN_VALUE) tag.putLong(STRIKE_TAG, lastStrike);

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
        if (tag.contains(STRIKE_TAG)) lastStrike = tag.getLong(STRIKE_TAG);

        cachedMatch = null;
        clientMatchId = tag.contains(MATCH_TAG) ? ResourceLocation.tryParse(tag.getString(MATCH_TAG)) : null;
        hasSufficientFluxia = tag.getBoolean(HAS_FLUXIA_TAG);
        foundation = new AltarTier.Foundation(
                Math.max(1, tag.getInt(TIER_TAG)),
                tag.getInt(RUNES_TAG),
                0,
                tag.getFloat(EFFICIENCY_TAG),
                tag.getFloat(YIELD_TAG),
                java.util.Map.of(),
                tag.getInt(MISPLACED_TAG));
        familiarEfficiency = tag.getFloat("FamEff");
        familiarYield = tag.getFloat("FamYield");
        familiarCapacity = tag.getInt("FamCap");
        familiarsBound = tag.getInt("FamBound");
        familiarSlots = Math.max(1, tag.getInt("FamSlots"));
        familiarNames.clear();
        ListTag names = tag.getList("FamNames", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < names.size(); i++) familiarNames.add(Component.literal(names.getString(i)));
        tank.setMax(Math.max(tank.capacity(), 1));

        missing.clear();
        ListTag missingList = tag.getList(MISSING_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < missingList.size(); i++) {
            CompoundTag e = missingList.getCompound(i);
            missing.add(new MissingCell(
                    e.getInt("x"), e.getInt("y"), e.getInt("z"),
                    e.contains("t") ? sigilTypeByName(e.getString("t")) : Optional.empty(),
                    e.contains("c") ? Optional.ofNullable(DyeColor.byName(e.getString("c"), null)) : Optional.empty()
            ));
        }

        wrongTypeSpots.clear();
        for (long packed : tag.getLongArray(WRONG_TYPE_TAG)) wrongTypeSpots.add(BlockPos.of(packed));
    }

    private static Optional<SigilType> sigilTypeByName(String name) {
        for (SigilType t : SigilType.values()) {
            if (t.getSerializedName().equals(name)) return Optional.of(t);
        }
        return Optional.empty();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net,
                             ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag, lookupProvider);
    }

    // =====================================================================
    // Arcane Goggles
    // =====================================================================

    @Override
    public Component goggleTitle() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void addGoggleLines(List<Component> lines) {
        lines.add(GoggleText.amount("goggles.wispwillow.reserve", tank.amount(), tank.capacity(), "ƒ"));
        lines.add(GoggleText.line("goggles.wispwillow.tier",
                Component.literal("I".repeat(Math.min(5, foundation.tier())) + "  (" + foundation.runes() + " runes)")
                        .withStyle(net.minecraft.ChatFormatting.GOLD)));

        // The single most common mistake: runes laid beside the altar rather than under it.
        if (foundation.tier() == 1 && foundation.hasMisplacedRunes()) {
            lines.add(GoggleText.line("goggles.wispwillow.foundation",
                    GoggleText.warn(foundation.misplaced() + " runes on the wrong layer")));
            lines.add(Component.translatable("goggles.wispwillow.foundation_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        if (totalEfficiency() > 0f || totalYield() > 0f) {
            lines.add(GoggleText.line("goggles.wispwillow.boons",
                    Component.literal(Math.round(totalEfficiency() * 100) + "% cheaper, "
                            + Math.round(totalYield() * 100) + "% bonus")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)));
            lines.add(Component.translatable("goggles.wispwillow.boons_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        if (familiarsBound > 0) {
            // "2 / 3" reads as in-service over capacity, so an altar that is ignoring a
            // familiar says so instead of silently dropping it.
            String ratio = familiarNames.size() + " / " + familiarSlots;
            lines.add(GoggleText.line("goggles.wispwillow.familiars",
                    familiarsBound > familiarSlots
                            ? GoggleText.warn(ratio + "  (" + (familiarsBound - familiarSlots) + " idle)")
                            : GoggleText.good(ratio)));

            if (!familiarNames.isEmpty()) {
                lines.add(Component.literal("  " + String.join(", ",
                                familiarNames.stream().map(Component::getString).toList()))
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            if (familiarsBound > familiarSlots && foundation.tier() < AltarTier.MAX_TIER) {
                lines.add(Component.translatable("goggles.wispwillow.familiar_hint")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }

        // A burning vigil is the whole answer to "what is this altar doing": it is not
        // staging a rite, and the rite readout below would describe one it will refuse.
        if (activeVigil != null) {
            lines.add(GoggleText.line("goggles.wispwillow.vigil",
                    vigilName().copy().withStyle(net.minecraft.network.chat.Style.EMPTY
                            .withColor(activeVigil.effect().colour() & 0xFFFFFF))));
            lines.add(GoggleText.line("goggles.wispwillow.vigil_remaining",
                    Component.literal(Vigil.formatTicks(vigilRemaining()))
                            .withStyle(net.minecraft.ChatFormatting.AQUA)));

            int upkeep = vigilUpkeep();
            // Seconds of burn the reserve alone would buy: the number that says whether
            // this altar is living off its tank or off the network feeding it.
            int held = upkeep <= 0 ? Integer.MAX_VALUE : tank.amount() / upkeep;
            lines.add(GoggleText.line("goggles.wispwillow.vigil_upkeep",
                    held * 20 >= vigilRemaining()
                            ? GoggleText.good(upkeep + " ƒ/s")
                            : GoggleText.warn(upkeep + " ƒ/s  (" + held + "s held)")));
            return;
        }

        RitualRecipe match = cachedMatchOrClient();
        if (match == null) {
            lines.add(GoggleText.line("goggles.wispwillow.rite", GoggleText.warn("none detected")));
            return;
        }

        lines.add(GoggleText.line("goggles.wispwillow.rite",
                Component.translatable(match.getId().toLanguageKey())
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)));

        int cost = effectiveCost(match.fluxiaRequired().orElse(0));
        lines.add(GoggleText.line("goggles.wispwillow.cost",
                tank.amount() >= cost
                        ? GoggleText.good(GoggleText.num(cost) + " ƒ")
                        : GoggleText.warn(GoggleText.num(cost) + " ƒ")));

        // The reserve is only half the price of a rite; say what the reagents cost too,
        // and what the boons have taken off them.
        int reagents = effectiveInputs(match).size();
        int rawReagents = match.inputs().size();
        if (rawReagents > 0) {
            lines.add(GoggleText.line("goggles.wispwillow.reagents",
                    reagents < rawReagents
                            ? GoggleText.good(reagents + " items  (was " + rawReagents + ")")
                            : Component.literal(reagents + " items")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY)));
        }

        if (foundation.tier() < match.minTier()) {
            lines.add(GoggleText.line("goggles.wispwillow.needs_tier",
                    GoggleText.warn("tier " + match.minTier())));
        }

        lines.add(GoggleText.line("goggles.wispwillow.circle",
                missing.isEmpty()
                        ? GoggleText.good("complete")
                        : GoggleText.warn(missing.size() + " glyph(s) missing")));
    }

    @Override
    public float goggleBar() {
        // While a vigil burns the bar is the burn: how much of it is left to run.
        if (activeVigil != null && activeVigil.durationTicks() > 0) {
            return Mth.clamp((float) vigilRemaining() / activeVigil.durationTicks(), 0f, 1f);
        }

        RitualRecipe match = cachedMatchOrClient();
        // The price after boons, not the recipe sticker price: a full bar has to mean the
        // rite will fire, or a well appointed altar reads as short of Fluxia it does not need.
        int cost = match == null ? 0 : effectiveCost(match.fluxiaRequired().orElse(0));
        // While a rite is staged the bar tracks progress toward its cost, which is the
        // number you actually care about; otherwise it just shows the reserve.
        if (cost > 0) return Math.min(1f, (float) tank.amount() / cost);
        return tank.capacity() <= 0 ? -1f : (float) tank.amount() / tank.capacity();
    }
}
