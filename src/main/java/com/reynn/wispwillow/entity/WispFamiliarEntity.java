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
 * The Wisp Familiar: a mote of bound light that widens its altar's reserve.
 *
 * <p>Exists chiefly to demonstrate that {@link FamiliarEntity} is a template — this whole
 * species is a constructor, a boon and two sounds.</p>
 */
public class WispFamiliarEntity extends FamiliarEntity implements VariantHolder<WispFamiliarEntity.Variant>, GeoEntity {

    public WispFamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static final String VARIANT_TAG = "Variant";

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(WispFamiliarEntity.class, EntityDataSerializers.INT);

    @Override
    public FamiliarBoon boon() {
        return FamiliarBoon.CAPACIOUS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createFamiliarAttributes();
    }

    // ---- behaviour --------------------------------------------------------

    /**
     * A smooth loop traced along a cubic Bezier, with the control points re-rolled each lap.
     *
     * <p>A circle is the obvious way to make something hover, and it is exactly why the old
     * orbit looked mechanical: constant radius, constant speed, no acceleration anywhere. A
     * Bezier through four scattered points gives the wisp somewhere to gather speed and
     * somewhere to dawdle, and because the points move every lap it never quite repeats.</p>
     */
    private static final Act BEZIER = new Act("bezier") {
        static final int LAP = 160;

        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            int lap = age / LAP;
            float t = (age % LAP) / (float) LAP;
            Vec3 a = controlPoint(self, altar, lap, 0);
            Vec3 b = controlPoint(self, altar, lap, 1);
            Vec3 c = controlPoint(self, altar, lap, 2);
            // The next lap's first point, so one lap hands off to the next without a jump.
            Vec3 d = controlPoint(self, altar, lap + 1, 0);
            return cubic(a, b, c, d, t);
        }

        /** Deterministic in lap and index, so the curve holds still while it is being flown. */
        private Vec3 controlPoint(FamiliarEntity self, Vec3 altar, int lap, int index) {
            double seed = lap * 2.399963 + index * 1.618034 + self.phase();
            double angle = seed * 2.0;
            double radius = 1.4 + (Math.sin(seed * 3.1) * 0.5 + 0.5) * 2.2;
            double height = 0.2 + (Math.sin(seed * 1.7) * 0.5 + 0.5) * 1.9;
            return new Vec3(altar.x + Math.cos(angle) * radius,
                    altar.y + height,
                    altar.z + Math.sin(angle) * radius);
        }

        private Vec3 cubic(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
            float u = 1 - t;
            double w0 = u * u * u, w1 = 3 * u * u * t, w2 = 3 * u * t * t, w3 = t * t * t;
            return new Vec3(a.x * w0 + b.x * w1 + c.x * w2 + d.x * w3,
                    a.y * w0 + b.y * w1 + c.y * w2 + d.y * w3,
                    a.z * w0 + b.z * w1 + c.z * w2 + d.z * w3);
        }

        @Override public double responsiveness() { return 0.16; }
        @Override public int duration(RandomSource rng) { return LAP * (2 + rng.nextInt(2)); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 4 != 0) return;
            level.sendParticles(ParticleTypes.END_ROD,
                    self.getX(), self.getY() + 0.2, self.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    };

    /** Sinks almost to the altar stone and hangs there, guttering like a candle. */
    private static final Act GUTTER = new Act("gutter") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            double bob = Math.sin(age * 0.07 + self.phase()) * 0.22;
            return self.personalSpot(0.9, 0.15 + bob);
        }
        @Override public double responsiveness() { return 0.08; }
        @Override public int duration(RandomSource rng) { return 90 + rng.nextInt(110); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 10 != 0) return;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    self.getX(), self.getY() + 0.1, self.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    };

    @Override
    protected List<Act> repertoire() {
        return List.of(BEZIER, BEZIER, BEZIER, GUTTER, STARE);
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

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
        this.setVariant(WispFamiliarEntity.Variant.byId(compound.getInt(VARIANT_TAG)));
    }

    @Override
    public WispFamiliarEntity.Variant getVariant() {
        return WispFamiliarEntity.Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    @Override
    public void setVariant(WispFamiliarEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getId());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @org.jetbrains.annotations.Nullable SpawnGroupData spawnGroupData) {
        WispFamiliarEntity.Variant[] variants = WispFamiliarEntity.Variant.values();
        setVariant(variants[level.getRandom().nextInt(variants.length)]);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public enum Variant implements StringRepresentable {
        RED(0, "red"),
        BLUE(1, "blue"),
        PINK(2, "pink");

        private static final IntFunction<WispFamiliarEntity.Variant> BY_ID =
                ByIdMap.continuous(WispFamiliarEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

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

        public static WispFamiliarEntity.Variant byId(int id) { return BY_ID.apply(id); }

        public static WispFamiliarEntity.Variant[] allVariants() { return Arrays.copyOf(values(), values().length); }
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wisp_familiar.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.wisp_familiar.move");

    /** Leaning into its travel with the tail streaming when under way; hovering when not. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            WispFamiliarEntity wisp = state.getAnimatable();
            if (wisp.isBanishing()) return state.setAndContinue(IDLE);
            return state.setAndContinue(wisp.isTravelling(0.0012) ? MOVE : IDLE);
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
