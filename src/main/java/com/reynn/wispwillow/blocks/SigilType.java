package com.reynn.wispwillow.blocks;

import net.minecraft.util.StringRepresentable;

public enum SigilType implements StringRepresentable {
    BASE, RUNES, STAR, TRIAD, SERPENT, ECLIPSE, HEX;
    public String modelName() { return name().toLowerCase(); }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}