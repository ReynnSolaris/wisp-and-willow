package com.reynn.wispwillow.awakening;

import com.reynn.wispwillow.items.Awakening;
import com.reynn.wispwillow.registry.ComponentRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * How deeply an awakened piece has grown into its work.
 *
 * <p>Stored as a running count of resonance on the stack. The level is derived from the count
 * rather than stored beside it, so the two can never disagree, and everything an awakened
 * piece does — its waking traits and its imbuement alike — scales with that level.</p>
 *
 * <p>The count stops at the last threshold. A fully resonant piece has nothing left to
 * record, and not rewriting its component on every swing keeps it from resyncing for nothing.</p>
 */
public final class Resonance {
    private Resonance() {}

    public static final int MAX_LEVEL = 5;

    /** Resonance needed to reach each level; level I is where every awakened piece starts. */
    private static final int[] THRESHOLDS = {0, 60, 300, 1_200, 4_000};

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    public static int points(ItemStack stack) {
        return stack.getOrDefault(ComponentRegistry.RESONANCE.get(), 0);
    }

    public static int level(ItemStack stack) {
        return levelFor(points(stack));
    }

    public static int levelFor(int points) {
        int level = 1;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (points >= THRESHOLDS[i]) level = i + 1;
        }
        return level;
    }

    /** Resonance a piece needs to have reached the given level. */
    public static int threshold(int level) {
        return THRESHOLDS[Mth.clamp(level, 1, MAX_LEVEL) - 1];
    }

    public static String roman(int level) {
        return ROMAN[Mth.clamp(level, 1, MAX_LEVEL) - 1];
    }

    /**
     * Adds resonance to an awakened piece.
     *
     * @return the level it has just reached if this raised it, otherwise 0
     */
    public static int add(ItemStack stack, int amount) {
        if (amount <= 0 || !Awakening.isAwakened(stack)) return 0;
        int before = points(stack);
        int after = (int) Math.min(threshold(MAX_LEVEL), (long) before + amount);
        if (after == before) return 0;
        stack.set(ComponentRegistry.RESONANCE.get(), after);
        int from = levelFor(before), to = levelFor(after);
        return to > from ? to : 0;
    }
}
