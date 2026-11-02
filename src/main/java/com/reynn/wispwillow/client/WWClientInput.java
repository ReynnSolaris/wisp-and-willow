package com.reynn.wispwillow.client;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.items.OmniChalkItem;
import com.reynn.wispwillow.net.OmniChalkCyclePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sneak and scroll while holding the Omni Chalk to change which rite it draws.
 *
 * <p>The scroll is swallowed rather than passed on, so the hotbar does not slide out from
 * under the tool you are configuring — the same bargain every mod that binds this gesture
 * makes, and the reason it is gated on sneaking rather than on merely holding the item.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class WWClientInput {
    private WWClientInput() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!mc.player.isShiftKeyDown()) return;

        boolean holding = mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof OmniChalkItem
                || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof OmniChalkItem;
        if (!holding) return;

        double dy = event.getScrollDeltaY();
        if (dy == 0) return;

        event.setCanceled(true);
        PacketDistributor.sendToServer(new OmniChalkCyclePayload(dy > 0 ? 1 : -1, false));
    }
}
