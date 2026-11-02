package com.reynn.wispwillow.items;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Glimmerweave, counted piece by piece.
 *
 * <p>Cloth spun from a glimmerfawn's down keeps some of the fawn's lightness. Two pieces and
 * the wearer lands like down (no fall damage at all); four and they see in the dark the way
 * the fawn does in its realm. Any living thing wearing it counts, so a zombie that picks a
 * hood and robe up off the ground lands softly too — which is what makes it feel like a
 * property of the cloth rather than a perk of the player.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class GlimmerweaveSet {
    private GlimmerweaveSet() {}

    public static final int DOWNSOFT = 2;
    public static final int GLIMMER_SIGHT = 4;

    private static final EquipmentSlot[] ARMOR =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static boolean isPiece(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.GLIMMERWEAVE_ARMOR);
    }

    public static int count(LivingEntity wearer) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (isPiece(wearer.getItemBySlot(slot))) n++;
        }
        return n;
    }

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (count(event.getEntity()) >= DOWNSOFT) {
            event.setDamageMultiplier(0f);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (count(player) < GLIMMER_SIGHT) return;
        // Refreshed well above the ten seconds at which night vision starts to flicker.
        MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
        if (sight == null || sight.getDuration() < 240) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
        }
    }

    /** The two thresholds, on every piece, so the set explains itself in the inventory. */
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (!isPiece(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("item.wispwillow.glimmerweave.set.two").withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("item.wispwillow.glimmerweave.set.four").withStyle(ChatFormatting.AQUA));
    }
}
