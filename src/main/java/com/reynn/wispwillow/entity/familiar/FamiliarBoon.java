package com.reynn.wispwillow.entity.familiar;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * What a familiar does for the altar it is bound to.
 *
 * <p>This is the template axis: a new familiar species is a new entity class plus one of
 * these, and it slots into the altar's boon maths with no further wiring.</p>
 */
public enum FamiliarBoon implements StringRepresentable {

    /** Halves what every rite costs, in Fluxia and in reagents alike. The headline boon. */
    FRUGAL("frugal", 0.50f, 0f, 0f, 0x7FE8FF),

    /** Meaningful chance of a doubled result, on top of any blood runes. */
    BOUNTIFUL("bountiful", 0f, 0.25f, 0f, 0xC23B3B),

    /** Deepens the altar's reserve while it is nearby. */
    CAPACIOUS("capacious", 0f, 0f, 20000f, 0x4FA8E8),

    /** A little of everything, for a generalist companion. */
    BALANCED("balanced", 0.15f, 0.10f, 5000f, 0xB28BFF);

    private final String name;
    private final float efficiency;
    private final float yield;
    private final float capacity;
    private final int color;

    FamiliarBoon(String name, float efficiency, float yield, float capacity, int color) {
        this.name = name;
        this.efficiency = efficiency;
        this.yield = yield;
        this.capacity = capacity;
        this.color = color;
    }

    @Override public String getSerializedName() { return name; }

    /** Fraction removed from a rite's price, in Fluxia and in reagents alike. */
    public float efficiency() { return efficiency; }

    /** Added chance of a bonus result. */
    public float yieldChance() { return yield; }

    /** Extra Fluxia reserve granted to the bound altar. */
    public int capacity() { return (int) capacity; }

    public int color() { return color; }

    public Component describe() {
        return Component.translatable("familiar.wispwillow.boon." + name)
                .withStyle(style -> style.withColor(color));
    }

    public Component summary() {
        StringBuilder sb = new StringBuilder();
        if (efficiency > 0f) sb.append(Math.round(efficiency * 100)).append("% cheaper");
        if (yield > 0f) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Math.round(yield * 100)).append("% bonus");
        }
        if (capacity > 0f) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("+").append((int) capacity).append(" reserve");
        }
        return Component.literal(sb.toString()).withStyle(ChatFormatting.GREEN);
    }
}
