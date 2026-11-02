package com.reynn.wispwillow.entity.spirit;

import com.reynn.wispwillow.registry.EntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A pale deer of the Spirit Realm, and the only thing there that will let you near it.
 *
 * <p>The realm is small and deliberately empty, so its one creature has to carry all of it: a
 * glimmerfawn can be tamed, can be bred, and grows a coat of {@link ItemRegistry#SPIRIT_DOWN spirit
 * down} that can be sheared and grows back. Three uses in one animal beats three animals with one
 * use each in a place whose whole character is emptiness.</p>
 *
 * <p>All of it runs on {@link ItemRegistry#MOONLILY Moonlily}, which grows only in the Willow Fen.
 * The realm's husbandry therefore depends on something carried in from the overworld — you cannot
 * bootstrap a herd from inside, and a vigil is thirty minutes long, so bring flowers.</p>
 *
 * <p>Tamed fawns come home with you: {@link com.reynn.wispwillow.world.SpiritTravel} walks a
 * player's pets through the way behind them when the vigil ends.</p>
 */
public class GlimmerfawnEntity extends TamableAnimal implements GeoEntity {

    /** Entity event: the coat coming off, for the client's particle burst. */
    private static final byte EVENT_SHEARED = 101;

    private static final EntityDataAccessor<Boolean> DATA_SHEARED =
            SynchedEntityData.defineId(GlimmerfawnEntity.class, EntityDataSerializers.BOOLEAN);

    /** Odds a moonlily takes, per feeding. */
    private static final int TAME_ODDS = 3;

    /** Ticks for a shorn coat to come back on its own — about five minutes. */
    private static final int REGROW_MIN = 6000;
    private static final int REGROW_MAX = 9000;

    private int regrow;

    public GlimmerfawnEntity(EntityType<? extends GlimmerfawnEntity> type, Level level) {
        super(type, level);
        setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new PanicGoal(this, 1.8));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHEARED, false);
    }

    public boolean isSheared() {
        return entityData.get(DATA_SHEARED);
    }

    private void setSheared(boolean sheared) {
        entityData.set(DATA_SHEARED, sheared);
    }

    /** A grown fawn with its coat on. Babies keep theirs. */
    public boolean readyForShearing() {
        return isAlive() && !isSheared() && !isBaby();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemRegistry.MOONLILY.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(Items.SHEARS) && readyForShearing()) {
            if (level() instanceof ServerLevel server) {
                shear(server);
                held.hurtAndBreak(1, player, getSlotForHand(hand));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isFood(held)) {
            if (!isTame()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    if (random.nextInt(TAME_ODDS) == 0) {
                        tame(player);
                        navigation.stop();
                        level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            // A flower on a shorn coat brings it straight back: husbandry beats waiting.
            if (isSheared() && !isBaby()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setSheared(false);
                    regrow = 0;
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            if (isBaby()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    ageUp(getSpeedUpSecondsWhenFeeding(-getAge()), true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            if (canFallInLove()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setInLove(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        if (isTame() && isOwnedBy(player) && held.isEmpty()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                navigation.stop();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    /** Takes the coat off and puts it on the ground, the way shearing a sheep does. */
    public void shear(ServerLevel level) {
        setSheared(true);
        regrow = REGROW_MIN + random.nextInt(REGROW_MAX - REGROW_MIN);
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.broadcastEntityEvent(this, EVENT_SHEARED);

        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            spawnAtLocation(new ItemStack(ItemRegistry.SPIRIT_DOWN.get()));
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SHEARED) {
            for (int i = 0; i < 12; i++) {
                level().addParticle(ParticleTypes.END_ROD,
                        getRandomX(0.7), getY(0.6), getRandomZ(0.7),
                        (random.nextDouble() - 0.5) * 0.06, random.nextDouble() * 0.05, (random.nextDouble() - 0.5) * 0.06);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide) {
            if (isSheared() && regrow > 0 && --regrow == 0) {
                setSheared(false);
            }
            return;
        }

        // Motes coming off the coat — the realm's light, caught in it. Shorn fawns stop shedding,
        // which is how you spot one across a field.
        if (!isSheared() && random.nextInt(18) == 0) {
            level().addParticle(ParticleTypes.END_ROD,
                    getRandomX(0.6), getY(0.7), getRandomZ(0.6),
                    0.0, 0.01, 0.0);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        GlimmerfawnEntity calf = EntityRegistry.GLIMMERFAWN.get().create(level);
        if (calf != null && getOwnerUUID() != null) {
            calf.setOwnerUUID(getOwnerUUID());
            calf.setTame(true, true);
        }
        return calf;
    }

    @Override
    public boolean canMate(Animal partner) {
        if (partner == this) return false;
        if (!(partner instanceof GlimmerfawnEntity other)) return false;
        return !isOrderedToSit() && !other.isOrderedToSit() && isInLove() && other.isInLove();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Sheared", isSheared());
        tag.putInt("Regrow", regrow);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSheared(tag.getBoolean("Sheared"));
        regrow = tag.getInt("Regrow");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 360;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + (random.nextFloat() - random.nextFloat()) * 0.1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.AMETHYST_BLOCK_STEP, 0.12F, 1.4F);
    }

    /**
     * Any solid ground in the realm, at any light level.
     *
     * <p>No brightness check on purpose. The Spirit Realm's sky is fixed at dusk, so the usual
     * {@code isBrightEnoughToSpawn} test — written for an overworld that has a noon — would keep
     * the only creature in the dimension from ever appearing in it.</p>
     */
    public static boolean checkGlimmerfawnSpawnRules(EntityType<GlimmerfawnEntity> type, ServerLevelAccessor level,
                                                     MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        // End stone counts: the realm's island crowns are only painted with soil where the floor
        // feature reached, and a herd confined to the painted patches would leave most of the
        // islands empty.
        boolean ground = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(Blocks.END_STONE)
                || below.is(Blocks.MOSS_BLOCK);
        return ground
                && level.getBlockState(pos).isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND);
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.glimmerfawn.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.glimmerfawn.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.glimmerfawn.sit");
    private static final RawAnimation GRAZE = RawAnimation.begin().thenPlay("animation.glimmerfawn.graze");

    /** Client-side: the tick the current graze ends, so a herd never all puts its head down at once. */
    private int grazeUntil;

    /**
     * Sitting wins; walking when it is going somewhere; standing still it grazes now and then.
     * A baby keeps its head up: it has not learned to.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
                    GlimmerfawnEntity fawn = state.getAnimatable();
                    if (fawn.isInSittingPose()) return state.setAndContinue(SIT);
                    if (fawn.isTravelling(0.0004)) {
                        fawn.grazeUntil = 0;
                        return state.setAndContinue(WALK);
                    }
                    if (fawn.tickCount < fawn.grazeUntil) return state.setAndContinue(GRAZE);
                    if (!fawn.isBaby() && fawn.random.nextInt(320) == 0) {
                        fawn.grazeUntil = fawn.tickCount + 100;
                        return state.setAndContinue(GRAZE);
                    }
                    return state.setAndContinue(IDLE);
                })
                .setAnimationSpeedHandler(fawn -> fawn.isInSittingPose() ? 1.0
                        : Math.max(0.8, Math.min(2.2, fawn.getDeltaMovement().horizontalDistance() * 9.0))));
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
