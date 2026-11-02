package com.reynn.wispwillow.entity;

import com.reynn.wispwillow.entity.familiar.FamiliarBoon;
import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Raven: the author's familiar, and the only one that cannot be obtained by playing.
 *
 * <p>A bespoke GeckoLib rig ({@code geo/entity/raven_familiar}) rather than a recoloured
 * parrot: the animations are its own, and the entity's only job is to pick between them,
 * {@code fly} while it is going somewhere and {@code idle} once it has settled.</p>
 *
 * <p>What makes it read as different is the shimmer: a ribbon of colour that cycles the full
 * hue circle rather than sitting on a fixed tint like every other effect in the mod. Nothing
 * else in Wisp &amp; Willow does that, which is the point — it should be obvious at a glance
 * that this one is not part of the set.</p>
 */
public class RavenFamiliarEntity extends FamiliarEntity implements GeoEntity {

    /**
     * What the body is doing, synced so the client can pick an animation for it. The acts
     * themselves run only on the server; this is the one bit of them the renderer needs.
     */
    private static final EntityDataAccessor<Byte> POSE =
            SynchedEntityData.defineId(RavenFamiliarEntity.class, EntityDataSerializers.BYTE);
    private static final byte POSE_AIR = 0;
    private static final byte POSE_PERCHED = 1;
    private static final byte POSE_STOOP = 2;

    private static final RawAnimation PECK = RawAnimation.begin().thenPlay("animation.raven.peck");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.raven.walk");

    /** Ticks for one full trip around the colour wheel. */
    private static final float HUE_PERIOD = 200f;

    // ---- the rig ----------------------------------------------------------

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.raven.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.raven.fly");

