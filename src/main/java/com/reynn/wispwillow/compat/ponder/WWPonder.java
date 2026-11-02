package com.reynn.wispwillow.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;

/**
 * The one class the client calls to switch the Ponder integration on.
 *
 * <p>Only reached once Ponder is known to be loaded, so neither this nor anything it touches
 * is ever loaded without it.</p>
 */
public final class WWPonder {
    private WWPonder() {}

    public static void register() {
        PonderIndex.addPlugin(new WWPonderPlugin());
    }
}
