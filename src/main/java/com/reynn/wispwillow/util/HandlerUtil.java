package com.reynn.wispwillow.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class HandlerUtil {
    private HandlerUtil() {}

    public static IItemHandler adjacentItemHandler(Level level, BlockPos pos, Direction toward) {
        BlockPos target = pos.relative(toward);

        // 1) sided capability (face touching back toward us)
        IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, target, toward.getOpposite());
        if (sided != null) {
            DebugLog.foundHandler(level, pos, toward, sided);
            return sided;
        }

        // 2) unsided capability
        IItemHandler any = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
        if (any != null) {
            DebugLog.foundHandler(level, pos, toward, any);
            return any;
        }

        // 3) vanilla container
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof Container c) {
            InvWrapper wrap = new InvWrapper(c);
            DebugLog.foundHandler(level, pos, toward, wrap);
            return wrap;
        }

        DebugLog.foundHandler(level, pos, toward, null);
        return null;
    }
}
