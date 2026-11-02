package com.reynn.wispwillow.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.world.SpiritTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * A rite the altar <em>keeps</em> rather than one it completes.
 *
 * <p>Every other rite in this book is a transaction: you lay the circle, drop the reagents,
 * and take the result. A vigil is a standing arrangement — it pays out continuously, charges
 * continuously, and holds the altar for as long as it lasts. The altar will not perform any
 * other rite while one is burning, which is the cost that makes them interesting: an altar
 * keeping the weather is an altar you are not using for anything else.</p>
 *
 * <p>Three things end one: it runs its course, the circle beneath it is broken, or the
 * Fluxia runs out. Only the first is graceful.</p>
 */
public record Vigil(Effect effect, int durationTicks, int upkeepPerSecond) {

    public static final MapCodec<Vigil> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Effect.CODEC.fieldOf("effect").forGetter(Vigil::effect),
            Codec.INT.fieldOf("duration_ticks").forGetter(Vigil::durationTicks),
            Codec.INT.fieldOf("upkeep_per_second").forGetter(Vigil::upkeepPerSecond)
    ).apply(i, Vigil::new));

    public static final Codec<Vigil> CODEC = MAP_CODEC.codec();

    /** What a vigil actually does to the world while it burns. */
    public enum Effect implements StringRepresentable {

        /**
         * Holds the sky open.
         *
         * <p>Re-asserted every second rather than set once: a player sleeping, another
         * altar, or a command can all change the weather underneath it, and a vigil that
         * quietly lost its argument with the world would look broken.</p>
         */
        CLEAR_SKIES("clear_skies", 0xFF7FD8FF, 24) {
            @Override public void apply(ServerLevel level, BlockPos altar, int age) {
                if (age % 20 != 0) return;
                if (level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(12_000, 0, false, false);
                }
            }
            @Override public void ambient(ServerLevel level, BlockPos altar, int age) {
                if (age % 6 != 0) return;
                level.sendParticles(ParticleTypes.END_ROD,
                        altar.getX() + 0.5, altar.getY() + 2.2, altar.getZ() + 0.5,
                        1, 1.4, 0.6, 1.4, 0.01);
            }
        },

        /** The other way round: keeps a storm overhead. */
        GATHERING_STORM("gathering_storm", 0xFF6B5BA8, 32) {
            @Override public void apply(ServerLevel level, BlockPos altar, int age) {
                if (age % 20 != 0) return;
                if (!level.isThundering()) {
                    level.setWeatherParameters(0, 12_000, true, true);
                }
            }
            @Override public void ambient(ServerLevel level, BlockPos altar, int age) {
                if (age % 4 != 0) return;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        altar.getX() + 0.5, altar.getY() + 2.4, altar.getZ() + 0.5,
                        2, 1.6, 0.8, 1.6, 0.05);
            }
        },

        /**
         * Random-ticks whatever will grow, inside the circle.
         *
         * <p>Deliberately uses the block's own {@code randomTick} rather than forcing an age
         * property: that way it accelerates anything the game considers growable — crops,
         * saplings, cane, another mod's plant — without needing to know what any of it is.</p>
         */
        VERDANT_TIDE("verdant_tide", 0xFF7FE87F, 16) {
            @Override public void apply(ServerLevel level, BlockPos altar, int age) {
                if (age % 5 != 0) return;
                for (int i = 0; i < 6; i++) {
                    BlockPos at = altar.offset(
                            level.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                            level.random.nextInt(5) - 2,
                            level.random.nextInt(RADIUS * 2 + 1) - RADIUS);
                    BlockState state = level.getBlockState(at);
                    if (state.isRandomlyTicking()) {
                        state.randomTick(level, at, level.random);
                    }
                }
            }
            @Override public void ambient(ServerLevel level, BlockPos altar, int age) {
                if (age % 3 != 0) return;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5,
                        2, RADIUS * 0.5, 0.8, RADIUS * 0.5, 0.0);
            }
        },

        /**
         * Pushes hostiles out of the circle.
         *
         * <p>Repulsion rather than a spawn ban: a ban needs global state — every active
         * warding altar in the world, consulted from a spawn event — and it is invisible when
         * it works. Something being shoved backwards out of the light is legible.</p>
         */
        WARDING_LIGHT("warding_light", 0xFFE8C86B, 20) {
            @Override public void apply(ServerLevel level, BlockPos altar, int age) {
                // A pulse rather than a continuous force. Adding to the existing velocity
                // every tick compounds: in testing a zombie stood in the ring for four
                // seconds and left the loaded chunks entirely. Setting a fixed outward
                // speed a few times a second is bounded, and reads as being shoved
                // repeatedly rather than fired out of a cannon.
                if (age % 5 != 0) return;

                Vec3 centre = Vec3.atCenterOf(altar);
                AABB box = new AABB(centre, centre).inflate(RADIUS, 4, RADIUS);
                for (Entity entity : level.getEntities((Entity) null, box, e -> e instanceof Enemy)) {
                    // Flattened before normalising: a mob standing on the altar's own step
                    // is a metre above the centre, and a 3D vector there would spend most
                    // of the shove pushing it up rather than out.
                    Vec3 away = entity.position().subtract(centre);
                    away = new Vec3(away.x, 0.0, away.z);
                    if (away.lengthSqr() < 1.0e-3) away = new Vec3(1, 0, 0);
                    entity.setDeltaMovement(away.normalize().scale(0.35).add(0, 0.18, 0));
                    // Without this the client never sees the shove: velocity changes on the
                    // server are not synced to the entity's viewers unless it is marked.
                    entity.hurtMarked = true;
                }
            }
            @Override public void ambient(ServerLevel level, BlockPos altar, int age) {
                if (age % 5 != 0) return;
                double a = (age * 0.08) % (Math.PI * 2);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        altar.getX() + 0.5 + Math.cos(a) * RADIUS,
                        altar.getY() + 0.6,
                        altar.getZ() + 0.5 + Math.sin(a) * RADIUS,
                        1, 0.05, 0.2, 0.05, 0.0);
            }
        },

        /**
         * Holds a way open into the {@link com.reynn.wispwillow.worldgen.spirit.SpiritRealm}.
         *
         * <p>The only vigil that does something to <em>you</em> rather than to the world. Stand on
         * the altar while it burns and it takes you through; when it stops — spent, broken or
         * starved — everyone it took is put back where they were standing when they left.</p>
         *
         * <p>That exit is unconditional on purpose. A realm you can only leave by keeping an altar
         * fed is a trap, and the half hour is the point: it is an excursion, not a home.</p>
         */
        OPEN_WAY("open_way", 0xFFB58BFF, 30) {
            @Override public void apply(ServerLevel level, BlockPos altar, int age) {
                // Re-asserted every second rather than hung once. The sheet is a block like any
                // other and a player can mine, pour or explode it away; a way out that could be
                // destroyed from the outside would strand whoever is already through it.
                if (age % 20 != 0) return;
                for (int dy = 1; dy <= GATE_HEIGHT; dy++) {
                    BlockPos at = altar.above(dy);
                    if (level.getBlockState(at).is(BlockRegistry.SPIRIT_PORTAL.get())) continue;
                    if (!level.getBlockState(at).canBeReplaced()) continue;
                    level.setBlock(at, BlockRegistry.SPIRIT_PORTAL.get().defaultBlockState(), 3);
                }

                // The sheet is how you *see* the way open; it is not the only thing that opens it.
                // If the gate cannot be hung — something solid sitting above the altar, a block
                // broken and not yet re-asserted, a sheet the player is standing in before it was
                // placed — the rite still takes whoever stands in the doorway. A vigil that looked
                // lit and quietly did nothing would be indistinguishable from a broken one.
                AABB doorway = new AABB(
                        altar.getX(), altar.getY() + 1.0, altar.getZ(),
                        altar.getX() + 1.0, altar.getY() + 1.0 + GATE_HEIGHT, altar.getZ() + 1.0)
                        .inflate(0.2);
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, doorway)) {
                    if (player.isOnPortalCooldown()) continue;
                    // Someone the realm just put down here is not asking to go back in. The portal
                    // cooldown only covers the first fifteen seconds; without this the rite pulls
                    // them straight through again the moment it lapses.
                    if (SpiritTravel.standingWhereTheyLanded(player)) continue;
                    // Any way home recorded on someone still standing in the overworld is stale.
                    if (SpiritTravel.isTravelling(player)) SpiritTravel.forgetReturn(player);
                    SpiritTravel.send(player, level.getServer());
                }
            }
            @Override public void ambient(ServerLevel level, BlockPos altar, int age) {
                if (age % 4 != 0) return;
                double a = (age * 0.05) % (Math.PI * 2);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        altar.getX() + 0.5 + Math.cos(a) * 1.2,
                        altar.getY() + 1.1,
                        altar.getZ() + 0.5 + Math.sin(a) * 1.2,
                        1, 0.02, 0.3, 0.02, 0.01);
            }
            @Override public void onEnd(ServerLevel level, BlockPos altar, boolean graceful) {
                // The way shuts first, so nobody walks into a gate that is already spent.
                for (int dy = 1; dy <= GATE_HEIGHT; dy++) {
                    BlockPos at = altar.above(dy);
                    if (level.getBlockState(at).is(BlockRegistry.SPIRIT_PORTAL.get())) {
                        level.removeBlock(at, false);
                    }
                }
                int returned = SpiritTravel.returnEveryone(level.getServer(), graceful);
                if (returned > 0) {
                    level.playSound(null, altar, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.6F, 1.4F);
                }
            }
        };

        /** How far a vigil reaches, for the effects that care. */
        public static final int RADIUS = 8;

        /** How tall the Open Way's sheet stands above its altar: enough to walk into. */
        public static final int GATE_HEIGHT = 2;

        public static final Codec<Effect> CODEC = StringRepresentable.fromEnum(Effect::values);

        private final String name;
        private final int colour;
        private final int upkeep;

        Effect(String name, int colour, int upkeep) {
            this.name = name;
            this.colour = colour;
            this.upkeep = upkeep;
        }

        @Override public String getSerializedName() { return name; }

        /** ARGB, used by the beam, the ring and the goggle readout alike. */
        public int colour() { return colour; }

        /**
         * The rate this effect is meant to burn at, in Fluxia per second, unless a rite
         * overrides it.
         *
         * <p>Per second rather than per tick because of what the altar can actually be
         * fed: a pool pushes at most 800 ƒ/s into any one machine and holds 40,000, so a
         * per-tick rate in these numbers would cost half a million Fluxia a vigil and no
         * reachable supply would keep one lit. At this rate a full altar reserve carries a
         * vigil most of the way and a linked pool carries it the rest.</p>
         */
        public int defaultUpkeep() { return upkeep; }

        public String translationKey() {
            return "vigil.wispwillow." + name;
        }

        /** Server-side work. Called every tick the vigil is burning. */
        public abstract void apply(ServerLevel level, BlockPos altar, int age);

        /** Server-spawned decoration. Separate from {@link #apply} so it can be throttled. */
        public abstract void ambient(ServerLevel level, BlockPos altar, int age);

        /**
         * Called once when the burn stops, however it stopped.
         *
         * <p>Most vigils simply cease — the weather they were holding drifts back on its own, and
         * they have nothing to undo. One does: a vigil that took people somewhere has to bring
         * them back, whether it ran its course or the circle was broken under it.</p>
         *
         * @param graceful true only when the vigil ran to its natural end
         */
        public void onEnd(ServerLevel level, BlockPos altar, boolean graceful) {}

        @Nullable
        public static Effect byName(String raw) {
            if (raw == null || raw.isEmpty()) return null;
            for (Effect e : values()) {
                if (e.name.equals(raw)) return e;
            }
            // Tolerate an enum-cased name, since that is what a hand-edited NBT tends to hold.
            try {
                return valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException notAnEffect) {
                return null;
            }
        }
    }

    /** Total Fluxia a full burn will cost, for the book and the goggles. */
    public int totalCost() {
        return (durationTicks / UPKEEP_INTERVAL) * upkeepPerSecond;
    }

    /** How often upkeep is actually taken, in ticks. */
    public static final int UPKEEP_INTERVAL = 20;

    /** Whole minutes and seconds a burn lasts, for anything that has to say it out loud. */
    public static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // ------------------------------------------------------------------
    // Persistence
    //
    // A vigil is written into the altar's own NBT rather than looked up from its rite on
    // load: a datapack that removes or retunes the rite mid-burn would otherwise strand or
    // silently re-price something the player already paid for.
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("effect", effect.getSerializedName());
        tag.putInt("duration", durationTicks);
        tag.putInt("upkeep", upkeepPerSecond);
        return tag;
    }

    @Nullable
    public static Vigil load(CompoundTag tag) {
        Effect effect = Effect.byName(tag.getString("effect"));
        if (effect == null) return null;
        int duration = tag.getInt("duration");
        if (duration <= 0) return null;
        int upkeep = tag.contains("upkeep") ? tag.getInt("upkeep") : effect.defaultUpkeep();
        return new Vigil(effect, duration, Math.max(0, upkeep));
    }
}
