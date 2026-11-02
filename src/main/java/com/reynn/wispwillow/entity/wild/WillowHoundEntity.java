package com.reynn.wispwillow.entity.wild;

import com.reynn.wispwillow.registry.EntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A hound woven from willow withes, with a knot of sap-light where its heart would be.
 *
 * <p>The mod's first tameable creature, and deliberately not a familiar. A familiar is bound to an
 * altar: deathless, weightless, and unable to leave the stone it orbits. A hound belongs to a
 * <em>person</em>. It follows you, it can be told to stay, it can be killed, and it will carry
 * things back to you.</p>
 *
 * <p>Tamed with a {@link ItemRegistry#WILLOW_SAP_BOTTLE Bottle of Willow Sap} — the same sap the
 * tree is tapped for, which is what the light in its chest is made of. The bottle comes back empty,
 * as it does when you drink it.</p>
 *
 * <p>The fetching is the point of keeping one. A tamed hound that is not sitting will go and pick
 * up loose items nearby and bring them to its owner's feet, which makes it worth having along while
 * you strip a cave or clear a tree, rather than just worth looking at.</p>
 */
public class WillowHoundEntity extends TamableAnimal implements GeoEntity {

    /** How far a hound will range from its owner to collect something. */
    private static final double FETCH_RANGE = 10.0;

    /** Odds a bottle of sap takes, per feeding. Low enough to cost a few bottles, like a wolf. */
    private static final int TAME_ODDS = 3;

    public WillowHoundEntity(EntityType<? extends WillowHoundEntity> type, Level level) {
        super(type, level);
        setTame(false, false);
        // Whatever it is carrying is its owner's, not loot: a hound that dies mid-errand drops it.
        setDropChance(EquipmentSlot.MAINHAND, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new FetchGoal(this));
        goalSelector.addGoal(5, new FollowOwnerNearbyGoal(this, 1.15, 9.0F, 3.0F));
        goalSelector.addGoal(6, new BreedGoal(this, 1.0));
        goalSelector.addGoal(7, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(8, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemRegistry.WILLOW_SAP_BOTTLE.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!isTame() && isFood(held)) {
            if (!level().isClientSide) {
                spendSap(player, hand, held);
                if (random.nextInt(TAME_ODDS) == 0) {
                    tame(player);
                    setOrderedToSit(false);
                    navigation.stop();
                    setTarget(null);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isTame()) {
            // Feeding a hurt hound mends it; feeding a healthy pair sets them courting.
            if (isFood(held) && getHealth() < getMaxHealth()) {
                if (!level().isClientSide) {
                    spendSap(player, hand, held);
                    heal(4.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (isFood(held) && !isBaby() && canFallInLove()) {
                if (!level().isClientSide) {
                    spendSap(player, hand, held);
                    setInLove(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            // An empty hand from its owner is the sit/stay toggle.
            if (isOwnedBy(player) && held.isEmpty()) {
                if (!level().isClientSide) {
                    setOrderedToSit(!isOrderedToSit());
                    jumping = false;
                    navigation.stop();
                    setTarget(null);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        return super.mobInteract(player, hand);
    }

    /** A sap bottle is consumed like a drink: the glass comes back unless you are in creative. */
    private void spendSap(Player player, InteractionHand hand, ItemStack held) {
        if (player.getAbilities().instabuild) return;
        held.shrink(1);
        ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
        if (held.isEmpty()) {
            player.setItemInHand(hand, empty);
        } else if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(14) == 0) {
            // The knot of sap-light in its chest, seen through the weave.
            Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
            level().addParticle(ParticleTypes.GLOW,
                    getX() + forward.x * 0.35, getY() + getBbHeight() * 0.62, getZ() + forward.z * 0.35,
                    0.0, 0.005, 0.0);
        }
    }

    /** What it is carrying, if anything. Empty the rest of the time. */
    public ItemStack getCarried() {
        return getItemBySlot(EquipmentSlot.MAINHAND);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        WillowHoundEntity pup = EntityRegistry.WILLOW_HOUND.get().create(level);
        if (pup != null && getOwnerUUID() != null) {
            pup.setOwnerUUID(getOwnerUUID());
            pup.setTame(true, true);
        }
        return pup;
    }

    /** Only tamed hounds breed, and never while one of them has been told to stay put. */
    @Override
    public boolean canMate(Animal partner) {
        if (partner == this || !isTame()) return false;
        if (!(partner instanceof WillowHoundEntity other) || !other.isTame()) return false;
        return !isOrderedToSit() && !other.isOrderedToSit() && isInLove() && other.isInLove();
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        if (isTame()) {
            LivingEntity owner = getOwner();
            if (entity == owner) return true;
            if (owner != null) return owner.isAlliedTo(entity);
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Carried", getCarried().saveOptional(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Carried")) {
            setItemSlot(EquipmentSlot.MAINHAND,
                    ItemStack.parseOptional(registryAccess(), tag.getCompound("Carried")));
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public float getVoicePitch() {
        // Lower and woodier than a wolf's; the same throat, made of green timber.
        return 0.6F + (random.nextFloat() - random.nextFloat()) * 0.1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isTame() ? SoundEvents.WOLF_AMBIENT : SoundEvents.WOLF_PANT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOOD_STEP, 0.16F, 0.9F);
    }

    /** Fen ground, at any hour: the hounds are as nocturnal as the fen itself, which is to say not. */
    public static boolean checkHoundSpawnRules(EntityType<WillowHoundEntity> type, ServerLevelAccessor level,
                                               MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                && isBrightEnoughToSpawn(level, pos);
    }

    /**
     * Go and get it, then bring it here.
     *
     * <p>Two phases in one goal, because they are one errand: a hound that picked something up and
     * then lost interest on the way back would be worse than one that never fetched at all.</p>
     */
    static class FetchGoal extends Goal {
        private final WillowHoundEntity hound;
        @Nullable private ItemEntity quarry;
        private int cooldown;

        FetchGoal(WillowHoundEntity hound) {
            this.hound = hound;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!hound.isTame() || hound.isOrderedToSit() || hound.isBaby()) return false;
            if (hound.getOwner() == null) return false;
            if (!hound.getCarried().isEmpty()) return true;  // already loaded: deliver it
            if (cooldown-- > 0) return false;

            List<ItemEntity> loose = hound.level().getEntitiesOfClass(ItemEntity.class,
                    hound.getBoundingBox().inflate(FETCH_RANGE),
                    item -> item.isAlive() && !item.hasPickUpDelay() && item.onGround()
                            && item.getOwner() != hound                       // not its own delivery
                            && item.distanceToSqr(hound.getOwner()) > 2.25);  // already at the owner's feet
            if (loose.isEmpty()) return false;

            quarry = loose.stream()
                    .min((a, b) -> Double.compare(hound.distanceToSqr(a), hound.distanceToSqr(b)))
                    .orElse(null);
            return quarry != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!hound.isTame() || hound.isOrderedToSit()) return false;
            if (!hound.getCarried().isEmpty()) return hound.getOwner() != null;
            return quarry != null && quarry.isAlive();
        }

        @Override
        public void stop() {
            quarry = null;
            hound.getNavigation().stop();
            cooldown = 20;
        }

        @Override
        public void tick() {
            ItemStack carried = hound.getCarried();

            if (carried.isEmpty()) {
                if (quarry == null) return;
                hound.getLookControl().setLookAt(quarry, 10.0F, hound.getMaxHeadXRot());
                hound.getNavigation().moveTo(quarry, 1.2);
                if (hound.distanceToSqr(quarry) < 2.0) {
                    hound.setItemSlot(EquipmentSlot.MAINHAND, quarry.getItem().copy());
                    hound.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 0.8F);
                    quarry.discard();
                    quarry = null;
                }
                return;
            }

            LivingEntity owner = hound.getOwner();
            if (owner == null) return;
            hound.getLookControl().setLookAt(owner, 10.0F, hound.getMaxHeadXRot());
            hound.getNavigation().moveTo(owner, 1.2);
            if (hound.distanceToSqr(owner) < 4.0) {
                // Set down at the owner's feet rather than forced into their inventory: a full
                // pack should not make the hound eat what it was asked to carry.
                ItemEntity dropped = new ItemEntity(hound.level(),
                        hound.getX(), hound.getY() + 0.4, hound.getZ(), carried.copy());
                dropped.setNoPickUpDelay();
                dropped.setThrower(hound);
                // Nudge it the last stretch toward the owner so it lands inside their pickup reach.
                var toOwner = owner.position().subtract(hound.position());
                dropped.setDeltaMovement(toOwner.x * 0.15, 0.1, toOwner.z * 0.15);
                hound.level().addFreshEntity(dropped);
                hound.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                hound.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.2F);
                cooldown = 20;
            }
        }
    }

    /**
     * Stay near, but not underfoot.
     *
     * <p>Vanilla's follow goal teleports the pet to its owner the moment it falls behind, which
     * suits a wolf being dragged across the world. A hound is meant to read as walking with you, so
     * this one only walks — it gives up and idles if the owner is genuinely unreachable.</p>
     */
    static class FollowOwnerNearbyGoal extends Goal {
        private final WillowHoundEntity hound;
        private final double speed;
        private final float start;
        private final float stop;
        @Nullable private LivingEntity owner;
        private int repath;

        FollowOwnerNearbyGoal(WillowHoundEntity hound, double speed, float start, float stop) {
            this.hound = hound;
            this.speed = speed;
            this.start = start;
            this.stop = stop;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = hound.getOwner();
            if (candidate == null || candidate.isSpectator()) return false;
            if (hound.isOrderedToSit()) return false;
            if (hound.distanceToSqr(candidate) < start * start) return false;
            owner = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            // Deliberately not testing the navigator here. canContinueToUse runs before the first
            // tick has had a chance to path, so an isDone() check stops the goal on the very tick
            // it starts and the hound never takes a step.
            return owner != null && !hound.isOrderedToSit()
                    && hound.distanceToSqr(owner) > stop * stop;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            owner = null;
            hound.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            hound.getLookControl().setLookAt(owner, 10.0F, hound.getMaxHeadXRot());
            if (--repath > 0) return;
            repath = adjustedTickDelay(10);
            hound.getNavigation().moveTo(owner, speed);
        }
    }

    // ---- the rig (GeckoLib) ------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.willow_hound.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.willow_hound.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.willow_hound.sit");

    /** Sitting wins; otherwise a trot when it is going somewhere, and the idle breathing when not. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
                    WillowHoundEntity hound = state.getAnimatable();
                    if (hound.isInSittingPose()) return state.setAndContinue(SIT);
                    return state.setAndContinue(hound.isTravelling(0.0004) ? WALK : IDLE);
                })
                // The trot keeps pace with the ground speed: a hound sent running does not amble.
                .setAnimationSpeedHandler(hound -> hound.isInSittingPose() ? 1.0
                        : Math.max(0.8, Math.min(2.2, hound.getDeltaMovement().horizontalDistance() * 9.0))));
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
