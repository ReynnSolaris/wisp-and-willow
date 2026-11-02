package com.reynn.wispwillow.items;

import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The embuement the Awakening Empyrite rite leaves on a stack.
 *
 * <p>Awakening is stored as the {@link ComponentRegistry#AWAKENED} marker component and
 * nothing else. That one fact drives everything else in the feature: the rite sets it,
 * the tooltip reads it, and the renderer swaps the enchantment glint to gold when it is
 * there. Because it is a component and not an enchantment it costs the item no
 * enchantment slot, cannot be ground off, and is not something an anvil can trade away
 * — the metal is awake, and it stays awake.</p>
 *
 * <p>Both sides use this class. Keep it free of client-only types.</p>
 */
public final class Awakening {
    private Awakening() {}

    /** Whether this stack has been through the rite. */
    public static boolean isAwakened(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ComponentRegistry.AWAKENED.get());
    }

    /** Whether the rite would do anything to this stack. */
    public static boolean canAwaken(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(TagRegistry.Items.EMPYRITE)
                && !isAwakened(stack);
    }

    /**
     * Marks a stack awakened in place.
     *
     * <p>Nothing else about the stack is touched, so enchantments, damage, a custom
     * name and any trim all come back out of the circle exactly as they went in.</p>
     *
     * @return true if this call is what changed it, false if it was already awake or
     *         was never empyrite to begin with
     */
    public static boolean awaken(ItemStack stack) {
        if (!canAwaken(stack)) return false;
        stack.set(ComponentRegistry.AWAKENED.get(), Unit.INSTANCE);
        return true;
    }

    /** The imbuement this stack carries, or null for none. */
    @Nullable
    public static Imbuement imbuementOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ComponentRegistry.IMBUEMENT.get());
    }

    /**
     * Whether an imbuing rite for {@code imbuement} would do anything to this stack: it has to
     * be awake, be the kind of piece the imbuement suits, and not already carry it. A piece
     * carrying a different imbuement qualifies — imbuing it replaces the old one.
     */
    public static boolean canImbue(ItemStack stack, Imbuement imbuement) {
        return isAwakened(stack) && imbuement.accepts(stack) && imbuementOf(stack) != imbuement;
    }

    /**
     * Imbues a stack in place, replacing any imbuement it had. Resonance is left alone: what a
     * piece has grown into is the metal's, not the trick's.
     *
     * @return true if this call is what changed it
     */
    public static boolean imbue(ItemStack stack, Imbuement imbuement) {
        if (!canImbue(stack, imbuement)) return false;
        stack.set(ComponentRegistry.IMBUEMENT.get(), imbuement);
        return true;
    }
}
