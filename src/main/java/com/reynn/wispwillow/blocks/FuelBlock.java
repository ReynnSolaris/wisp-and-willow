package com.reynn.wispwillow.blocks;

import net.minecraft.world.level.block.Block;

public class FuelBlock extends Block {
    private int burnTime = 0;
    public FuelBlock(Properties properties, int burnTime) {
        super(properties);
        this.burnTime = burnTime;
    }

    public int getBurnTime() {
        return this.burnTime;
    }
}
