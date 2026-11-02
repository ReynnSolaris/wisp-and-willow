package com.reynn.wispwillow.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A foundation rune. Laid in complete rings one block below the
 * {@link AltarBlock Willow Altar} to raise its tier.
 *
 * <p>Deliberately a plain block with no block entity: the altar reads these by block
 * state during the scan it already performs, so a large foundation costs nothing to tick.</p>
 */
public class RuneBlock extends Block {
    private final RuneType type;

    public RuneBlock(RuneType type, BlockBehaviour.Properties props) {
        super(props);
        this.type = type;
    }

    public RuneType runeType() {
        return type;
    }
}
