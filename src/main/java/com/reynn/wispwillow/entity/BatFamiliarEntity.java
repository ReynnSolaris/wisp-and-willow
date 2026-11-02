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
 * The Bat Familiar: a little of everything, and the easiest of the four to find.
 *
 * <p>It carries {@link FamiliarBoon#BALANCED} — some cost reduction, some bonus chance and
 * some extra reserve, none of it as much as a specialist gives. With the frog taking
 * Bountiful, this is the last of the four boons to find a species, so every entry in
 * {@code FamiliarBoon} is now something you can actually bind to an altar.</p>
 *
 * <p>Bats live in every cave, which is deliberate: the generalist should be the one you can
 * get hold of first, and the specialists should be worth going looking for.</p>
 */
public class BatFamiliarEntity extends FamiliarEntity implements VariantHolder<BatFamiliarEntity.Variant>, GeoEntity {

    public static final String VARIANT_TAG = "Variant";

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(BatFamiliarEntity.class, EntityDataSerializers.INT);

    public BatFamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public FamiliarBoon boon() {
        return FamiliarBoon.BALANCED;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createFamiliarAttributes();
    }

    // ---- behaviour --------------------------------------------------------

    /** Fast, erratic, never level for long. A bat that flew in circles would not read as a bat. */
    private static final Act FLIT = new Act("flit") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            // A fresh waypoint about once a second is what makes it look flighty rather than
            // steered: it is always turning toward somewhere it has only just decided on.
            if (self.waypoint() == null || age % 18 == 0) {
                self.waypoint(self.randomVicinityPoint().add(0, 0.8, 0));
            }
            return self.waypoint();
        }
        @Override public double responsiveness() { return 0.26; }
        @Override public int duration(RandomSource rng) { return 100 + rng.nextInt(120); }
    };

    /** Hangs still and high, the way a bat at rest actually does. */
    private static final Act ROOST = new Act("roost") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            // Its own spot on the ring, so a colony lines the rafters instead of stacking
            // into a single point.
            return self.personalSpot(2.4, 2.6 + Math.sin(age * 0.02 + self.phase()) * 0.08);
        }
        @Override public double responsiveness() { return 0.09; }
        @Override public int duration(RandomSource rng) { return 140 + rng.nextInt(160); }
    };

    @Override
    protected List<Act> repertoire() {
        return List.of(FLIT, FLIT, FLIT, ROOST, ROOST, STARE);
    }

    /** It orbits on the wing and never lands, so it never wants gravity. */
    @Override
    public boolean isNoGravity() {
        return true;
    }

    // ---- flavour ----------------------------------------------------------

    @Nullable
    @Override
    public SoundEvent getAmbientSound() { return SoundEvents.BAT_AMBIENT; }

    @Override
    public SoundEvent getHurtSound(DamageSource source) { return SoundEvents.BAT_HURT; }

    @Nullable
    @Override
    public SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }

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
     * {@code textures/entity/familiars/bat_familiar_<name>.png} straight from
     * {@link #getName()}, so a name with no matching file renders as missing-texture.
     */
    public enum Variant implements StringRepresentable {
        DUSK(0, "dusk"),
        ASHEN(1, "ashen"),
        UMBRAL(2, "umbral");

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

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.bat_familiar.fly");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.bat_familiar.hover");

    /** It rides the air the whole time: a hard wingbeat under way, a slow one holding station. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            BatFamiliarEntity bat = state.getAnimatable();
            if (bat.isBanishing()) return state.setAndContinue(HOVER);
            return state.setAndContinue(bat.isTravelling(0.0016) ? FLY : HOVER);
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
