package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.awakening.Resonance;
import com.reynn.wispwillow.items.Awakening;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

/**
 * Steps an Empyrite piece through every awakening state without the rites.
 *
 * <p>Hold the piece in the other hand. Use: a dormant piece wakes at Resonance I; an awake
 * one climbs a level, and past V drops back to I. Sneak-use: cycles the imbuement through
 * the ones that suit the piece, then back to none. Between them that is every combination the
 * traits, the tooltip and the renderer have to handle.</p>
 */
public class ResonanceTunerItem extends DevToolItem {

    public ResonanceTunerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tuner = player.getItemInHand(hand);
        if (!gate(player)) return InteractionResultHolder.fail(tuner);

        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack piece = player.getItemInHand(other);
        if (piece.isEmpty() || !piece.is(TagRegistry.Items.EMPYRITE)) {
            if (!level.isClientSide) {
                overlay(player, Component.translatable("item.wispwillow.resonance_tuner.no_piece")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(tuner);
        }
        if (level.isClientSide) return InteractionResultHolder.success(tuner);

        if (player.isSecondaryUseActive()) {
            if (!Awakening.isAwakened(piece)) {
                overlay(player, Component.translatable("item.wispwillow.resonance_tuner.asleep")
                        .withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(tuner);
            }
            List<Imbuement> options = Arrays.stream(Imbuement.values()).filter(i -> i.accepts(piece)).toList();
            if (options.isEmpty()) {
                overlay(player, Component.translatable("item.wispwillow.resonance_tuner.nothing_suits")
                        .withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(tuner);
            }
            Imbuement current = Awakening.imbuementOf(piece);
            int index = current == null ? -1 : options.indexOf(current);
            Imbuement next = index + 1 < options.size() ? options.get(index + 1) : null;
            if (next == null) {
                piece.remove(ComponentRegistry.IMBUEMENT.get());
                overlay(player, Component.translatable("item.wispwillow.resonance_tuner.imbuement",
                        Component.translatable("item.wispwillow.resonance_tuner.none")));
            } else {
                // Set directly rather than through Awakening.imbue: that refuses a repeat, and a
                // tester cycling through the list wants every stop on it.
                piece.set(ComponentRegistry.IMBUEMENT.get(), next);
                overlay(player, Component.translatable("item.wispwillow.resonance_tuner.imbuement", next.displayName()));
            }
        } else if (!Awakening.isAwakened(piece)) {
            Awakening.awaken(piece);
            overlay(player, Component.translatable("item.wispwillow.resonance_tuner.awakened"));
        } else {
            int current = Resonance.level(piece);
            int next = current >= Resonance.MAX_LEVEL ? 1 : current + 1;
            piece.set(ComponentRegistry.RESONANCE.get(), Resonance.threshold(next));
            overlay(player, Component.translatable("item.wispwillow.resonance_tuner.resonance", Resonance.roman(next)));
        }

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        return InteractionResultHolder.consume(tuner);
    }
}
