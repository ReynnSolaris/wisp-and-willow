package com.reynn.wispwillow.entity;

import com.google.common.collect.Maps;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;
import java.util.function.IntFunction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Axolotl Familiar: a frugal companion that halves the cost of every rite performed
 * at the altar it is bound to.
 *
 * <p>This is the reference implementation of {@link FamiliarEntity}. A new species needs
 * only a class like this one, a {@link FamiliarBoon}, and a renderer.</p>
 */
public class AxolotlFamiliarEntity extends FamiliarEntity
        implements LerpingModel, VariantHolder<AxolotlFamiliarEntity.Variant>, GeoEntity {

    public static final AnimationState idleAnimationState = new AnimationState();
    public static final String VARIANT_TAG = "Variant";

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(AxolotlFamiliarEntity.class, EntityDataSerializers.INT);

    private final Map<String, Vector3f> modelRotationValues = Maps.newHashMap();

    public AxolotlFamiliarEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FamiliarBoon boon() {
        return FamiliarBoon.FRUGAL;
    }

    // ---- attributes -------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return createFamiliarAttributes();
    }

    // ---- behaviour --------------------------------------------------------

    /** Swims through the air the way it would through water: slow, undulating, low. */
    private static final Act PADDLE = new Act("paddle") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            if (self.waypoint() == null || age % 70 == 0) {
                self.waypoint(self.randomVicinityPoint());
            }
            // The side-to-side is the whole point: it is what separates swimming from flying.
            Vec3 to = self.waypoint();
            Vec3 flat = new Vec3(to.x - self.getX(), 0, to.z - self.getZ());
            Vec3 side = flat.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0)
                    : new Vec3(-flat.z, 0, flat.x).normalize();
            double sway = Math.sin(age * 0.11 + self.phase()) * 0.45;
            return to.add(side.scale(sway)).add(0, Math.sin(age * 0.06) * 0.2, 0);
        }
        @Override public double responsiveness() { return 0.07; }
        @Override public int duration(RandomSource rng) { return 140 + rng.nextInt(160); }
    };

    /** Settles on the floor by the altar and stays there, entirely unbothered. */
    private static final Act LOAF = new Act("loaf") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            return self.personalSpot(1.8, 0.0);
        }
        @Override public boolean grounded() { return true; }
        @Override public int duration(RandomSource rng) { return 160 + rng.nextInt(200); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 45 != 0) return;
            level.sendParticles(ParticleTypes.BUBBLE_POP,
                    self.getX(), self.getY() + 0.3, self.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        }
    };

    @Override
    protected List<Act> repertoire() {
        return List.of(PADDLE, PADDLE, LOAF, LOAF, STARE);
    }

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

    /**
     * Pick a gem at random. Without this every familiar spawns as variant 0 and five of the
     * six textures are dead weight in the jar.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        Variant[] variants = Variant.values();
        setVariant(variants[level.getRandom().nextInt(variants.length)]);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ---- flavour ----------------------------------------------------------

    @Override public int getMaxHeadXRot() { return 1; }
    @Override public int getMaxHeadYRot() { return 1; }

    @Override
    public SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Nullable
    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        // It floats rather than swims now, so it always uses the airborne idle.
        return SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    public @NotNull SoundEvent getSwimSplashSound() { return SoundEvents.AXOLOTL_SPLASH; }

    @Override
    public @NotNull SoundEvent getSwimSound() { return SoundEvents.AXOLOTL_SWIM; }

    @Override
    public Map<String, Vector3f> getModelRotationValues() {
        return this.modelRotationValues;
    }

    // ---- variants ---------------------------------------------------------

    /**
     * Names here are the texture file names: the renderer builds
     * {@code textures/entity/familiars/axolotl_familiar_<name>.png} straight from
     * {@link #getName()}, so a name with no matching file renders as missing-texture.
     */
    public enum Variant implements StringRepresentable {
        AMETHYST(0, "amethyst"),
        AZURITE(1, "azurite"),
        CITRINE(2, "citrine"),
        EMERALD(3, "emerald"),
        OPAL(4, "opal"),
        RUBY(5, "ruby");

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

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.axolotl_familiar.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.axolotl_familiar.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.axolotl_familiar.swim");

    /** In water it swims; on land it crawls when going somewhere and settles when not. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            AxolotlFamiliarEntity axolotl = state.getAnimatable();
            if (axolotl.isBanishing()) return state.setAndContinue(IDLE);
            if (axolotl.isInWaterOrBubble()) return state.setAndContinue(SWIM);
            return state.setAndContinue(axolotl.isTravelling(0.0003) ? WALK : IDLE);
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
