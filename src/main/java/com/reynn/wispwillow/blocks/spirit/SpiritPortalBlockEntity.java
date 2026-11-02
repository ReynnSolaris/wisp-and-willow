package com.reynn.wispwillow.blocks.spirit;

import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries no state at all. It exists only so the sheet gets a
 * {@link com.reynn.wispwillow.client.renderers.SpiritPortalRenderer renderer} — the drifting
 * field is drawn, not modelled.
 */
public class SpiritPortalBlockEntity extends BlockEntity {

    public SpiritPortalBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SPIRIT_PORTAL.get(), pos, state);
    }
}
