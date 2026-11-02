package com.reynn.wispwillow.entity.wild;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.entity.spirit.GlimmerfawnEntity;
import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Attributes and natural-spawn rules for the creatures that live in the world, not at an altar. */
@EventBusSubscriber(modid = WispWillow.MODID)
public class WildlifeEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.WISPFLY.get(), WispflyEntity.createAttributes().build());
        event.put(EntityRegistry.FEN_HERON.get(), FenHeronEntity.createAttributes().build());
        event.put(EntityRegistry.WILLOW_HOUND.get(), WillowHoundEntity.createAttributes().build());
        event.put(EntityRegistry.GLIMMERFAWN.get(), GlimmerfawnEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(EntityRegistry.WISPFLY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WispflyEntity::checkWispflySpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EntityRegistry.FEN_HERON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FenHeronEntity::checkHeronSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EntityRegistry.WILLOW_HOUND.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WillowHoundEntity::checkHoundSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EntityRegistry.GLIMMERFAWN.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GlimmerfawnEntity::checkGlimmerfawnSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
