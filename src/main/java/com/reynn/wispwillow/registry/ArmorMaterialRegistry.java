package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ArmorMaterialRegistry {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, WispWillow.MODID);

    /**
     * The goggles are lenses, not protection: one point of armour, so wearing them is a
     * real trade against a helmet rather than a free upgrade.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARCANE =
            ARMOR_MATERIALS.register("arcane", () -> new ArmorMaterial(
                    defense(1),
                    8,                                   // enchantability
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(net.minecraft.world.item.Items.AMETHYST_SHARD),
                    List.of(new ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(WispWillow.MODID, "arcane_goggles"))),
                    0.0f,                                // toughness
                    0.0f                                 // knockback resistance
            ));

    /**
     * Empyrite plate: a shade better than netherite everywhere, and far better to enchant.
     *
     * <p>The knockback resistance is deliberately kept: it is netherite's signature property
     * and a tier above it that lost that would feel like a downgrade in the only fight that
     * matters. Toughness sits just above netherite's for the same reason.</p>
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EMPYRITE =
            ARMOR_MATERIALS.register("empyrite", () -> new ArmorMaterial(
                    fullSet(4, 8, 6, 4),
                    22,                                  // enchantability
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ItemRegistry.EMPYRITE_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(WispWillow.MODID, "empyrite"))),
                    3.5f,                                // toughness
                    0.1f                                 // knockback resistance
            ));

    /**
     * Glimmerweave: cloth spun from a glimmerfawn's down.
     *
     * <p>Iron's protection in a robe, nothing at all in toughness or knockback, and the best
     * enchantability in the mod — it is a mage's set, not a soldier's. The magic proper is
     * the set bonus in {@link com.reynn.wispwillow.items.GlimmerweaveSet}: two pieces and
     * falling cannot hurt you, four and you see in the dark.</p>
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GLIMMERWEAVE =
            ARMOR_MATERIALS.register("glimmerweave", () -> new ArmorMaterial(
                    fullSet(2, 5, 6, 2),
                    30,                                  // enchantability — gold is 25
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ItemRegistry.GLIMMERWEAVE.get()),
                    List.of(new ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(WispWillow.MODID, "glimmerweave"))),
                    0.0f,                                // toughness
                    0.0f                                 // knockback resistance
            ));

    private static EnumMap<ArmorItem.Type, Integer> fullSet(int boots, int legs, int chest, int helmet) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) map.put(type, 0);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.HELMET, helmet);
        // Body is for wolves and horses; a player set never reads it, but leaving it at zero
        // would make the material useless for a wolf armour item later on.
        map.put(ArmorItem.Type.BODY, chest);
        return map;
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int head) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) map.put(type, 0);
        map.put(ArmorItem.Type.HELMET, head);
        return map;
    }

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }

    private ArmorMaterialRegistry() {}
}
