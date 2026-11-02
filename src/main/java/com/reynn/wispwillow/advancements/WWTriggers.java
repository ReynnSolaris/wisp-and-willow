package com.reynn.wispwillow.advancements;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own advancement criteria, and the names its milestones go by.
 *
 * <p>The names are constants rather than loose strings because they are written into the
 * generated advancement JSON on one side and fired from game code on the other; a typo
 * between the two would simply mean an advancement that never completes.</p>
 */
public final class WWTriggers {
    private WWTriggers() {}

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, WispWillow.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, MilestoneTrigger> MILESTONE =
            TRIGGERS.register("milestone", MilestoneTrigger::new);

    /** A rite woke a piece of empyrite in your circle. */
    public static final String AWAKENED = "awakened";
    /** A rite taught an awakened piece an imbuement. */
    public static final String IMBUED = "imbued";
    /** Something you carry reached Resonance V. */
    public static final String RESONANCE_V = "resonance_v";
    /** All four awakened armour pieces, worn at once. */
    public static final String REGALIA = "regalia";

    /** Fires for a real player and quietly does nothing for anything else. */
    public static void fire(Player player, String milestone) {
        if (player instanceof ServerPlayer server) {
            MILESTONE.get().trigger(server, milestone);
        }
    }

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }
}
