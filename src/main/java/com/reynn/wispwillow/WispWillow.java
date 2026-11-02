package com.reynn.wispwillow;

import com.mojang.logging.LogUtils;
import com.reynn.wispwillow.registry.*;
import com.reynn.wispwillow.items.SoulPactEvents;
import com.reynn.wispwillow.util.WWChunkLoading;
import com.reynn.wispwillow.world.WillowInfusionHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WispWillow.MODID)
public class WispWillow {
    public static final String MODID = "wispwillow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WispWillow(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        ParticleRegistry.register(modEventBus);
        FluidRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        ArmorMaterialRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MainRegistry.register(modEventBus);
        EnchantmentRegistry.register(modEventBus);
        RecipeRegistry.register(modEventBus);
        WWChunkLoading.init(modEventBus);
        SoundRegistry.register(modEventBus);
        AttachmentRegistry.ATTACHMENTS.register(modEventBus);
        StructureRegistry.DEFERRED_REGISTRY_STRUCTURE.register(modEventBus);
        StructureRegistry.DEFERRED_REGISTRY_STRUCTURE_PIECE.register(modEventBus);
        StructurePlacementRegistry.DEFERRED_REGISTRY_STRUCTURE_PLACEMENT_TYPE.register(modEventBus);
        EntityRegistry.register(modEventBus);
        FeatureRegistry.register(modEventBus);
        ComponentRegistry.register(modEventBus);
        EffectRegistry.register(modEventBus);
        com.reynn.wispwillow.advancements.WWTriggers.register(modEventBus);
        NeoForge.EVENT_BUS.register(WillowInfusionHandler.class);
        NeoForge.EVENT_BUS.register(SoulPactEvents.class);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, "wispwillow-client.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC, "wispwillow-server.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("Wisp & Willow common setup complete.");
    }
}
