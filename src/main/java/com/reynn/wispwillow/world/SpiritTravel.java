package com.reynn.wispwillow.world;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.worldgen.spirit.SpiritRealm;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Going into the Spirit Realm, and being put back out of it.
 *
 * <p>Where a player came from is written onto the player, not into the altar that sent them: an
 * altar can be broken, its chunk unloaded, or its vigil ended by something that never knew who
 * walked through. Carrying the return point on the traveller means the way home survives all of
 * that, and a player who logs out mid-vigil still lands somewhere sensible when the realm closes
 * around them.</p>
 */
public final class SpiritTravel {
    private SpiritTravel() {}

    /** How close a tamed animal has to be standing to be counted as coming with you. */
    private static final double ESCORT_RADIUS = 12.0;

    /** How far out to look for an island before giving up and building a pad, in chunks. */
    private static final int ARRIVAL_SEARCH_CHUNKS = 6;

    /** Where that pad goes. Mid-height, so it hangs in the void like everything else here. */
    private static final int LANDING_Y = 70;

    /** How far to one side the return gate stands, so you do not arrive inside it. */
    private static final int RETURN_GATE_OFFSET = 2;
    private static final int RETURN_GATE_HEIGHT = 2;

    /**
     * What happens on the far side of a transition: the portal noise, and — the part that matters —
     * a chunk ticket at the destination.
     *
     * <p>Without the ticket a traveller is inserted into a chunk nothing is holding loaded. The
     * arrival coordinates are correct and the server even reports them correctly, and then the player
     * falls straight through terrain that has not finished coming into existence: measured on
     * 2026-09-16 as a clean arrival at y=60 followed by 110 blocks of free fall to y=-58. Over an
     * island field with a void underneath, that is the difference between landing and dying.</p>
     */
    private static final DimensionTransition.PostDimensionTransition ARRIVE =
            DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET);

    private static final String RETURN_TAG = "WispwillowSpiritReturn";
    private static final String LANDING_TAG = "WispwillowSpiritLanding";
    private static final String LANDED_AT = "Time";

    /** How far a returning traveller has to walk before the gate will take them again. */
    private static final double LANDING_CLEAR = 4.0;

    /**
     * How long a landing mark can suppress the gate at all, whatever else happens.
     *
     * <p>This is the safety net, and the first version did not have one. A mark is only ever
     * <em>read</em> when someone is at a gate, and the code that drops it lives inside that read —
     * so walking away never cleared anything, because nothing was looking. Since a return puts you
     * back where you left from, and where you left from is the altar, the mark simply stayed there:
     * light the vigil again, step in, and be refused, for ever. Every trip after the first was
     * blocked and the rite looked broken.</p>
     *
     * <p>Thirty seconds outlasts the portal cooldown, which is what the mark exists to cover, and
     * expires on its own whether or not anyone ever asks about it again.</p>
     */
    private static final long LANDING_GRACE = 600L;
    private static final String DIM_KEY = "Dimension";
    private static final String X = "X", Y = "Y", Z = "Z";

    /** Sends a player through, remembering exactly where they stood. */
    public static void send(ServerPlayer player, MinecraftServer server) {
        ServerLevel spirit = server.getLevel(SpiritRealm.LEVEL);
        if (spirit == null) {
            WispWillow.LOGGER.error("Spirit Realm dimension is missing; cannot send {}", player.getName().getString());
            return;
        }
        if (player.level().dimension().equals(SpiritRealm.LEVEL)) return;

        rememberReturn(player);
        Vec3 arrival = arrivalIn(spirit, player.blockPosition());
        // Gathered before the player moves: afterwards they are standing in another world and the
        // search would come up empty.
        List<TamableAnimal> escort = petsFollowing(player);
        // Set before the move, not after: changeDimension places the entity itself, and that
        // placement runs entityInside synchronously — so a cooldown applied on the next line is
        // already too late to stop the gate at the far end from taking them straight back.
        player.setPortalCooldown();
        player.changeDimension(new DimensionTransition(spirit, arrival, Vec3.ZERO,
                player.getYRot(), player.getXRot(), ARRIVE));
        walkThrough(escort, spirit, arrival);
        openReturnGate(spirit, arrival);
        player.displayClientMessage(Component.translatable("vigil.wispwillow.open_way.entered"), true);
    }

    /** Puts everyone in the realm back where they came from. Called when the vigil ends. */
    public static int returnEveryone(MinecraftServer server, boolean graceful) {
        ServerLevel spirit = server.getLevel(SpiritRealm.LEVEL);
        if (spirit == null) return 0;

        // Copied first: sending a player out of the level mutates the list being iterated.
        List<ServerPlayer> travellers = new ArrayList<>(spirit.players());
        for (ServerPlayer player : travellers) {
            returnHome(player, server, graceful);
        }
        return travellers.size();
    }

    /** One traveller home: to where they left from, or to their spawn if that is gone. */
    public static void returnHome(ServerPlayer player, MinecraftServer server, boolean graceful) {
        CompoundTag data = player.getPersistentData();
        ServerLevel destination = null;
        Vec3 target = null;

        if (data.contains(RETURN_TAG)) {
            CompoundTag saved = data.getCompound(RETURN_TAG);
            ResourceLocation dim = ResourceLocation.tryParse(saved.getString(DIM_KEY));
            if (dim != null) {
                destination = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
            }
            if (destination != null) {
                target = new Vec3(saved.getDouble(X), saved.getDouble(Y), saved.getDouble(Z));
            }
            data.remove(RETURN_TAG);
        }

        if (destination == null) {
            destination = server.overworld();
            target = null;
        }
        if (target == null) {
            BlockPos spawn = destination.getSharedSpawnPos();
            target = Vec3.atBottomCenterOf(spawn.atY(destination.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ())));
        }

        target = clearOfAnyGate(destination, target);

        List<TamableAnimal> escort = petsFollowing(player);
        // Vanilla's own guard against a portal grabbing you the instant you land on it, and it has
        // to be armed before the move for the same reason as above.
        player.setPortalCooldown();
        player.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO,
                player.getYRot(), player.getXRot(), ARRIVE));
        walkThrough(escort, destination, target);
        markLanding(player, destination, target);
        player.level().playSound(null, BlockPos.containing(target), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.7F, graceful ? 1.0F : 0.6F);
        player.displayClientMessage(Component.translatable(graceful
                ? "vigil.wispwillow.open_way.returned"
                : "vigil.wispwillow.open_way.cast_out"), false);
    }

    private static void rememberReturn(ServerPlayer player) {
        CompoundTag saved = new CompoundTag();
        saved.putString(DIM_KEY, player.level().dimension().location().toString());
        saved.putDouble(X, player.getX());
        saved.putDouble(Y, player.getY());
        saved.putDouble(Z, player.getZ());
        player.getPersistentData().put(RETURN_TAG, saved);
    }

    /** True once a player has a way home recorded — i.e. the realm owes them an exit. */
    public static boolean isTravelling(ServerPlayer player) {
        return player.getPersistentData().contains(RETURN_TAG);
    }

    /**
     * Drop a recorded way home.
     *
     * <p>A return point only means anything while its owner is actually in the realm. One found on a
     * player standing in the overworld belongs to a trip that already ended — a session that closed
     * badly, or a vigil that stopped while they were logged out. Left in place it is worse than
     * useless: the gate reads it as "this one already has a way out" and refuses to take them, so
     * the rite appears to do nothing at all, for ever.</p>
     */
    public static void forgetReturn(ServerPlayer player) {
        player.getPersistentData().remove(RETURN_TAG);
    }

    /**
     * Where a returning traveller was set down, so the gate does not immediately take them back.
     *
     * <p>You are returned to the spot you left from, and while the vigil is still burning that spot
     * is right beside its altar — inside the gate's reach, if not inside the sheet itself. The
     * portal cooldown covers the first fifteen seconds and then lapses, and anyone still standing
     * there is taken straight back through. Someone who came home early to drop off what they were
     * carrying would be pulled back into the realm while doing it.</p>
     *
     * <p>Clearing the way home is not enough to prevent that, because an absent way home is exactly
     * what the outbound gate is looking for. What is needed is a memory of having <em>just
     * arrived</em>, which is what this is: the gate refuses anyone still standing where it put them,
     * and forgets them the moment they walk away under their own power.</p>
     */
    private static void markLanding(ServerPlayer player, ServerLevel level, Vec3 at) {
        CompoundTag mark = new CompoundTag();
        mark.putString(DIM_KEY, level.dimension().location().toString());
        mark.putDouble(X, at.x);
        mark.putDouble(Y, at.y);
        mark.putDouble(Z, at.z);
        mark.putLong(LANDED_AT, level.getGameTime());
        player.getPersistentData().put(LANDING_TAG, mark);
    }

    /**
     * True while a player has not yet moved away from where a return put them down.
     *
     * <p>Self-clearing: the check itself drops the mark once they are clear of it, or once they are
     * in a different world than the one they landed in, so nothing else has to remember to tidy up
     * and a mark left by a crash cannot strand the gate shut.</p>
     */
    public static boolean standingWhereTheyLanded(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(LANDING_TAG)) return false;

        CompoundTag mark = data.getCompound(LANDING_TAG);

        // Age first, and unconditionally. Every other exit from this method depends on someone
        // being here to ask the question, and the whole failure was that nobody ever was.
        long age = player.level().getGameTime() - mark.getLong(LANDED_AT);
        if (age < 0L || age > LANDING_GRACE) {
            data.remove(LANDING_TAG);
            return false;
        }

        if (!player.level().dimension().location().toString().equals(mark.getString(DIM_KEY))) {
            data.remove(LANDING_TAG);
            return false;
        }

        double dx = player.getX() - mark.getDouble(X);
        double dy = player.getY() - mark.getDouble(Y);
        double dz = player.getZ() - mark.getDouble(Z);
        if (dx * dx + dy * dy + dz * dz > LANDING_CLEAR * LANDING_CLEAR) {
            data.remove(LANDING_TAG);
            return false;
        }
        return true;
    }

    /**
     * Somewhere to land in the realm, which is mostly void.
     *
     * <p>The realm generates on the End's noise, so the column a traveller left from is more often
     * than not empty air all the way down. Taking the heightmap's word for it — which is what the
     * first version did, back when this dimension used overworld terrain — drops people straight
     * out of the world.</p>
     *
     * <p>So: try the column overhead, then sweep outward a few chunks for an island, and failing
     * that build something to stand on, exactly as vanilla does when it puts you into the End with
     * nothing underneath.</p>
     */
    private static Vec3 arrivalIn(ServerLevel spirit, BlockPos from) {
        Vec3 direct = standingSpot(spirit, from.getX(), from.getZ());
        if (direct != null) return direct;

        for (int ring = 1; ring <= ARRIVAL_SEARCH_CHUNKS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Perimeter of the ring only; the inside was covered by a smaller one.
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    Vec3 spot = standingSpot(spirit, from.getX() + dx * 16, from.getZ() + dz * 16);
                    if (spot != null) return spot;
                }
            }
        }
        return buildLanding(spirit, from.getX(), from.getZ());
    }

    /**
     * The top of a real island at this column, or null if the column is void.
     *
     * <p>The chunk is generated first: without it the height query answers for a chunk that does
     * not exist yet and everyone arrives inside the ground.</p>
     */
    @Nullable
    private static Vec3 standingSpot(ServerLevel spirit, int x, int z) {
        spirit.getChunk(x >> 4, z >> 4);
        int y = spirit.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= spirit.getMinBuildHeight() + 1) return null;
        if (spirit.getBlockState(new BlockPos(x, y - 1, z)).isAir()) return null;
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    /**
     * Nudge a landing spot out of a gate, if it is standing in one.
     *
     * <p>You are returned to the exact spot you left from — which, while the vigil is still burning,
     * is <em>inside</em> the sheet hanging over its altar. Landing there was read as walking into the
     * gate and sent the traveller straight back, over and over. Anyone who wants to come home early
     * would have bounced between the worlds until the rite ran out.</p>
     */
    private static Vec3 clearOfAnyGate(ServerLevel level, Vec3 target) {
        BlockPos at = BlockPos.containing(target);
        // Generated first: an unloaded chunk answers every block query with air, so the gate
        // standing at the landing spot would go unseen and the nudge would never happen.
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        if (!isGate(level, at) && !isGate(level, at.above())) return target;

        // Wider than it was, and it now checks for something to stand on. The old version only
        // asked for two air blocks, which a spot hanging in mid-air satisfies perfectly well — so
        // a "safe" landing could set the traveller down off the side of whatever they came home to.
        for (int step = 2; step <= 6; step++) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos side = at.relative(dir, step);
                if (gateNear(level, side)) continue;
                if (!level.getBlockState(side).isAir() || !level.getBlockState(side.above()).isAir()) continue;
                if (!level.getBlockState(side.below()).isSolid()) continue;
                return new Vec3(side.getX() + 0.5, side.getY(), side.getZ() + 0.5);
            }
        }

        // Nothing clear on the flat: take the surface a few blocks off instead. Returning the
        // original target here — which is what used to happen — sets the traveller back down inside
        // the live gate they just came out of, and that bounce is the whole reason this exists.
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos side = at.relative(dir, 3);
            level.getChunk(side.getX() >> 4, side.getZ() >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, side.getX(), side.getZ());
            BlockPos surface = new BlockPos(side.getX(), y, side.getZ());
            if (gateNear(level, surface)) continue;
            return new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        }
        return target;
    }

    /** A gate at this block, the one above it, or the one below — the sheet stands two tall. */
    private static boolean gateNear(ServerLevel level, BlockPos pos) {
        return isGate(level, pos) || isGate(level, pos.above()) || isGate(level, pos.below());
    }

    private static boolean isGate(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockRegistry.SPIRIT_PORTAL.get());
    }

    /**
     * A way back, standing where you came in.
     *
     * <p>Until now the only exit was the vigil ending, which meant a traveller who finished early
     * had to wait out the half hour. The gate is raised two blocks clear of the arrival spot rather
     * than on it, so stepping through does not immediately put you back where you started, and it
     * is left standing: the way home should still be there when you come back to it.</p>
     */
    private static void openReturnGate(ServerLevel spirit, Vec3 arrival) {
        int x = Mth.floor(arrival.x) + RETURN_GATE_OFFSET;
        int z = Mth.floor(arrival.z);
        spirit.getChunk(x >> 4, z >> 4);

        int y = spirit.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= spirit.getMinBuildHeight() + 1) return;   // nothing to stand it on

        BlockPos base = new BlockPos(x, y, z);
        for (int dy = 0; dy < RETURN_GATE_HEIGHT; dy++) {
            BlockPos at = base.above(dy);
            if (!spirit.getBlockState(at).canBeReplaced()) continue;
            spirit.setBlock(at, BlockRegistry.SPIRIT_PORTAL.get().defaultBlockState(), 3);
        }
    }

    /** A small end stone pad with headroom, for when there is no island within reach. */
    private static Vec3 buildLanding(ServerLevel spirit, int x, int z) {
        BlockPos centre = new BlockPos(x, LANDING_Y, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                spirit.setBlockAndUpdate(centre.offset(dx, 0, dz),
                        BlockRegistry.SPIRITSTONE.get().defaultBlockState());
                for (int dy = 1; dy <= 3; dy++) {
                    spirit.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        return new Vec3(x + 0.5, LANDING_Y + 1, z + 0.5);
    }

    /**
     * A player's tamed animals, standing close enough to be coming with them.
     *
     * <p>Ones told to sit are left behind on purpose: "stay" should mean stay, including when the
     * world changes around them. Familiars are not tamable animals and never travel — they belong
     * to an altar, and the altar is not going anywhere.</p>
     */
    private static List<TamableAnimal> petsFollowing(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(TamableAnimal.class,
                player.getBoundingBox().inflate(ESCORT_RADIUS),
                pet -> pet.isAlive() && pet.isTame() && !pet.isOrderedToSit()
                        && player.getUUID().equals(pet.getOwnerUUID()));
    }

    /** Puts the escort through after its owner, onto the same spot. */
    private static void walkThrough(List<TamableAnimal> escort, ServerLevel destination, Vec3 target) {
        for (TamableAnimal pet : escort) {
            if (!pet.isAlive()) continue;
            pet.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO,
                    pet.getYRot(), pet.getXRot(), DimensionTransition.PLACE_PORTAL_TICKET));
        }
    }

    @Nullable
    public static ServerLevel realm(Level any) {
        MinecraftServer server = any.getServer();
        return server == null ? null : server.getLevel(SpiritRealm.LEVEL);
    }
}
