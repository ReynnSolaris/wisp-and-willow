package com.reynn.wispwillow.fluxia.net;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = WispWillow.MODID)
public final class FluxiaNetwork {
    private FluxiaNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var reg = e.registrar(WispWillow.MODID);

        // S2C: update the local attachment and refresh the HUD
        reg.playToClient(
                FluxiaSyncPayload.TYPE,
                FluxiaSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var p = Minecraft.getInstance().player;
                    if (p == null) return;
                    var fx = p.getData(AttachmentRegistry.FLUXIA.get());
                    fx.setMax(payload.max());
                    fx.setRegen(payload.regen());
                    fx.setFlux(payload.flux());
                })
        );

        // S2C: a transfer in flight, drawn as a travelling mote. This used to be registered by
        // the storage network's AANetwork; spitters and links send it every transfer, and an
        // unregistered payload makes NeoForge refuse the send outright.
        reg.playToClient(FluxArcS2C.TYPE, FluxArcS2C.STREAM_CODEC, FluxArcS2C::handle);
    }
}
