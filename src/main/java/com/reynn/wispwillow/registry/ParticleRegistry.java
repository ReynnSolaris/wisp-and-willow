package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, WispWillow.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLUX_SPARK =
            PARTICLES.register("flux_spark", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
