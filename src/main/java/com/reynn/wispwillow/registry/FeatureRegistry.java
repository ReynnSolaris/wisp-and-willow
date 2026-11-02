package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.worldgen.feature.BogPoolFeature;
import com.reynn.wispwillow.worldgen.feature.FallenLogFeature;
import com.reynn.wispwillow.worldgen.feature.FenFloorFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritFloorFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritRuinFeature;
import com.reynn.wispwillow.worldgen.feature.SpiritSpireFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Feature types whose shape vanilla has no configuration for. */
public class FeatureRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, WispWillow.MODID);

    public static final DeferredHolder<Feature<?>, FallenLogFeature> FALLEN_LOG =
            FEATURES.register("fallen_log", () -> new FallenLogFeature(FallenLogFeature.Config.CODEC));

    public static final DeferredHolder<Feature<?>, BogPoolFeature> BOG_POOL =
            FEATURES.register("bog_pool", () -> new BogPoolFeature(BogPoolFeature.Config.CODEC));

    public static final DeferredHolder<Feature<?>, FenFloorFeature> FEN_FLOOR =
            FEATURES.register("fen_floor", () -> new FenFloorFeature(FenFloorFeature.Config.CODEC));

    /** The same trick as the fen floor, over end stone: what makes the realm a meadow. */
    public static final DeferredHolder<Feature<?>, SpiritFloorFeature> SPIRIT_FLOOR =
            FEATURES.register("spirit_floor", () -> new SpiritFloorFeature(SpiritFloorFeature.Config.CODEC));

    /** Standing stones on the islands, at both the cairn and the landmark scale. */
    public static final DeferredHolder<Feature<?>, SpiritRuinFeature> SPIRIT_RUIN =
            FEATURES.register("spirit_ruin", () -> new SpiritRuinFeature(SpiritRuinFeature.Config.CODEC));

    /** Tapering spiritstone pillars: the realm's only natural verticals. */
    public static final DeferredHolder<Feature<?>, SpiritSpireFeature> SPIRIT_SPIRE =
            FEATURES.register("spirit_spire", () -> new SpiritSpireFeature(SpiritSpireFeature.Config.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
