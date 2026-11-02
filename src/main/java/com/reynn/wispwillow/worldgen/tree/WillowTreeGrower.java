package com.reynn.wispwillow.worldgen.tree;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.worldgen.ConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class WillowTreeGrower {
    public static TreeGrower willowTreeGrower = new TreeGrower(WispWillow.MODID+":willow",
            Optional.empty(),
            Optional.of(ConfiguredFeatures.WILLOW_TREE_KEY),
            Optional.empty());
}
