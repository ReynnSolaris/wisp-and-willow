package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries no state at all. It exists only so the portal sheet gets a
 * {@link com.reynn.wispwillow.client.renderers.GatewayPortalRenderer renderer} — the moving
 * starfield is drawn, not modelled, exactly as vanilla does for an end portal.
 */
public class GatewayPortalBlockEntity extends BlockEntity {

    public GatewayPortalBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GATEWAY_PORTAL.get(), pos, state);
    }
}
