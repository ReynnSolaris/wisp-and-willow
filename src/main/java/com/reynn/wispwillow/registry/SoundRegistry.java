package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, WispWillow.MODID);

    public static Supplier<SoundEvent> ITEM_INFUSION = registerSoundEvent("infusion");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
