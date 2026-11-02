package com.reynn.wispwillow.mixin;

import com.reynn.wispwillow.items.Awakening;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Awakened empyrite glints whether or not it is enchanted.
 *
 * <p>Awakening is a component rather than an enchantment, so nothing in vanilla would give
 * an awakened-but-unenchanted piece a glint at all. This is the funnel every glint decision
 * passes through — the item renderer and the armour layer both ask {@code hasFoil()} before
 * they draw one — so answering it here is enough for all of them.</p>
 *
 * <p>Only ever widens the answer: an item that already had a foil keeps it.</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void wispwillow$awakenedAlwaysGlints(CallbackInfoReturnable<Boolean> cir) {
        if (Awakening.isAwakened((ItemStack) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
