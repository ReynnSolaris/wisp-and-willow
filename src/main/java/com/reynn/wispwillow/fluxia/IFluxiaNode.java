package com.reynn.wispwillow.fluxia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

public interface IFluxiaNode {
    FluxiaNodeType getFluxRole();
    IFluxiaStorage getFluxStorage();  // may be null for pure RELAY

    /** Can we connect out of this side? (used for adjacency) */
    boolean canConnectFlux(Direction side);

    /** Positions that were explicitly linked via the wand */
    Set<BlockPos> getLinkedNodes();

    /** Called when wand links to another node */
    void addLinkedNode(BlockPos other);

    /** Called when link broken */
    void removeLinkedNode(BlockPos other);

    /** (Optional) per-tick usage hint for networks */
    default int getDesiredFluxPerTick() {
        return 0; // machines will override
    }
}

