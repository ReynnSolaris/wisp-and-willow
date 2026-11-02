package com.reynn.wispwillow.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Small helpers shared by the JEI categories. */
final class JeiSlots {
    static final int SLOT = 18;
    static final int PAD = 4;
    /**
     * JEI pages are light grey, where bright colours (aqua, gold, light purple) wash out, so the
     * categories use the dark variants and this for gold.
     */
    static final int DARK_GOLD = 0x9A6400;

    private JeiSlots() {}

    /**
     * The altar and the pool take one item per ingredient, so a rite needing four dragon
     * breath carries four identical ingredients. Collapse those into one slot showing the
     * stack at that count, keyed off the ingredient's first item.
     */
    static List<List<ItemStack>> collapse(List<Ingredient> ingredients) {
        LinkedHashMap<String, int[]> counts = new LinkedHashMap<>();
        LinkedHashMap<String, Ingredient> exemplars = new LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            ItemStack[] items = ing.getItems();
            String key = items.length == 0 ? "?" : BuiltInRegistries.ITEM.getKey(items[0].getItem()).toString();
            counts.computeIfAbsent(key, k -> new int[1])[0]++;
            exemplars.putIfAbsent(key, ing);
        }
        List<List<ItemStack>> out = new ArrayList<>(counts.size());
        for (var e : counts.entrySet()) {
            out.add(stacks(exemplars.get(e.getKey()), e.getValue()[0]));
        }
        return out;
    }

    /** Every item an ingredient accepts, at the given count. */
    static List<ItemStack> stacks(Ingredient ingredient, int count) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) out.add(stack.copyWithCount(count));
        return out;
    }

    /** 25000 as "25,000", the way the goggles print it. */
    static String num(int value) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT).format(value);
    }

    static int rows(int slots, int perRow) {
        return Math.max(1, (slots + perRow - 1) / perRow);
    }

    /** Draws a line of text the way the category pages do: styled component, no shadow. */
    static void text(GuiGraphics g, Component text, int x, int y) {
        g.drawString(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF, false);
    }

    static void textRight(GuiGraphics g, Component text, int right, int y) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, text, right - font.width(text), y, 0xFFFFFFFF, false);
    }
}
