package com.reynn.wispwillow.blocks;

import net.minecraft.util.StringRepresentable;

/**
 * The four foundation runes. Every rune counts toward the altar's tier; three of them also
 * grant a scaling bonus, so how you fill a ring matters as much as filling it.
 */
public enum RuneType implements StringRepresentable {
    /** Structural filler. Cheap, counts for tier, grants nothing else. */
    BLANK("blank", 0x9AA0B5),
    /** Deepens the altar's Fluxia reserve. */
    WATER("water", 0x4FA8E8),
    /** Shaves Fluxia off the cost of every rite. */
    EARTH("earth", 0x7BC24A),
    /** Chance for a rite to yield extra output. */
    BLOOD("blood", 0xC23B3B);

    private final String name;
    private final int color;

    RuneType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() { return name; }

    public String itemName() { return name + "_rune"; }

    /** Accent colour, used by the goggles readout and the grimoire. */
    public int color() { return color; }

    /** Extra Fluxia reserve granted per rune of this type. */
    public int capacityBonus() { return this == WATER ? 2000 : 0; }

    /** Fraction of a rite's Fluxia cost removed per rune of this type. */
    public float efficiencyBonus() { return this == EARTH ? 0.005f : 0f; }

    /** Chance per rune that a rite yields a bonus copy of its output. */
    public float yieldBonus() { return this == BLOOD ? 0.004f : 0f; }
}