    /** Squared blocks-per-tick below which the bird counts as hovering in place. */
    private static final double MOVING_THRESHOLD_SQR = 0.0016;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public RavenFamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public FamiliarBoon boon() {
        // The generalist's boon. A developer familiar that was also the strongest one would
        // be a boast rather than a signature.
        return FamiliarBoon.BALANCED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSE, POSE_AIR);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isBanishing()) return;
        Act act = currentAct();
        byte pose = POSE_AIR;
        if (act == STOOP) {
            pose = POSE_STOOP;
        } else if (act == SETTLE) {
            // Perched once it has actually arrived; it flies in first.
            Vec3 spot = act.target(this, altarCentre(), actAge());
            if (position().distanceToSqr(spot) < 0.35 * 0.35) pose = POSE_PERCHED;
        }
        if (entityData.get(POSE) != pose) entityData.set(POSE, pose);
    }

    public boolean isPerched() { return entityData.get(POSE) == POSE_PERCHED; }
    public boolean isStooping() { return entityData.get(POSE) == POSE_STOOP; }

    public static AttributeSupplier.Builder createAttributes() {
        return createFamiliarAttributes();
    }

    /** It rides the air the whole time, like the bat. */
    @Override
    public boolean isNoGravity() {
        return true;
    }

    /**
     * One controller, two states. It has no gravity and never walks, so there is no ground
     * animation to choose: moving means flying, and anything else means the folded idle.
     *
     * <p>Movement is read from the position delta as well as the velocity, because on the
     * client a server-driven mob's velocity is not reliably synced while its position is.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flight", 4, state -> {
                    RavenFamiliarEntity raven = state.getAnimatable();
                    if (raven.isBanishing()) return state.setAndContinue(IDLE);
                    if (raven.isPerched()) {
                        // On its spot: folded, with a shuffle if the perch bob has it moving.
                        double dx = raven.getX() - raven.xOld, dz = raven.getZ() - raven.zOld;
                        return state.setAndContinue(dx * dx + dz * dz > 0.0009 ? WALK : IDLE);
                    }
                    double dx = raven.getX() - raven.xOld;
                    double dy = raven.getY() - raven.yOld;
                    double dz = raven.getZ() - raven.zOld;
                    boolean moving = dx * dx + dy * dy + dz * dz > MOVING_THRESHOLD_SQR
                            || raven.getDeltaMovement().horizontalDistanceSqr() > MOVING_THRESHOLD_SQR;
                    return state.setAndContinue(moving ? FLY : IDLE);
                })
                // The stoop beats harder; everything else at the animation's own pace.
                .setAnimationSpeedHandler(raven -> raven.isStooping() ? 1.7 : 1.0)
                // Fired from the server while perched; plays once over the idle and returns.
                .triggerableAnim("peck", PECK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ---- the shimmer ------------------------------------------------------

    /**
     * The current colour, as a smooth trip around the hue circle.
     *
     * <p>Driven off {@code tickCount} plus the entity's phase, so two ravens are never on
     * the same colour and neither is tied to world time.</p>
     */
    public int shimmerColour(float partialTick) {
        float hue = ((tickCount + partialTick) / HUE_PERIOD + (float) (phase() / (Math.PI * 2))) % 1.0f;
        return Mth.hsvToRgb(hue, 0.65f, 1.0f);
    }

    /**
     * A swirl of colour trailing the bird.
     *
     * <p>{@code ENTITY_EFFECT} is the potion-swirl particle, and in 1.21 it carries its own
     * colour rather than reading one from the entity — which is exactly what is needed to
     * walk it around the hue circle instead of leaving it a fixed tint.</p>
     */
    private void shimmer(ServerLevel level, float spread) {
        ColorParticleOption colour =
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, shimmerColour(0f) | 0xFF000000);
        level.sendParticles(colour,
                getX(), getY() + 0.3, getZ(), 2, spread, spread * 0.6, spread, 0.0);
    }

    // ---- behaviour --------------------------------------------------------

    /** A wide, unhurried circle, higher than anything else in the shrine flies. */
    private static final Act GLIDE = new Act("glide") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            double angle = age * 0.026 + self.phase();
            // Radius breathes slowly, so the circle never quite closes on itself.
            double radius = 3.0 + Math.sin(age * 0.011) * 0.9;
            return new Vec3(altar.x + Math.cos(angle) * radius,
                    altar.y + 2.2 + Math.sin(age * 0.017) * 0.5,
                    altar.z + Math.sin(angle) * radius);
        }
        @Override public double responsiveness() { return 0.14; }
        @Override public int duration(RandomSource rng) { return 180 + rng.nextInt(160); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 3 == 0 && self instanceof RavenFamiliarEntity raven) raven.shimmer(level, 0.12f);
        }
    };

    /** Drops onto its own spot on the altar rim and sits there watching, wings folded. */
    private static final Act SETTLE = new Act("settle") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            // On the altar's own slab (the centre is 1.1 above the block, the slab 0.9), a
            // little off centre so the offering still shows, and held still: a bird that bobs
            // in place while "sitting" is hovering, not perched.
            return self.personalSpot(0.3, -0.18);
        }
        @Override public double responsiveness() { return 0.10; }
        @Override public int duration(RandomSource rng) { return 140 + rng.nextInt(180); }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (age % 9 == 0 && self instanceof RavenFamiliarEntity raven) raven.shimmer(level, 0.06f);
            if (age % 60 == 35 && self instanceof RavenFamiliarEntity raven && raven.isPerched()
                    && level.getRandom().nextInt(3) != 0) {
                raven.triggerAnim("flight", "peck");
            }
            if (age % 110 == 70) {
                level.playSound(null, self.blockPosition(), SoundEvents.PARROT_AMBIENT,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.3f, 0.55f);
            }
        }
    };

    /** A sharp dive and pull-up. Rare, and the only fast thing it ever does. */
    private static final Act STOOP = new Act("stoop") {
        @Override public Vec3 target(FamiliarEntity self, Vec3 altar, int age) {
            float t = Mth.clamp(age / 45.0f, 0f, 1f);
            // Down fast, back up slower: a parabola read from the top.
            double height = 3.2 - 3.0 * Math.sin(t * Math.PI);
            double angle = self.phase() + t * 2.4;
            return new Vec3(altar.x + Math.cos(angle) * 2.0,
                    altar.y + height,
                    altar.z + Math.sin(angle) * 2.0);
        }
        @Override public double responsiveness() { return 0.3; }
        @Override public int duration(RandomSource rng) { return 50; }
        @Override public void tick(FamiliarEntity self, ServerLevel level, int age) {
            if (self instanceof RavenFamiliarEntity raven) raven.shimmer(level, 0.2f);
        }
    };

    @Override
    protected List<Act> repertoire() {
        return List.of(GLIDE, GLIDE, GLIDE, SETTLE, SETTLE, STOOP, STARE);
    }

    // ---- flavour ----------------------------------------------------------

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Nullable
    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
