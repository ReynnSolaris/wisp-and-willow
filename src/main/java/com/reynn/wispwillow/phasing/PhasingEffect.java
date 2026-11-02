package com.reynn.wispwillow.phasing;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Phasing: while it lasts, stone is not in your way.
 *
 * <p>The effect itself does nothing — it is a flag with a duration and an icon. Everything
 * it means is read off it elsewhere: {@link Phasing} grants and ends it and drives the
 * drift, and the two mixins turn it into {@code noPhysics} and into the one thing that can
 * still stop you. Keeping the behaviour out of here is what lets the client act on the
 * effect the same tick it arrives, without waiting to be told what it does.</p>
 *
 * <p>Neutral rather than beneficial: it is how you reach the places Fluxia hides, and it
 * is also how you end up inside a wall when it runs out.</p>
 */
public class PhasingEffect extends MobEffect {
    public PhasingEffect() {
        super(MobEffectCategory.NEUTRAL, 0x7FE8FF);
    }
}
