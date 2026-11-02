package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.phasing.PhasingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Status effects this mod defines.
 */
public final class EffectRegistry {
    private EffectRegistry() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, WispWillow.MODID);

    /**
     * What Liquid Fluxia does to anything that goes under: the body stops agreeing with
     * stone. See {@link com.reynn.wispwillow.phasing.Phasing}.
     */
    public static final DeferredHolder<MobEffect, PhasingEffect> PHASING =
            EFFECTS.register("phasing", PhasingEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
