package com.reynn.wispwillow.items;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.EmpyreanSet;
import com.reynn.wispwillow.awakening.GearKind;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.awakening.Resonance;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Names the embuement on an awakened piece, and everything that has grown out of it.
 *
 * <p>The gold glint already says something has happened to the metal; only the tooltip says
 * what. Right under the name, above the attribute block the traits themselves appear in:
 * resonance and how far it is to the next level, the imbuement and what it does at this level,
 * and on armour how much of the Regalia is being worn.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class AwakeningTooltip {
    private AwakeningTooltip() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        GearKind kind = GearKind.of(stack);
        List<Component> lines = new ArrayList<>();

        if (!Awakening.isAwakened(stack)) {
            // Point sleeping gear at the next step, on the pieces that have one to take.
            if (kind != GearKind.NONE && stack.is(TagRegistry.Items.EMPYRITE)) {
                lines.add(Component.translatable("awakening.wispwillow.dormant")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        } else {
            MutableComponent head = Component.translatable("item.wispwillow.awakened")
                    .withStyle(ChatFormatting.GOLD);
            lines.add(head);

            int level = Resonance.level(stack);
            if (kind != GearKind.NONE) {
                head.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("awakening.wispwillow.resonance",
                                Resonance.roman(level)).withStyle(ChatFormatting.YELLOW));
                lines.add(indent(level < Resonance.MAX_LEVEL
                        ? Component.translatable("awakening.wispwillow.progress", Resonance.points(stack),
                                Resonance.threshold(level + 1), Resonance.roman(level + 1))
                        : Component.translatable("awakening.wispwillow.maxed"))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            Imbuement imbuement = Awakening.imbuementOf(stack);
            if (imbuement != null) {
                lines.add(Component.translatable("awakening.wispwillow.imbued", imbuement.displayName())
                        .withStyle(ChatFormatting.GRAY));
                lines.add(indent(imbuement.describe(level)).withStyle(ChatFormatting.GRAY));
            }

            if (kind.armor()) addSetLines(lines, event.getEntity());
        }

        if (!lines.isEmpty()) {
            List<Component> tip = event.getToolTip();
            tip.addAll(Math.min(1, tip.size()), lines);
        }
    }

    private static void addSetLines(List<Component> lines, @Nullable Player player) {
        int worn = player == null ? 0 : EmpyreanSet.count(player);
        lines.add(Component.translatable("awakening.wispwillow.set", worn, EmpyreanSet.AEGIS)
                .withStyle(worn >= EmpyreanSet.EMBERPROOF ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        lines.add(indent(Component.translatable("awakening.wispwillow.set.two"))
                .withStyle(worn >= EmpyreanSet.EMBERPROOF ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        lines.add(indent(Component.translatable("awakening.wispwillow.set.four"))
                .withStyle(worn >= EmpyreanSet.AEGIS ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent indent(Component line) {
        return Component.literal("  ").append(line);
    }
}
