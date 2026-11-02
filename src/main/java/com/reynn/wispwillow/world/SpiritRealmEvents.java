package com.reynn.wispwillow.world;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.worldgen.spirit.SpiritRealm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The realm never keeps anyone.
 *
 * <p>A vigil returns every traveller when it ends, but it can only return the ones who are actually
 * logged in. Someone who quits mid-excursion is still in the Spirit Realm when the vigil burns out,
 * and comes back to a world with no altar holding a way open. This closes that at the other end:
 * arrive in the realm at login and you are put back out, to the spot your way in was recorded from,
 * or to your spawn if that is gone.</p>
 *
 * <p>It also covers a migration that would otherwise be fatal. The realm used to run from y=-64 and
 * now runs from y=0, so a player saved at an old low altitude is <em>below the world</em> the moment
 * it loads.</p>
 *
 * <p><b>Why this waits a second instead of acting on the event.</b> Moving the player from inside
 * {@link PlayerEvent.PlayerLoggedInEvent}, or from a {@code server.execute} queued by it, does not
 * stick: the rest of the login sequence runs afterwards and puts them back at their saved position,
 * in the dimension they were saved in. Both were measured failing on 2026-09-16 — the log said it
 * had moved them and nearly two seconds later they were still standing in the realm. Counting down
 * a few ticks and moving them once login has settled is what actually works.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class SpiritRealmEvents {
    private SpiritRealmEvents() {}

    /** Long enough for the join to finish before we touch the player's position. */
    private static final int SETTLE_TICKS = 30;

    private static final Map<UUID, Integer> pending = new HashMap<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(SpiritRealm.LEVEL)) return;
        pending.put(player.getUUID(), SETTLE_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (pending.isEmpty()) return;
        MinecraftServer server = event.getServer();

        Iterator<Map.Entry<UUID, Integer>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (entry.setValue(entry.getValue() - 1) > 1) continue;
            it.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) continue;
            if (!player.level().dimension().equals(SpiritRealm.LEVEL)) continue;

            WispWillow.LOGGER.info("{} logged in inside the Spirit Realm; putting them out",
                    player.getName().getString());
            // Not graceful: from the player's point of view the way closed while they were gone.
            SpiritTravel.returnHome(player, server, false);
        }
    }
}
