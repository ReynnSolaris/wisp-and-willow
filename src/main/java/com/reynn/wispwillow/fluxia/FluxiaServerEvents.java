package com.reynn.wispwillow.fluxia;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.entity.AxolotlFamiliarEntity;
import com.reynn.wispwillow.entity.BatFamiliarEntity;
import com.reynn.wispwillow.entity.FrogFamiliarEntity;
import com.reynn.wispwillow.entity.WispFamiliarEntity;
import com.reynn.wispwillow.fluxia.net.FluxiaSyncPayload;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WispWillow.MODID)
public class FluxiaServerEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.AXOLOTL_FAMILIAR.get(), AxolotlFamiliarEntity.createAttributes().build());
        event.put(EntityRegistry.WISP_FAMILIAR.get(), WispFamiliarEntity.createAttributes().build());
        event.put(EntityRegistry.FROG_FAMILIAR.get(), FrogFamiliarEntity.createAttributes().build());
        event.put(EntityRegistry.BAT_FAMILIAR.get(), BatFamiliarEntity.createAttributes().build());
        event.put(EntityRegistry.RAVEN_FAMILIAR.get(), com.reynn.wispwillow.entity.RavenFamiliarEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        var p = e.getEntity();
        if (!(p instanceof ServerPlayer sp)) return;

        var fx = sp.getData(AttachmentRegistry.FLUXIA.get());
        if (fx == null) return;

        // regen once per second
        if (sp.tickCount % 20 == 0 && fx.amount() < fx.capacity()) {
            int before = fx.amount();
            fx.set(Math.min(fx.capacity(), fx.amount() + (fx.regen())));
            if (fx.amount() != before) {
                PacketDistributor.sendToPlayer(sp, FluxiaSyncPayload.from(fx));
            }
        }
    }
}
