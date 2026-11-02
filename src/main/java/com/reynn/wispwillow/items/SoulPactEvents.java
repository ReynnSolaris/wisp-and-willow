package com.reynn.wispwillow.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Gives a primed Soul Pact the first word on a right-click.
 *
 * <p>{@code Item#interactLivingEntity} is too late. {@code Player#interactOn} offers the
 * click to the creature before it ever reaches the held item, and an allay with an empty
 * hand accepts anything you are holding — so a pact aimed at an allay was handed to the
 * allay instead. {@link PlayerInteractEvent.EntityInteract} fires ahead of both.</p>
 *
 * <p>Refusals cancel the click too, for the same reason: telling someone "nothing in the
 * cow answers" is no good if the pact has already been eaten.</p>
 *
 * <p>Registered by hand from the mod constructor, matching
 * {@link com.reynn.wispwillow.world.WillowInfusionHandler} — annotating it as well would
 * run it twice per click.</p>
 */
public final class SoulPactEvents {
    private SoulPactEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        InteractionResult result =
                SoulPactItem.tryCapture(event.getEntity(), target, event.getItemStack());
        if (result == null) return;

        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
