package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.entity.familiar.Developers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base of the developer kit: tools for testing one feature without playing up to it.
 *
 * <p>None of these have a recipe. They live in their own creative tab, and every one of them
 * checks {@link #gate} before doing anything, so a stack that reaches a survival inventory by
 * accident (a dropped item, a chest from a creative world) is inert there. Creative mode or a
 * listed developer ({@link Developers}) is what unlocks them.</p>
 *
 * <p>Each tool's tooltip is its own {@code item.wispwillow.<id>.desc} line plus the shared
 * creative-only notice. Keep this class free of client-only types.</p>
 */
public abstract class DevToolItem extends Item {

    protected DevToolItem(Properties props) {
        super(props);
    }

    /** Creative mode, or a listed developer: the two kinds of player these tools answer to. */
    public static boolean allowed(Player player) {
        return player.getAbilities().instabuild || Developers.is(player);
    }

    /**
     * The gate every tool runs first. Tells the player, once, why nothing happened.
     *
     * @return true if the tool may act
     */
    public static boolean gate(Player player) {
        if (allowed(player)) return true;
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("item.wispwillow.dev_tool.denied")
                    .withStyle(ChatFormatting.RED), true);
        }
        return false;
    }

    // ---- report helpers: chat lines in one consistent voice ----

    protected static void chat(Player player, Component line) {
        player.sendSystemMessage(line);
    }

    protected static void overlay(Player player, Component line) {
        player.displayClientMessage(line, true);
    }

    /** A heading: "[Probe] Willow Altar at 12 64 -3". */
    protected static MutableComponent head(String tool, Component what) {
        return Component.literal("[" + tool + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(what.copy().withStyle(ChatFormatting.WHITE));
    }

    /** "  label: value" with the label greyed. */
    protected static MutableComponent kv(String label, Component value) {
        return Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY).append(value);
    }

    protected static MutableComponent kv(String label, String value) {
        return kv(label, Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    protected static MutableComponent good(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    protected static MutableComponent warn(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".sneak").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.dev_tool")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
