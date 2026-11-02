package com.reynn.wispwillow.awakening;

import com.mojang.serialization.Codec;
import com.reynn.wispwillow.WispWillow;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * The one lasting trick an awakened piece can be taught by an imbuing rite.
 *
 * <p>A piece holds one imbuement at a time; imbuing it again replaces the old one. Every
 * number an imbuement uses lives here as a function of the piece's {@link Resonance} level,
 * so the effect code, the tooltip and the grimoire all quote the same figures.</p>
 *
 * <p>Saved by name, so reordering this enum never changes what a stored item is.</p>
 */
public enum Imbuement implements StringRepresentable {
    SUNFIRE("sunfire", Target.MELEE, 0xFF8A3D, "Sunfire",
            "Strikes set foes alight for %1$s seconds and hit the undead %2$s%% harder"),
    SIPHON("siphon", Target.MELEE, 0xE0506E, "Siphon",
            "Heals you for %1$s%% of the damage your strikes deal"),
    REAPING("reaping", Target.MELEE, 0xB8BECF, "Reaping",
            "Foes you slay give up %1$s%% more experience"),
    KILNHEART("kilnheart", Target.TOOL, 0xFF7A2F, "Kilnheart",
            "Ores and raw metal come out of the ground already smelted"),
    VEINSENSE("veinsense", Target.TOOL, 0xC89A5A, "Veinsense",
            "Breaking an ore breaks up to %1$s more of its vein; sneak to break just one"),
    TETHER("tether", Target.TOOL, 0xD070F0, "Tether",
            "What you break flies straight into your pack, experience and all"),
    CLARITY("clarity", Target.HEAD, 0x7FE8FF, "Clarity",
            "Sight in the dark, no blindness or darkness, and breath that lasts %1$s× as long"),
    BULWARK("bulwark", Target.CHEST, 0xF0D070, "Bulwark",
            "Up to %1$s hearts of golden shielding, refilling after %2$s seconds unharmed"),
    STRIDE("stride", Target.LEGS, 0x8BD85A, "Stride",
            "%1$s%% faster on foot, and whole blocks are a step rather than a jump"),
    SKYSTEP("skystep", Target.FEET, 0xDDE6FF, "Skystep",
            "Falling cannot hurt you, and you jump with %1$s%% more spring");

    public static final Codec<Imbuement> CODEC = StringRepresentable.fromEnum(Imbuement::values);
    public static final StreamCodec<ByteBuf, Imbuement> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Imbuement::ordinal);

    /** Which pieces an imbuement can be laid on. */
    public enum Target {
        MELEE("a sword or axe"),
        TOOL("a pickaxe, shovel, axe or hoe"),
        HEAD("a helm"),
        CHEST("a cuirass"),
        LEGS("tassets"),
        FEET("greaves");

        private final String english;

        Target(String english) { this.english = english; }

        /** For the grimoire, which is written in English at datagen. */
        public String english() { return english; }

        public boolean accepts(GearKind kind) {
            return switch (this) {
                case MELEE -> kind.melee();
                case TOOL -> kind.digs();
                case HEAD -> kind == GearKind.HELMET;
                case CHEST -> kind == GearKind.CHESTPLATE;
                case LEGS -> kind == GearKind.LEGGINGS;
                case FEET -> kind == GearKind.BOOTS;
            };
        }
    }

    // ------------------------------------------------------------------
    // The numbers, by resonance level (1..5)
    // ------------------------------------------------------------------

    public static float sunfireSeconds(int level) { return 2 + level; }
    public static float sunfireUndeadBonus(int level) { return 0.10f + 0.05f * level; }
    public static float siphonFraction(int level) { return 0.05f + 0.02f * level; }
    public static float reapingBonus(int level) { return 0.20f + 0.20f * level; }
    public static int veinsenseExtra(int level) { return 4 + 4 * level; }
    public static int clarityOxygen(int level) { return level; }
    public static int bulwarkHearts(int level) { return 2 + level; }
    /** How long a Bulwark wearer has to go unharmed before the shielding starts to refill. */
    public static final int BULWARK_REST_TICKS = 160;
    public static float strideSpeed(int level) { return 0.04f + 0.02f * level; }
    /** Added to the 0.6 a player already has, so a full block is a step. */
    public static final float STRIDE_STEP = 0.6f;
    public static float skystepJump(int level) { return 0.03f * level; }
    private static final float BASE_JUMP = 0.42f;

    // ------------------------------------------------------------------

    private final String id;
    private final Target target;
    private final int color;
    private final String englishName;
    private final String englishDescription;

    Imbuement(String id, Target target, int color, String englishName, String englishDescription) {
        this.id = id;
        this.target = target;
        this.color = color;
        this.englishName = englishName;
        this.englishDescription = englishDescription;
    }

    @Override public String getSerializedName() { return id; }

    public Target target() { return target; }

    public int color() { return color; }

    public boolean accepts(ItemStack stack) { return target.accepts(GearKind.of(stack)); }

    public String langKey() { return "imbuement." + WispWillow.MODID + "." + id; }

    public MutableComponent displayName() {
        return Component.translatable(langKey()).withStyle(style -> style.withColor(color));
    }

    public MutableComponent describe(int level) {
        return Component.translatable(langKey() + ".desc", args(level));
    }

    /** The English the lang provider writes, and the grimoire's source text. */
    public String englishName() { return englishName; }

    public String englishDescriptionTemplate() { return englishDescription; }

    public String englishDescription(int level) {
        return String.format(Locale.ROOT, englishDescription, args(level));
    }

    /** The values a description quotes at a given level, already formatted for reading. */
    public Object[] args(int level) {
        return switch (this) {
            case SUNFIRE -> new Object[] {whole(sunfireSeconds(level)), pct(sunfireUndeadBonus(level))};
            case SIPHON -> new Object[] {pct(siphonFraction(level))};
            case REAPING -> new Object[] {pct(reapingBonus(level))};
            case VEINSENSE -> new Object[] {String.valueOf(veinsenseExtra(level))};
            case CLARITY -> new Object[] {String.valueOf(clarityOxygen(level) + 1)};
            case BULWARK -> new Object[] {String.valueOf(bulwarkHearts(level)), String.valueOf(BULWARK_REST_TICKS / 20)};
            case STRIDE -> new Object[] {pct(strideSpeed(level))};
            case SKYSTEP -> new Object[] {pct(skystepJump(level) / BASE_JUMP)};
            case KILNHEART, TETHER -> new Object[0];
        };
    }

    private static String pct(float fraction) { return String.valueOf(Math.round(fraction * 100)); }

    private static String whole(float value) { return String.valueOf(Math.round(value)); }
}
