package com.reynn.wispwillow.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

public class AmethystFarmland extends FarmBlock {
    public AmethystFarmland(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState blockstate = level.getBlockState(pos.above());
        Block aboveBlock = blockstate.getBlock();
        if (aboveBlock instanceof CropBlock bb) {
            int i = bb.getAge(blockstate);
            if (i < bb.getMaxAge()) {
                float f = 6;
                if (CommonHooks.canCropGrow(level, pos.above(), blockstate, random.nextInt((int) (25.0F / f) + 1) == 0)) {
                    level.setBlock(pos.above(), bb.getStateForAge(i + 1), 2);
                    CommonHooks.fireCropGrowPost(level, pos.above(), blockstate);
                }
            }
        }
    }
}
