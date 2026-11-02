package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WispWillow.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SimpleFluxiaStorage>> FLUXIA =
            ATTACHMENTS.register("player_fluxia", () ->
                    AttachmentType.builder(() -> new SimpleFluxiaStorage(100, 2, 2, 2))
                            .serialize(SimpleFluxiaStorage.CODEC)           // <-- REQUIRED for copyOnDeath
                            .sync(SimpleFluxiaStorage.STREAM_CODEC) // client HUD sync-friendly
                            .copyOnDeath()                                     // now valid
                            .build()
            );

    private AttachmentRegistry() {}
}
