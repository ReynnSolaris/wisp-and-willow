package com.reynn.wispwillow.net;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.items.OmniChalkItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Payloads that are not part of the Fluxia network's own traffic. */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class WWNetwork {
    private WWNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var reg = e.registrar(WispWillow.MODID);

        reg.playToServer(
                OmniChalkCyclePayload.TYPE,
                OmniChalkCyclePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    // Either hand: the chalk is a two-handed tool in practice, held in the
                    // off hand while the main hand holds the book.
                    ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (!(stack.getItem() instanceof OmniChalkItem)) {
                        stack = player.getItemInHand(InteractionHand.OFF_HAND);
                    }
                    if (!(stack.getItem() instanceof OmniChalkItem)) return;

                    OmniChalkItem.cycle(stack, payload.delta(), payload.chapter());
                    player.displayClientMessage(Component.translatable(
                            "item.wispwillow.omni_chalk.selected",
                            OmniChalkItem.selectionName(stack)), true);
                })
        );
    }
}
