package com.reynn.wispwillow.client.goggles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.text.NumberFormat;
import java.util.Locale;

/** Shared formatting so every goggle panel reads the same way. */
public final class GoggleText {
    private GoggleText() {}

    public static final int FLUXIA_COLOR = 0xFF7FE8FF;
    public static final int NETWORK_COLOR = 0xFFB28BFF;

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);

    public static String num(long value) {
        return NUMBERS.format(value);
    }

    /** "Stored: 12,340 / 40,000 f" with the label greyed and the value tinted. */
    public static Component amount(String labelKey, long value, long max, String unit) {
        return Component.translatable(labelKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(num(value) + " / " + num(max) + " " + unit)
                        .withStyle(ChatFormatting.AQUA));
    }

    public static Component line(String labelKey, Component value) {
        return Component.translatable(labelKey).withStyle(ChatFormatting.GRAY).append(value);
    }

    public static Component line(String labelKey, String value) {
        return line(labelKey, Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    public static MutableComponent good(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    public static MutableComponent warn(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    /** Ticks rendered as a short "12.5s". */
    public static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0f);
    }
}
