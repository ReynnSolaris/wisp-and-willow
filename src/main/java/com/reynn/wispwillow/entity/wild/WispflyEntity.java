package com.reynn.wispwillow.entity.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A firefly of the fens: a slow, drifting point of light that keeps low over the water.
 *
 * <p>Flight is steered the way a bat's is, by nudging velocity toward a waypoint, but the
 * waypoints are picked a little above the ground or water surface rather than anywhere in
 * the air. That keeps a swarm hanging over the reeds where it looks right, instead of
 * scattering into the canopy.</p>
 *
 * <p>Wispflies only come out after dark. Like any ambient creature they despawn once no one
 * is near, so the fen empties again by morning without any explicit bedtime.</p>
 */
public class WispflyEntity extends AmbientCreature implements GeoEntity {

    private static final double MAX_HOVER = 3.5;

    @Nullable
    private Vec3 waypoint;

    public WispflyEntity(EntityType<? extends WispflyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && random.nextInt(24) == 0) {
            level().addParticle(ParticleTypes.GLOW, getX(), getY() + 0.1, getZ(),
                    (random.nextDouble() - 0.5) * 0.01, -0.01, (random.nextDouble() - 0.5) * 0.01);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (waypoint == null || random.nextInt(50) == 0 || waypoint.distanceToSqr(position()) < 0.8
                || !level().isEmptyBlock(BlockPos.containing(waypoint))) {
            waypoint = pickWaypoint();
        }

        Vec3 motion = getDeltaMovement();
        double dx = waypoint.x - getX();
        double dy = waypoint.y - getY();
        double dz = waypoint.z - getZ();
        Vec3 steered = motion.add(
                (Math.signum(dx) * 0.16 - motion.x) * 0.05,
                (Math.signum(dy) * 0.10 - motion.y) * 0.07,
                (Math.signum(dz) * 0.16 - motion.z) * 0.05);
        setDeltaMovement(steered);

        float yaw = (float) (Mth.atan2(steered.z, steered.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3F);
        zza = 0.4F;
    }

    private Vec3 pickWaypoint() {
        double x = getX() + random.nextInt(9) - 4 + random.nextDouble();
        double z = getZ() + random.nextInt(9) - 4 + random.nextDouble();
        int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return new Vec3(x, surface + 0.8 + random.nextDouble() * MAX_HOVER, z);
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    /** Natural spawns wait for dusk and open sky; eggs and spawners are not held to it. */
    public static boolean checkWispflySpawnRules(EntityType<WispflyEntity> type, ServerLevelAccessor level,
                                                 MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (MobSpawnType.isSpawner(spawnType) || spawnType == MobSpawnType.SPAWN_EGG) {
            return checkMobSpawnRules(type, level, spawnType, pos, random);
        }
        // A dimension with no sky light has neither a night to wait for nor a sky to see: the
        // Spirit Realm is lit from nowhere, so both of the overworld's tests would fail there
        // forever and no mote would ever drift over the islands.
        var dimension = level.getLevel().dimensionType();
        boolean noDayCycle = !dimension.hasSkyLight() || dimension.hasFixedTime();
        boolean afterDark = noDayCycle || level.getLevel().isNight();
        boolean openAir = noDayCycle || level.canSeeSky(pos);
        return afterDark && openAir && checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.wispfly.fly");

    /** It never lands: one loop of wingbeats, bobbing and the lantern's slow pulse. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> state.setAndContinue(FLY)));
    }
}
