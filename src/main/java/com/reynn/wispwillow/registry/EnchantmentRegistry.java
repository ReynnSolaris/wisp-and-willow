package com.reynn.wispwillow.registry;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.enchantment.VolatileExperimentEnchantmentEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EnchantmentRegistry {

    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, WispWillow.MODID);

    public static final Supplier<MapCodec<? extends EnchantmentEntityEffect>> VOLATILE_EXPERIMENT =
            ENTITY_ENCHANTMENT_EFFECTS.register("volatile_experiment", () -> VolatileExperimentEnchantmentEffect.CODEC);



    public static void register(IEventBus eventBus) {
        ENTITY_ENCHANTMENT_EFFECTS.register(eventBus);
    }
}
