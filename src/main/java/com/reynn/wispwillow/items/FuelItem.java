package com.reynn.wispwillow.items;

import net.minecraft.world.item.Item;

public class FuelItem extends Item {
    private int burnTime = 0;
    public FuelItem(Properties properties, int burnTime) {
        super(properties);
        this.burnTime = burnTime;
    }

    public int getBurnTime() {
        return this.burnTime;
    }
}
