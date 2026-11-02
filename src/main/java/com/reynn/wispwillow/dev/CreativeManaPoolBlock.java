package com.reynn.wispwillow.dev;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.blocks.ManaPoolBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Fluxia pool that is never empty. Creative-tab only.
 *
 * <p>Same block entity, same renderer, same links and the same bucket interactions as the
 * ordinary pool: the one difference is that {@code ManaPoolBlockEntity} gives it a
 * {@link com.reynn.wispwillow.fluxia.BottomlessFluxiaStorage} when it sees this block in the
 * state it is built for, and lets it feed every target at the target's own intake rate rather
 * than the pool's 800 ƒ/s. That makes it the quickest way to put a machine on full power and
 * watch what it does.</p>
 */
public class CreativeManaPoolBlock extends ManaPoolBlock {
    public static final MapCodec<CreativeManaPoolBlock> CODEC = simpleCodec(CreativeManaPoolBlock::new);

    public CreativeManaPoolBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static boolean is(BlockState state) {
        return state.getBlock() instanceof CreativeManaPoolBlock;
    }
}
