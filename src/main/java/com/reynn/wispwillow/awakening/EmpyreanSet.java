package com.reynn.wispwillow.awakening;

import com.reynn.wispwillow.items.Awakening;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * The Empyrean Regalia: awakened empyrite armour, counted piece by piece.
 *
 * <p>Two pieces make the wearer Emberproof, four add the Radiant Aegis. Only awakened pieces
 * count, which is the point — the set bonus is the reward for the rite, not for the smithing.</p>
 */
public final class EmpyreanSet {
    private EmpyreanSet() {}

    public static final int EMBERPROOF = 2;
    public static final int AEGIS = 4;

    /** Lava still hurts an Emberproof wearer, by this much of what it would. */
    public static final float LAVA_FACTOR = 0.5f;
    /** Fraction of incoming damage the Aegis turns away. */
    public static final float AEGIS_REDUCTION = 0.15f;
    /** How long a melee attacker burns for striking an Aegis wearer. */
    public static final float AEGIS_BURN_SECONDS = 4f;
    /** Resonance multiplier for everything an Aegis wearer carries. */
    public static final int AEGIS_RESONANCE = 2;

    private static final EquipmentSlot[] ARMOR =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static boolean isPiece(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                && stack.is(TagRegistry.Items.EMPYRITE)
                && Awakening.isAwakened(stack);
    }

    public static int count(LivingEntity wearer) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (isPiece(wearer.getItemBySlot(slot))) n++;
        }
        return n;
    }

    public static EquipmentSlot[] armorSlots() {
        return ARMOR.clone();
    }
}
