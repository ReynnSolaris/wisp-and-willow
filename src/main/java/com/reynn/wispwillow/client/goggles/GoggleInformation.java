package com.reynn.wispwillow.client.goggles;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Implemented by block entities that have something to say to a wearer of the
 * {@link com.reynn.wispwillow.items.ArcaneGogglesItem Arcane Goggles}.
 *
 * <p>Called on the <em>client</em> copy of the block entity, so everything reported here
 * must already be part of the block entity's update tag.</p>
 */
public interface GoggleInformation {

    /** Heading for the panel, e.g. the block's own name. */
    Component goggleTitle();

    /** One line per fact. Keep it to a handful; the panel is not a spreadsheet. */
    void addGoggleLines(List<Component> lines);

    /**
     * Optional 0..1 progress bar drawn under the title, or a negative number for none.
     * Typically a fill ratio.
     */
    default float goggleBar() { return -1f; }

    /** Colour of the progress bar, ARGB. */
    default int goggleBarColor() { return 0xFF7FE8FF; }
}
