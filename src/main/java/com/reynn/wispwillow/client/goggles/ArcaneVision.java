package com.reynn.wispwillow.client.goggles;

import com.reynn.wispwillow.items.ArcaneGogglesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

/** Client-side questions about what the local player can currently see. */
public final class ArcaneVision {
    private ArcaneVision() {}

    /** How far the in-world node overlay reaches. */
    public static final int OVERLAY_RANGE = 24;

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !mc.player.isSpectator() && ArcaneGogglesItem.isActive(mc.player);
    }

    /** The block the player is currently looking at, or null. */
    @Nullable
    public static BlockPos lookedAtBlock() {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos();
        }
        return null;
    }
}
