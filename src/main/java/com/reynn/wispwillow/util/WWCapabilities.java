package com.reynn.wispwillow.util;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Block capabilities the mod exposes to hoppers, pipes and other mods.
 *
 * <p>These used to sit in the storage network's {@code AANetwork} and moved here when the
 * network was removed.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class WWCapabilities {
    private WWCapabilities() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // The cauldron hands out a different view per side — anything but DOWN inserts to
        // the reagent side, DOWN extracts from the decanting side. That is the convention
        // every automation mod already assumes, so a hopper underneath and a pipe on the
        // flank work with no configuration.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegistry.ALCHEMICAL_CAULDRON.get(),
                (be, side) -> be.itemHandlerFor(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.ALCHEMICAL_CAULDRON.get(),
                (be, side) -> be.fluidHandlerFor(side)
        );
    }
}
