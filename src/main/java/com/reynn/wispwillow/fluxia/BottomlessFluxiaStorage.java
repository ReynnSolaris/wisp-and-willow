package com.reynn.wispwillow.fluxia;

import net.minecraft.nbt.CompoundTag;

/**
 * A reserve that never runs dry and never fills up: the creative-mode counterpart of
 * {@link SimpleFluxiaStorage}, for the Creative Fluxia Pool and the Creative Fluxia Tablet.
 *
 * <p>It reports itself permanently full, so nothing in the network ever tries to push into it,
 * and hands out whatever is asked of it (up to its per-tick rate) without the amount moving.
 * Every setter is a no-op: a save written by one of these loads back as the same bottomless
 * thing, and a tool that tries to drain it finds there is nothing to drain.</p>
 */
public class BottomlessFluxiaStorage extends SimpleFluxiaStorage {

    /**
     * Shown as the capacity. Large enough that no machine in the mod could empty it in a
     * session, small enough that any sum of it and a real tank still fits in an int.
     */
    public static final int CAPACITY = 1_000_000_000;

    /** @param perTick how much it will hand over in one tick; the receiver's own rate still applies */
    public BottomlessFluxiaStorage(int perTick) {
        super(CAPACITY, perTick, perTick, 0);
        this.flux = CAPACITY;
    }

    /** Always full, so nothing is ever accepted. */
    @Override public int insert(int amount, boolean simulate) { return 0; }

    /** Hands over up to the per-tick rate, and stays full. */
    @Override public int extract(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        return Math.min(outRate, amount);
    }

    @Override public int amount() { return max; }

    @Override public void set(int v) {}
    @Override public void setFlux(int flux) {}
    @Override public void setMax(int max) {}
    @Override public void setRegen(int regen) {}
    @Override public void load(CompoundTag tag) {}
}
