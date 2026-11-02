package com.reynn.wispwillow.items;

import net.minecraft.world.item.HoneyBottleItem;

/**
 * Tapped willow sap, drunk like honey.
 *
 * <p>All of the drinking behaviour — the animation, the sound, handing back the empty bottle
 * and settling a poisoned stomach — is the honey bottle's, unchanged. What the sap adds is in
 * its food properties: a short spell of Regeneration.</p>
 */
public class WillowSapBottleItem extends HoneyBottleItem {
    public WillowSapBottleItem(Properties properties) {
        super(properties);
    }
}
