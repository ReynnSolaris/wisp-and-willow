package com.reynn.wispwillow.phasing;

import com.reynn.wispwillow.Config;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.EffectRegistry;
import com.reynn.wispwillow.registry.FluidRegistry;
import com.reynn.wispwillow.registry.ParticleRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Going under in Liquid Fluxia unmoors you from stone.
 *
 * <p>Put your head under and you are Phasing: you sink through whatever is beneath you
 * until you come out somewhere open, and then the phase ends and you are solid again. Stay
 * inside the rock when it runs out and the rock wins — that is ordinary suffocation, which
 * vanilla already does the moment {@code noPhysics} goes false.</p>
 *
 * <p>Three rules do the whole job:</p>
 * <ul>
 *   <li><b>Submerged refreshes it.</b> While your eyes are under, the timer is topped back
 *       up, so a deep pool is a place you can think in rather than a countdown.</li>
 *   <li><b>Open air ends it.</b> Out of the Fluxia and out of the stone, the phase is over
 *       at once — which is what makes a sealed room below a pool reachable and a wall you
 *       were merely swimming past not worth phasing into.</li>
 *   <li><b>Wards stop it.</b> Blocks tagged {@link TagRegistry.Blocks#PHASE_PROOF} keep
 *       their collision even for a phased player; see {@link #clampAgainstWards}.</li>
 * </ul>
 *
 * <p>This class is the server's half plus the shared maths. The drift itself is steered on
 * the client, where the jump key lives — see {@code PhasingClient}.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class Phasing {
    private Phasing() {}

    /** How long a phase lasts once your eyes come out of the Fluxia. */
    public static final int DRIFT_TICKS = 120;

    /** Only topped back up below this, so holding still in a pool is not a packet a tick. */
    private static final int REFRESH_BELOW = 60;

    /** Slow falling given to someone who drops out of a ceiling into open air. */
    private static final int LANDING_TICKS = 60;

    /** Blocks per tick, sinking under your own weight, inside the Fluxia. */
    public static final double DRIFT_IN_FLUXIA = 0.06;
    /** Blocks per tick, sinking through solid ground. */
    public static final double DRIFT_IN_STONE = 0.12;
    /** Sneaking: down, and meaning it. */
    public static final double DIVE = 0.28;
    /** Jumping: back the way you came. */
    public static final double RISE = 0.12;
    /** How much of your own sideways momentum survives a tick inside a block. */
    public static final double STONE_DRAG = 0.6;

    /**
     * Players who have been inside a block during the phase they are in now.
     *
     * <p>Only they get the soft landing, so stepping out of a shallow pool does not hand
     * out slow falling for nothing. Server thread only.</p>
     */
    private static final Set<UUID> EMBEDDED = new HashSet<>();


    public static boolean isPhasing(LivingEntity entity) {
        return entity.hasEffect(EffectRegistry.PHASING);
    }

    /** Eyes under: the only way in. Wading through a puddle of Fluxia is safe. */
    public static boolean submerged(Entity entity) {
        return entity.isEyeInFluidType(FluidRegistry.FLUXIA_FLUID_TYPE.get());
    }

    public static boolean touchingFluxia(Entity entity) {
        return entity.isInFluidType(FluidRegistry.FLUXIA_FLUID_TYPE.get());
    }

    /**
     * Whether this entity is sharing space with a block — measured against the box it would
     * occupy <em>standing</em>, not the one it happens to have now.
     *
     * <p>Someone who has just come up out of the Fluxia is still in a swimming pose, and that
     * short box fits through gaps the player themselves does not. Ending the phase there looks
     * right for one tick and then goes badly wrong: the pose springs back to standing inside
     * rock, the server rejects the next movement packet as a new collision, and the player is
     * teleported back to the last position it accepted — which is the middle of the stone they
     * had just drifted through. Asking about the standing box means the phase only ends
     * somewhere they can actually stand, which is what "come out somewhere open" has to mean.</p>
     */
    public static boolean insideBlocks(Entity entity) {
        AABB standing = entity.getDimensions(Pose.STANDING).makeBoundingBox(entity.position());
        return !entity.level().noBlockCollision(entity, standing);
    }

    // ------------------------------------------------------------------
    // Granting it, and taking it away again
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (submerged(player) && eligible(player) && Config.FLUXIA_PHASING.get()) {
            MobEffectInstance held = player.getEffect(EffectRegistry.PHASING);
            if (held == null || held.getDuration() < REFRESH_BELOW) {
                player.addEffect(new MobEffectInstance(
                        EffectRegistry.PHASING, DRIFT_TICKS, 0, true, false, true));
            }
            return; // still under: there is nothing to come out of yet
        }

        if (!isPhasing(player)) {
            EMBEDDED.remove(player.getUUID());
            return;
        }

        // Inside the rock: remember it, so we know a landing is owed when they leave it.
        if (insideBlocks(player)) {
            EMBEDDED.add(player.getUUID());
            return;
        }

        // Still in the pool, just not under it. Let the timer run.
        if (touchingFluxia(player)) return;

        player.removeEffect(EffectRegistry.PHASING);
        if (EMBEDDED.remove(player.getUUID())) surface(player);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EMBEDDED.remove(event.getEntity().getUUID());
    }

    private static boolean eligible(ServerPlayer player) {
        return !player.isSpectator() && !player.isPassenger();
    }

    /** Coming out of stone into open air: you do not fall the rest of the way, you settle. */
    private static void surface(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, LANDING_TICKS, 0, true, false, true));
        player.resetFallDistance();

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7f, 1.5f);
        level.sendParticles(ParticleRegistry.FLUX_SPARK.get(),
                player.getX(), player.getY() + 0.9, player.getZ(), 18, 0.35, 0.6, 0.35, 0.02);
    }

    // ------------------------------------------------------------------
    // Wards
    // ------------------------------------------------------------------

    /**
     * Trims a phased player's movement so warded blocks still stop them.
     *
     * <p>{@code noPhysics} is all or nothing, so the one exception has to be put back by
     * hand: gather the collision shapes of nearby warded blocks and run the same per-axis
     * sweep vanilla does, against those alone. Everything else stays transparent.</p>
     */
    public static Vec3 clampAgainstWards(Entity entity, Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-7) return movement;

        Level level = entity.level();
        AABB box = entity.getBoundingBox();
        AABB swept = box.expandTowards(movement).inflate(1.0E-7);

        // Padded by a block on every side, the way vanilla's own BlockCollisions cursor is.
        // Without it, a ward whose top face is exactly flush with the player's feet falls
        // outside the swept box and is never asked about — so you sink straight through the
        // one block that was supposed to hold you.
        List<VoxelShape> wards = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(swept.minX) - 1, Mth.floor(swept.minY) - 1, Mth.floor(swept.minZ) - 1,
                Mth.floor(swept.maxX) + 1, Mth.floor(swept.maxY) + 1, Mth.floor(swept.maxZ) + 1)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(TagRegistry.Blocks.PHASE_PROOF)) continue;
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                wards.add(shape.move(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        if (wards.isEmpty()) return movement;

        return collide(movement, box, wards);
    }

    /** Vanilla's order: settle vertically first, then the smaller horizontal axis last. */
    private static Vec3 collide(Vec3 movement, AABB box, List<VoxelShape> shapes) {
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;

        if (y != 0.0) {
            y = Shapes.collide(Direction.Axis.Y, box, shapes, y);
            if (y != 0.0) box = box.move(0.0, y, 0.0);
        }

        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0) {
            z = Shapes.collide(Direction.Axis.Z, box, shapes, z);
            if (z != 0.0) box = box.move(0.0, 0.0, z);
        }
        if (x != 0.0) {
            x = Shapes.collide(Direction.Axis.X, box, shapes, x);
            if (x != 0.0) box = box.move(x, 0.0, 0.0);
        }
        if (!zFirst && z != 0.0) {
            z = Shapes.collide(Direction.Axis.Z, box, shapes, z);
        }

        return new Vec3(x, y, z);
    }
}
