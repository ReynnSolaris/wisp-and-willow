package com.reynn.wispwillow.entity.wild;

import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A grey wading bird that stalks the shallows of the fen.
 *
 * <p>Herons are shy. A player walking up scatters them; one who sneaks, or who is holding a
 * fish, can get close. Left alone in shallow water a heron will stand stock still with its
 * head lowered and, every so often, strike at something under the surface.</p>
 *
 * <p>Water carries no pathfinding penalty for them, which is the whole trick to making a wader
 * wade: their strolls cross the shallows instead of skirting them the way a cow's would.</p>
 */
public class FenHeronEntity extends Animal implements GeoEntity {

    /** Entity event: the strike at the end of a fishing stance. */
    private static final byte EVENT_STRIKE = 100;
    private static final int STRIKE_TICKS = 10;

    private static final EntityDataAccessor<Boolean> DATA_FISHING =
            SynchedEntityData.defineId(FenHeronEntity.class, EntityDataSerializers.BOOLEAN);

    // Client-side animation state.
    public float flap;
    public float oFlap;
    public float flapSpeed;
    public float oFlapSpeed;
    private float flapping = 1.0F;
    private float headDip;
    private float oHeadDip;
    private int strikeTime;

    public FenHeronEntity(EntityType<? extends FenHeronEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, FenHeronEntity::startles,
                8.0F, 1.2, 1.6, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new FishGoal(this));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    /** A careful approach — sneaking, or bringing fish — does not send a heron off. */
    private static boolean startles(LivingEntity entity) {
        if (entity.isDiscrete()) return false;
        return !(entity.getMainHandItem().is(ItemTags.FISHES) || entity.getOffhandItem().is(ItemTags.FISHES));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FISHING, false);
    }

    public boolean isFishing() {
        return entityData.get(DATA_FISHING);
    }

    void setFishing(boolean fishing) {
        entityData.set(DATA_FISHING, fishing);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Wing flap and a gentle glide down, the way a chicken does it.
        oFlap = flap;
        oFlapSpeed = flapSpeed;
        flapSpeed = Mth.clamp(flapSpeed + (onGround() || isInWater() ? -1.0F : 4.0F) * 0.3F, 0.0F, 1.0F);
        if (!onGround() && flapping < 1.0F) flapping = 1.0F;
        flapping *= 0.9F;
        Vec3 motion = getDeltaMovement();
        if (!onGround() && !isInWater() && motion.y < 0.0) {
            setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
        }
        flap += flapping * 2.0F;

        if (level().isClientSide) {
            oHeadDip = headDip;
            headDip = Mth.approach(headDip, isFishing() ? 1.0F : 0.0F, 0.08F);
            if (strikeTime > 0) strikeTime--;
        }
    }

    public float getHeadDip(float partialTick) {
        return Mth.lerp(partialTick, oHeadDip, headDip);
    }

    /** 0 at rest, peaking at 1 halfway through a strike. */
    public float getStrike(float partialTick) {
        if (strikeTime <= 0) return 0.0F;
        float t = (strikeTime - partialTick) / STRIKE_TICKS;
        return Mth.sin(Math.max(t, 0.0F) * Mth.PI);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_STRIKE) {
            strikeTime = STRIKE_TICKS;
            triggerAnim("main", "strike");
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityRegistry.FEN_HERON.get().create(level);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + (random.nextFloat() - random.nextFloat()) * 0.1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHICKEN_STEP, 0.15F, 0.8F);
    }

    /** Grass or the wet ground a fen is made of, in daylight-level brightness. */
    public static boolean checkHeronSpawnRules(EntityType<FenHeronEntity> type, ServerLevelAccessor level,
                                               MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.MUD) || below.is(BlockTags.DIRT);
        return ground && isBrightEnoughToSpawn(level, pos);
    }

    /**
     * Stand still in the shallows with the head down, then strike.
     *
     * <p>Only starts while the heron is actually standing in water — feet wet, head dry — so a
     * swimming or stranded heron never tries it.</p>
     */
    static class FishGoal extends Goal {
        private final FenHeronEntity heron;
        private int remaining;

        FishGoal(FenHeronEntity heron) {
            this.heron = heron;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean wading() {
            BlockPos feet = heron.blockPosition();
            return heron.level().getFluidState(feet).is(FluidTags.WATER)
                    && !heron.level().getFluidState(feet.above()).is(FluidTags.WATER)
                    && heron.onGround();
        }

        @Override
        public boolean canUse() {
            return heron.getRandom().nextInt(reducedTickDelay(100)) == 0 && wading();
        }

        @Override
        public boolean canContinueToUse() {
            return remaining > 0 && heron.hurtTime == 0 && wading();
        }

        @Override
        public void start() {
            remaining = adjustedTickDelay(80 + heron.getRandom().nextInt(100));
            heron.getNavigation().stop();
            heron.setFishing(true);
        }

        @Override
        public void stop() {
            heron.setFishing(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            remaining--;
            heron.getNavigation().stop();
            if (remaining == 12) {
                heron.level().broadcastEntityEvent(heron, EVENT_STRIKE);
            } else if (remaining == 6 && heron.level() instanceof ServerLevel server) {
                Vec3 look = Vec3.directionFromRotation(0.0F, heron.getYRot());
                double x = heron.getX() + look.x * 0.9;
                double z = heron.getZ() + look.z * 0.9;
                server.sendParticles(ParticleTypes.SPLASH, x, heron.getY() + 0.3, z, 8, 0.15, 0.05, 0.15, 0.1);
                heron.playSound(SoundEvents.GENERIC_SPLASH, 0.3F, 1.4F);
            }
        }
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fen_heron.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.fen_heron.walk");
    private static final RawAnimation FISH = RawAnimation.begin().thenLoop("animation.fen_heron.fish");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fen_heron.fly");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("animation.fen_heron.strike");

    /**
     * Off the ground it glides on open wings; on its spot it fishes, neck folded to the water;
     * otherwise it stalks or stands. The strike is fired from the server's entity event.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
                    FenHeronEntity heron = state.getAnimatable();
                    if (!heron.onGround() && !heron.isInWater() && heron.fallDistance > 0.4f) {
                        return state.setAndContinue(FLY);
                    }
                    if (heron.isFishing()) return state.setAndContinue(FISH);
                    return state.setAndContinue(heron.isTravelling(0.0004) ? WALK : IDLE);
                })
                .setAnimationSpeedHandler(heron -> heron.isFishing() ? 1.0
                        : Math.max(0.8, Math.min(2.0, heron.getDeltaMovement().horizontalDistance() * 8.0)))
                .triggerableAnim("strike", STRIKE));
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
