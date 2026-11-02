package com.reynn.wispwillow.entity.familiar;

import com.reynn.wispwillow.blocks.AltarBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A familiar: a small, deathless companion bound to a Willow Altar.
 *
 * <p>Designed as a template. A new species needs an entity class extending this, a
 * {@link FamiliarBoon}, a model and a renderer — everything else (binding, orbiting,
 * immortality, rehoming, banishment) lives here.</p>
 *
 * <p>Behaviour contract:</p>
 * <ul>
 *   <li>Bound to one altar; drifts in a slow orbit around it.</li>
 *   <li>Invulnerable to everything, and never despawns.</li>
 *   <li>If its altar is destroyed it looks for another within {@link #SEARCH_RADIUS};
 *       finding none it settles and waits.</li>
 *   <li>Only a Soul Wisp removes it, and that is a deliberate two-second dissolve.</li>
 * </ul>
 */
public abstract class FamiliarEntity extends PathfinderMob {

    /** How far a homeless familiar will look for a new altar. */
    public static final int SEARCH_RADIUS = 24;

    /** Radius of the idle orbit, in blocks. */
    private static final double ORBIT_RADIUS = 2.2;
    private static final double ORBIT_SPEED = 0.012;

    /**
     * How far from its altar a familiar will ever get under its own power.
     *
     * <p>Much tighter than {@link #SEARCH_RADIUS}, which is only about finding a new home
     * when the old one is broken. A familiar is meant to read as belonging to its altar, and
     * something drifting twenty blocks away reads as lost.</p>
     */
    private static final double VICINITY_RADIUS = 5.0;



    /** Ticks the banishment animation runs before the entity is removed. */
    public static final int BANISH_TICKS = 40;

    private static final EntityDataAccessor<Boolean> BANISHING =
            SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOMELESS =
            SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private BlockPos boundAltar;
    private double orbitPhase;

    private Act currentAct = DRIFT;
    private int idleTimer;
    private int actAge;
    /** Scratch space for whichever act is running; cleared whenever one is chosen. */
    @Nullable private Vec3 waypoint;
    /** Fixed per entity, so two of a kind never move in lockstep. See {@link #phase()}. */
    private double phaseOffset;
    private int banishTimer = -1;
    private int rehomeCooldown;

    protected FamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.orbitPhase = level.random.nextDouble() * Math.PI * 2;
        this.phaseOffset = level.random.nextDouble() * Math.PI * 2;
        setPersistenceRequired();
        setInvulnerable(true);
    }

    /** What this familiar grants to the altar it is bound to. */
    public abstract FamiliarBoon boon();

    // ==================================================================
    // Setup
    // ==================================================================

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createFamiliarAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        // Deliberately sparse: a familiar's whole job is to hover near its altar and
        // look interested. Anything more elaborate fights the orbit below.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BANISHING, false);
        builder.define(HOMELESS, false);
    }

    // ==================================================================
    // Immortality and persistence
    // ==================================================================

    @Override public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean requiresCustomPersistence() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(net.minecraft.world.entity.Entity entity) { }
    @Override public boolean canBeLeashed() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    /** Familiars ignore fall damage and drowning along with everything else. */
    @Override public boolean fireImmune() { return true; }
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) { return false; }

    // ==================================================================
    // Binding
    // ==================================================================

    public void bindTo(@Nullable BlockPos altar) {
        this.boundAltar = altar == null ? null : altar.immutable();
        this.entityData.set(HOMELESS, altar == null);
        // Leash to the vicinity, not the search radius. SEARCH_RADIUS is how far a homeless
        // familiar will look for a new altar; it was never meant to be how far a bound one
        // is allowed to roam, and using it here let them drift most of a chunk away.
        if (altar != null) restrictTo(altar, (int) Math.ceil(VICINITY_RADIUS) + 1);
    }

    @Nullable
    public BlockPos boundAltar() { return boundAltar; }

    public boolean isHomeless() { return this.entityData.get(HOMELESS); }

    public boolean isBanishing() { return this.entityData.get(BANISHING); }

    /** True when this familiar is actively serving the altar at {@code pos}. */
    public boolean serves(BlockPos pos) {
        return !isBanishing() && boundAltar != null && boundAltar.equals(pos);
    }

    // ==================================================================
    // Ticking
    // ==================================================================

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            if (isBanishing()) spawnBanishParticles();
            return;
        }

        if (banishTimer >= 0) {
            tickBanish((ServerLevel) level());
            return;
        }

        // Bind on the first tick after spawning, whatever spawned us.
        if (boundAltar == null && !isHomeless()) {
            bindToNearestAltar(this, (ServerLevel) level(), blockPosition());
        }

        // Re-home if the altar we were bound to is gone.
        if (rehomeCooldown-- <= 0) {
            rehomeCooldown = 40;
            verifyOrRehome((ServerLevel) level());
        }

        if (boundAltar != null) idleTick();
    }

    // ==================================================================
    // Idle behaviour
    // ==================================================================

    /**
     * One thing a familiar can be doing.
     *
     * <p>These live on the species, not on this class. A shared repertoire meant every
     * familiar moved identically — and the moment a player owned two, the illusion that
     * these were creatures rather than instances of one entity collapsed: they would perch
     * in the same spot, at the same height, on the same schedule. A bat should fly like a
     * bat. A frog has no business hovering at all.</p>
     */
    public abstract static class Act {
        private final String name;

        protected Act(String name) { this.name = name; }

        public String name() { return name; }

        /** Where the familiar wants to be this tick, in world space. */
        public abstract Vec3 target(FamiliarEntity self, Vec3 altar, int age);

        /** How hard it steers toward that. Higher is snappier. */
        public double responsiveness() { return 0.12; }

        public int duration(RandomSource rng) { return 80 + rng.nextInt(140); }

        /** Whether this act can be chosen right now. */
        public boolean available(FamiliarEntity self) { return true; }

        /**
         * True while the familiar walks or hops instead of flying.
         *
         * <p>Grounded acts hand movement back to gravity and to the entity's own jump, so a
         * frog arcs and lands rather than sliding along at a fixed altitude.</p>
         */
        public boolean grounded() { return false; }

        public void begin(FamiliarEntity self) {}

        /** Server-side flourishes: particles, sounds. Called every tick the act is active. */
        public void tick(FamiliarEntity self, ServerLevel level, int age) {}
    }

    // ---- acts every familiar has ----------------------------------------

    /**
     * Comes over to a nearby player and hovers, watching them.
     *
     * <p>The one act that is shared deliberately: it is about the player, not the species,
     * and it is the moment a familiar stops being scenery.</p>
     */
    public static final Act STARE = new Act("stare") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            Player p = self.nearestPlayer();
            if (p == null) return altar.add(0, 1.0, 0);
            Vec3 eye = p.getEyePosition();
            Vec3 away = self.position().subtract(eye);
            if (away.lengthSqr() < 1.0e-4) away = new Vec3(1, 0, 0);
            // Stop short of them. Hovering inside someone's face is unsettling, not charming.
            return eye.add(away.normalize().scale(1.7)).add(0, 0.15, 0);
        }
        @Override public boolean available(FamiliarEntity self) { return self.nearestPlayer() != null; }
        @Override public int duration(RandomSource rng) { return 70 + rng.nextInt(90); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 12 != 0) return;
            level.sendParticles(ParticleTypes.END_ROD,
                    self.getX(), self.getY() + 0.35, self.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    };

    /** Drifts to a random point near the altar. The filler between the interesting acts. */
    public static final Act DRIFT = new Act("drift") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            if (self.waypoint() == null) self.waypoint(self.randomVicinityPoint());
            return self.waypoint();
        }
        @Override public void begin(FamiliarEntity self) { self.waypoint(self.randomVicinityPoint()); }
    };

    /**
     * The species' repertoire. Override to give a familiar its own way of moving.
     *
     * <p>The base list is what a thing with no particular body plan would do.</p>
     */
    protected List<Act> repertoire() {
        return List.of(DRIFT, DRIFT, STARE);
    }

    /**
     * Every familiar's own phase offset.
     *
     * <p>Two of the same species standing in the same shrine must not move in lockstep, and
     * the acts that aim at a single spot — a perch, a roost — must not aim at the <em>same</em>
     * spot. Everything that could synchronise is skewed by this.</p>
     */
    public double phase() { return phaseOffset; }

    /** The act currently running, for species that animate differently per act. */
    protected Act currentAct() { return currentAct; }

    /** Ticks the current act has been running. */
    protected int actAge() { return actAge; }

    private void idleTick() {
        if (recoverIfStrayed()) return;

        if (idleTimer-- <= 0) chooseAct();
        actAge++;

        Vec3 altar = altarCentre();
        Vec3 target = clampToVicinity(currentAct.target(this, altar, actAge));

        if (currentAct.grounded()) {
            walkToward(target);
        } else {
            flyToward(target, currentAct.responsiveness());
        }

        if (level() instanceof ServerLevel sl) currentAct.tick(this, sl, actAge);
    }

    /**
     * Flies the familiar home if it has ended up outside its leash, overriding whatever it
     * was doing.
     *
     * <p>Clamping the act's <em>target</em> is not enough on its own. A grounded act hands
     * movement back to gravity, so a frog that hops off the edge of the platform its altar
     * stands on is in free fall with a target it can no longer reach — and a familiar that
     * has fallen out of the world is gone for good, because nothing else pulls it back.</p>
     *
     * @return true if the familiar is recovering and the running act should be skipped
     */
    private boolean recoverIfStrayed() {
        Vec3 home = altarCentre();
        double dx = getX() - home.x;
        double dz = getZ() - home.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double drop = home.y - getY();

        if (horizontal <= VICINITY_RADIUS + 2.0 && drop <= 4.0) return false;

        // Straight home, under its own power, ignoring the ground entirely.
        flyToward(home.add(0, 0.6, 0), 0.18);
        return true;
    }

    private void chooseAct() {
        RandomSource rng = getRandom();
        List<Act> acts = repertoire();

        // Rejection-sample rather than filter-then-pick: the unavailable case is rare, and
        // a familiar with nothing available should keep doing whatever it was doing.
        for (int attempt = 0; attempt < 8; attempt++) {
            Act pick = acts.get(rng.nextInt(acts.size()));
            if (!pick.available(this)) continue;
            currentAct = pick;
            break;
        }

        idleTimer = currentAct.duration(rng);
        actAge = 0;
        waypoint = null;
        currentAct.begin(this);
    }

    // ---- helpers the acts build on --------------------------------------

    public Vec3 altarCentre() {
        return boundAltar == null ? position()
                : new Vec3(boundAltar.getX() + 0.5, boundAltar.getY() + 1.1, boundAltar.getZ() + 0.5);
    }

    /**
     * The waypoint the running act is steering toward, if it keeps one.
     *
     * <p>Exposed through accessors rather than as a field because the species live in a
     * different package from this class, and an act is only ever handed the familiar it
     * belongs to.</p>
     */
    @Nullable public Vec3 waypoint() { return waypoint; }

    public void waypoint(@Nullable Vec3 point) { this.waypoint = point; }

    public Vec3 randomVicinityPoint() {
        RandomSource rng = getRandom();
        Vec3 c = altarCentre();
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist = 1.2 + rng.nextDouble() * (VICINITY_RADIUS - 1.6);
        return new Vec3(c.x + Math.cos(angle) * dist,
                c.y + (rng.nextDouble() - 0.35) * 2.0,
                c.z + Math.sin(angle) * dist);
    }

    /** A stable point on the ring around the altar, unique to this familiar. */
    public Vec3 personalSpot(double radius, double height) {
        Vec3 c = altarCentre();
        return new Vec3(c.x + Math.cos(phaseOffset) * radius,
                c.y + height,
                c.z + Math.sin(phaseOffset) * radius);
    }

    @Nullable
    public Player nearestPlayer() {
        return level().getNearestPlayer(this, VICINITY_RADIUS + 3.0);
    }

    /** Pulls any target back inside the leash, so no act can wander the familiar off. */
    private Vec3 clampToVicinity(Vec3 target) {
        Vec3 c = altarCentre();
        Vec3 offset = target.subtract(c);
        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (horizontal > VICINITY_RADIUS) {
            double scale = VICINITY_RADIUS / horizontal;
            offset = new Vec3(offset.x * scale, offset.y, offset.z * scale);
        }
        return c.add(offset.x, Mth.clamp(offset.y, -2.0, 4.0), offset.z);
    }

    /** Direct steering, for anything that flies. Pathfinding would zig-zag. */
    private void flyToward(Vec3 target, double responsiveness) {
        Vec3 delta = target.subtract(position()).scale(responsiveness);
        // Cap the correction so a teleport or a chunk reload does not fling it.
        if (delta.lengthSqr() > 0.25) delta = delta.normalize().scale(0.5);

        setDeltaMovement(delta);
        setNoGravity(true);
        faceTravel(delta);
    }

    /**
     * Movement for grounded acts: walk, and hop over anything in the way.
     *
     * <p>Gravity comes back on here. A frog that hovered a block above the floor to get
     * around a fence post would not be a frog.</p>
     */
    private void walkToward(Vec3 target) {
        setNoGravity(false);

        Vec3 flat = new Vec3(target.x - getX(), 0, target.z - getZ());
        if (flat.lengthSqr() < 0.09) {
            // Arrived. Keep the horizontal velocity, let gravity own the rest.
            setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
            return;
        }

        Vec3 step = flat.normalize().scale(0.08);
        setDeltaMovement(getDeltaMovement().x * 0.7 + step.x,
                getDeltaMovement().y,
                getDeltaMovement().z * 0.7 + step.z);
        faceTravel(step);
    }

    /** A vertical shove, for hopping acts. Only fires when actually standing on something. */
    public void hop(double power) {
        if (!onGround()) return;
        setDeltaMovement(getDeltaMovement().add(0, power, 0));
    }

    private void faceTravel(Vec3 delta) {
        if (delta.horizontalDistanceSqr() <= 1.0e-4) return;
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        yBodyRot = yaw;
    }

    /** Confirms the bound altar still exists, otherwise looks for a new home. */
    private void verifyOrRehome(ServerLevel level) {
        if (boundAltar != null && level.isLoaded(boundAltar)
                && level.getBlockState(boundAltar).getBlock() instanceof AltarBlock) {
            this.entityData.set(HOMELESS, false);
            return;
        }

        BlockPos replacement = findNearestAltar(level, blockPosition());
        if (replacement != null) {
            bindTo(replacement);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.6, getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        } else {
            // Nothing to serve: settle where it stands and wait to be found.
            boundAltar = null;
            this.entityData.set(HOMELESS, true);
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Nullable
    public static BlockPos findNearestAltar(Level level, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    m.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                    if (!(level.getBlockState(m).getBlock() instanceof AltarBlock)) continue;
                    double d = m.distSqr(from);
                    if (d < bestDist) {
                        bestDist = d;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }

    // ==================================================================
    // Banishment
    // ==================================================================

    /** Begins the dissolve. The entity is removed when the animation finishes. */
    public void beginBanish() {
        if (banishTimer >= 0) return;
        banishTimer = BANISH_TICKS;
        this.entityData.set(BANISHING, true);
        setNoGravity(true);
        setDeltaMovement(0, 0.02, 0);
    }

    private void tickBanish(ServerLevel level) {
        banishTimer--;

        // Rise and spin as it goes.
        setDeltaMovement(0, 0.05, 0);
        setYRot(getYRot() + 18.0f);

        if (banishTimer % 3 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    getX(), getY() + 0.4, getZ(), 4, 0.25, 0.3, 0.25, 0.02);
        }

        if (banishTimer <= 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                    getX(), getY() + 0.5, getZ(), 24, 0.4, 0.5, 0.4, 0.08);
            level.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.ILLUSIONER_MIRROR_MOVE,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.4f);
            discard();
        }
    }

    private void spawnBanishParticles() {
        double a = level().random.nextDouble() * Math.PI * 2;
        double r = 0.4 + level().random.nextDouble() * 0.3;
        level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                getX() + Math.cos(a) * r, getY() + level().random.nextDouble() * 0.8,
                getZ() + Math.sin(a) * r,
                0, 0.03, 0);
    }

    // ==================================================================
    // Persistence
    // ==================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (boundAltar != null) {
            tag.putLong("BoundAltar", boundAltar.asLong());
        }
        tag.putDouble("OrbitPhase", orbitPhase);
        tag.putDouble("PhaseOffset", phaseOffset);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BoundAltar")) {
            bindTo(BlockPos.of(tag.getLong("BoundAltar")));
        } else {
            this.entityData.set(HOMELESS, true);
        }
        orbitPhase = tag.getDouble("OrbitPhase");
        if (tag.contains("PhaseOffset")) phaseOffset = tag.getDouble("PhaseOffset");
        setPersistenceRequired();
    }

    /** Convenience for the summoning ritual and the spawn egg. */
    public static void bindToNearestAltar(FamiliarEntity familiar, ServerLevel level, BlockPos origin) {
        familiar.bindTo(findNearestAltar(level, origin));
    }
}
