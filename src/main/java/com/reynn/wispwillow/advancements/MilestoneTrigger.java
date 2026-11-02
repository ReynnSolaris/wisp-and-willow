package com.reynn.wispwillow.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * One trigger for the handful of moments no vanilla criterion can see.
 *
 * <p>Reaching Resonance V, wearing the whole Regalia, and having a rite wake or imbue your
 * gear are all things that happen to a data component or to a set of worn items, and an
 * advancement can only match a component <em>exactly</em> — there is no "resonance of at
 * least" and no "carries any imbuement". Rather than four near-identical triggers, this is
 * one trigger with a name on it, fired from the code that already knows the moment
 * arrived.</p>
 */
public class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.TriggerInstance> {

    @Override
    public Codec<MilestoneTrigger.TriggerInstance> codec() {
        return MilestoneTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String milestone) {
        this.trigger(player, instance -> instance.milestone().equals(milestone));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String milestone)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<MilestoneTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                .forGetter(MilestoneTrigger.TriggerInstance::player),
                        Codec.STRING.fieldOf("milestone")
                                .forGetter(MilestoneTrigger.TriggerInstance::milestone)
                ).apply(instance, MilestoneTrigger.TriggerInstance::new));

        public static Criterion<MilestoneTrigger.TriggerInstance> reached(String milestone) {
            return WWTriggers.MILESTONE.get().createCriterion(
                    new MilestoneTrigger.TriggerInstance(Optional.empty(), milestone));
        }
    }
}
