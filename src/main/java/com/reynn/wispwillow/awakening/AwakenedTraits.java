package com.reynn.wispwillow.awakening;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.items.Awakening;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * What waking gives a piece, as plain attribute modifiers.
 *
 * <p>Attributes rather than event maths wherever an effect can be one: they show up in the
 * tooltip on their own, stack correctly with enchantments and potions, and disappear the
 * moment the piece leaves its slot with nothing to clean up. The event fires on both sides,
 * so the client's tooltip and the server's numbers come from the same code.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class AwakenedTraits {
    private AwakenedTraits() {}

    public static float meleeDamage(int level) { return 0.5f + 0.5f * level; }
    public static float miningEfficiency(int level) { return 2f + 2f * level; }
    public static float reach(int level) { return 0.25f * level; }
    /**
     * Vanilla caps armour toughness at 20 and a set of empyrite already brings 14, so this is
     * sized to land a full resonant set exactly on the cap: anything more would be clamped away.
     */
    public static float toughness(int level) { return 0.3f * level; }
    /** One heart per awakened armour piece, at every level. */
    public static final float HEALTH_PER_PIECE = 2f;

    @SubscribeEvent
    static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!Awakening.isAwakened(stack)) return;
        GearKind kind = GearKind.of(stack);
        if (kind == GearKind.NONE) return;

        int level = Resonance.level(stack);
        EquipmentSlotGroup slot = kind.slotGroup();

        if (kind.melee()) {
            add(event, Attributes.ATTACK_DAMAGE, "attack_damage", meleeDamage(level),
                    AttributeModifier.Operation.ADD_VALUE, slot);
        }
        if (kind.digs()) {
            add(event, Attributes.MINING_EFFICIENCY, "mining_efficiency", miningEfficiency(level),
                    AttributeModifier.Operation.ADD_VALUE, slot);
            add(event, Attributes.BLOCK_INTERACTION_RANGE, "reach", reach(level),
                    AttributeModifier.Operation.ADD_VALUE, slot);
        }
        if (kind.armor()) {
            add(event, Attributes.ARMOR_TOUGHNESS, "toughness", toughness(level),
                    AttributeModifier.Operation.ADD_VALUE, slot);
            add(event, Attributes.MAX_HEALTH, "health", HEALTH_PER_PIECE,
                    AttributeModifier.Operation.ADD_VALUE, slot);
        }

        Imbuement imbuement = Awakening.imbuementOf(stack);
        if (imbuement == null || !imbuement.accepts(stack)) return;
        switch (imbuement) {
            case CLARITY -> add(event, Attributes.OXYGEN_BONUS, "clarity", Imbuement.clarityOxygen(level),
                    AttributeModifier.Operation.ADD_VALUE, slot);
            // Absorption is capped by this attribute, which is zero by default: without it the
            // Bulwark's shielding would be clamped away the moment it was granted.
            case BULWARK -> add(event, Attributes.MAX_ABSORPTION, "bulwark", Imbuement.bulwarkHearts(level) * 2f,
                    AttributeModifier.Operation.ADD_VALUE, slot);
            case STRIDE -> {
                add(event, Attributes.MOVEMENT_SPEED, "stride", Imbuement.strideSpeed(level),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE, slot);
                add(event, Attributes.STEP_HEIGHT, "stride_step", Imbuement.STRIDE_STEP,
                        AttributeModifier.Operation.ADD_VALUE, slot);
            }
            case SKYSTEP -> {
                add(event, Attributes.FALL_DAMAGE_MULTIPLIER, "skystep", -1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, slot);
                add(event, Attributes.JUMP_STRENGTH, "skystep_jump", Imbuement.skystepJump(level),
                        AttributeModifier.Operation.ADD_VALUE, slot);
            }
            default -> {
                // Weapon and tool imbuements act through events; see AwakenedEffects.
            }
        }
    }

    /** Ids are per slot, so four awakened armour pieces add four modifiers rather than one. */
    private static void add(ItemAttributeModifierEvent event, Holder<Attribute> attribute, String name,
                            double amount, AttributeModifier.Operation op, EquipmentSlotGroup slot) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                "awakened/" + slot.getSerializedName() + "/" + name);
        event.addModifier(attribute, new AttributeModifier(id, amount, op), slot);
    }
}
