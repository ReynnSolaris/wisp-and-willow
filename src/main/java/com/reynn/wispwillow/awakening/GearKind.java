package com.reynn.wispwillow.awakening;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * What sort of piece a stack is, as far as awakening cares.
 *
 * <p>Read off the item class rather than a tag, so it follows the vanilla hierarchy: an axe
 * is a {@link DiggerItem} and a weapon at once, and wakes as both.</p>
 */
public enum GearKind {
    NONE, SWORD, AXE, DIGGER, HELMET, CHESTPLATE, LEGGINGS, BOOTS;

    public static GearKind of(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        Item item = stack.getItem();
        if (item instanceof SwordItem) return SWORD;
        if (item instanceof AxeItem) return AXE; // before DiggerItem, which it extends
        if (item instanceof DiggerItem) return DIGGER;
        if (item instanceof ArmorItem armor) {
            return switch (armor.getType()) {
                case HELMET -> HELMET;
                case CHESTPLATE -> CHESTPLATE;
                case LEGGINGS -> LEGGINGS;
                case BOOTS -> BOOTS;
                case BODY -> NONE;
            };
        }
        return NONE;
    }

    public boolean melee() { return this == SWORD || this == AXE; }

    public boolean digs() { return this == AXE || this == DIGGER; }

    public boolean armor() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    /** Where this piece has to be for its traits to count. */
    public EquipmentSlotGroup slotGroup() {
        return switch (this) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.MAINHAND;
        };
    }
}
