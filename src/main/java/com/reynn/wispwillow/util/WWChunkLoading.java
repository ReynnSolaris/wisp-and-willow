package com.reynn.wispwillow.util;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public class WWChunkLoading {

    // ID for all chunk-loader tickets from this mod
    public static final ResourceLocation CHUNK_LOADER_ID =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "chunk_loader");

    // Controller instance (registered via event)
    public static TicketController CHUNK_LOADER_CONTROLLER;

    /** How long an unrefreshed chunk-loader ticket survives, in ticks. */
    public static final int TICKET_TIMEOUT_TICKS = 20;

    // Namespaced so it can't collide with another mod's "loader" ticket type.
    public static final TicketType<BlockPos> MY_TICKET =
            TicketType.create(WispWillow.MODID + ":chunk_loader", Vec3i::compareTo, TICKET_TIMEOUT_TICKS);

    public static void init(IEventBus modBus) {
        modBus.addListener(WWChunkLoading::onRegisterTicketControllers);
    }

    @SubscribeEvent
    private static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        // No validation callback needed, just accept all tickets
        CHUNK_LOADER_CONTROLLER = new TicketController(CHUNK_LOADER_ID);
        event.register(CHUNK_LOADER_CONTROLLER);
    }
}
