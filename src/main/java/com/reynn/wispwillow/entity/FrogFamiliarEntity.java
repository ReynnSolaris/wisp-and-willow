package com.reynn.wispwillow.entity;

import com.reynn.wispwillow.entity.familiar.FamiliarBoon;
import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.IntFunction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Frog Familiar: a patient, well-fed thing that makes an altar's work go further.
 *
 * <p>Where the axolotl makes a rite cheaper and the wisp makes the reserve deeper, the frog
 * is the one that occasionally hands back a second copy of what you asked for — the
 * {@link FamiliarBoon#BOUNTIFUL} boon, which until now nothing in the mod granted.</p>
 */
public class FrogFamiliarEntity extends FamiliarEntity implements VariantHolder<FrogFamiliarEntity.Variant>, GeoEntity {

    public static final String VARIANT_TAG = "Variant";

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(FrogFamiliarEntity.class, EntityDataSerializers.INT);

    public FrogFamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public FamiliarBoon boon() {
        return FamiliarBoon.BOUNTIFUL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createFamiliarAttributes();
    }

    // ---- behaviour --------------------------------------------------------

    /** Hops. On the ground, under gravity, with a real arc between landings. */
    private static final Act HOP = new Act("hop") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            if (self.waypoint() == null) self.waypoint(self.randomVicinityPoint());
            return self.waypoint();
        }
        @Override public boolean grounded() { return true; }
        @Override public void begin(FamiliarEntity self) { self.waypoint(self.randomVicinityPoint()); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            // The gap between hops is most of what makes it read as a frog. Hovering it
            // along at a constant height would have been much easier and completely wrong.
            if (age % 22 == 0) self.hop(0.42);
        }
        @Override public int duration(RandomSource rng) { return 120 + rng.nextInt(140); }
    };

    /** Sits. Croaks. Does nothing else, at length. */
    private static final Act SIT = new Act("sit") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            return self.personalSpot(2.0, 0.0);
        }
        @Override public boolean grounded() { return true; }
        @Override public int duration(RandomSource rng) { return 100 + rng.nextInt(180); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 60 != 40) return;
            level.playSound(null, self.blockPosition(), SoundEvents.FROG_AMBIENT,
                    SoundSource.NEUTRAL, 0.4f, 0.9f + level.random.nextFloat() * 0.3f);
            level.sendParticles(ParticleTypes.BUBBLE_POP,
                    self.getX(), self.getY() + 0.4, self.getZ(), 2, 0.1, 0.05, 0.1, 0.0);
        }
    };

    @Override
    protected List<Act> repertoire() {
        return List.of(HOP, HOP, HOP, SIT, SIT, STARE);
    }

    // ---- flavour ----------------------------------------------------------

    @Nullable
    @Override
    public SoundEvent getAmbientSound() { return SoundEvents.FROG_AMBIENT; }

    @Override
    public SoundEvent getHurtSound(DamageSource source) { return SoundEvents.FROG_HURT; }

    @Nullable
    @Override
    public SoundEvent getDeathSound() { return SoundEvents.FROG_DEATH; }

    // ---- variant ----------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(VARIANT_TAG, this.getVariant().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setVariant(Variant.byId(compound.getInt(VARIANT_TAG)));
    }

    @Override
    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    @Override
    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getId());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        Variant[] variants = Variant.values();
        setVariant(variants[level.getRandom().nextInt(variants.length)]);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /**
     * Names here are texture file names: the renderer builds
     * {@code textures/entity/familiars/frog_familiar_<name>.png} straight from
     * {@link #getName()}, so a name with no matching file renders as missing-texture.
     */
    public enum Variant implements StringRepresentable {
        TEMPERATE(0, "temperate"),
        WARM(1, "warm"),
        COLD(2, "cold");

        private static final IntFunction<Variant> BY_ID =
                ByIdMap.continuous(Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

        private final int id;
        private final String name;

        Variant(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }

        /** Used by the renderer to pick a texture. */
        public String getName() { return name; }

        @Override
        public String getSerializedName() { return name; }

        public static Variant byId(int id) { return BY_ID.apply(id); }

        public static Variant[] allVariants() { return Arrays.copyOf(values(), values().length); }
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.frog_familiar.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.frog_familiar.walk");

    /** A waddle when it is going somewhere; breathing, a pulsing throat and the odd blink when not. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            FrogFamiliarEntity frog = state.getAnimatable();
            if (frog.isBanishing()) return state.setAndContinue(IDLE);
            return state.setAndContinue(frog.isTravelling(0.0003) ? WALK : IDLE);
        }));
    }

    /**
     * Whether it is travelling, read from the position delta as well as the velocity: on the
     * client a server-driven mob's velocity is not reliably synced while its position is.
     */
    private boolean isTravelling(double thresholdSqr) {
        double dx = getX() - xOld, dy = getY() - yOld, dz = getZ() - zOld;
        return dx * dx + dy * dy + dz * dz > thresholdSqr
                || getDeltaMovement().horizontalDistanceSqr() > thresholdSqr;
    }
}
