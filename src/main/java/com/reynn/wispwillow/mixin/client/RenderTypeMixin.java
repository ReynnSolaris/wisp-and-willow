package com.reynn.wispwillow.mixin.client;

import com.reynn.wispwillow.client.AwakenedGlint;
import com.reynn.wispwillow.client.renderers.WWGlintRenderTypes;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands out the gold glint in place of the violet one while an awakened item is being drawn.
 *
 * <p>These five accessors are the single point at which the game decides what a glint looks
 * like — the item renderer, the armour layer and the compass foil buffer all come through
 * here — so substituting at this depth covers every glint an awakened piece can produce,
 * held, worn, dropped or framed, and needs no knowledge of the call sites above.</p>
 *
 * <p>{@link AwakenedGlint#active()} is false outside the brief window an awakened item is on
 * screen, including while the render buffers are being built at startup, so every other
 * glint in the game is returned untouched.</p>
 *
 * <p>This is also what makes an awakened piece <em>override</em> a normal enchantment glint
 * rather than stack with it: vanilla asks for one glint type and gets ours, so the violet
 * one is never drawn at all.</p>
 */
@Mixin(RenderType.class)
public class RenderTypeMixin {
    @Inject(method = "glint", at = @At("HEAD"), cancellable = true)
    private static void wispwillow$glint(CallbackInfoReturnable<RenderType> cir) {
        if (AwakenedGlint.active()) cir.setReturnValue(WWGlintRenderTypes.GLINT);
    }

    @Inject(method = "glintTranslucent", at = @At("HEAD"), cancellable = true)
    private static void wispwillow$glintTranslucent(CallbackInfoReturnable<RenderType> cir) {
        if (AwakenedGlint.active()) cir.setReturnValue(WWGlintRenderTypes.GLINT_TRANSLUCENT);
    }

    @Inject(method = "entityGlint", at = @At("HEAD"), cancellable = true)
    private static void wispwillow$entityGlint(CallbackInfoReturnable<RenderType> cir) {
        if (AwakenedGlint.active()) cir.setReturnValue(WWGlintRenderTypes.ENTITY_GLINT);
    }

    @Inject(method = "entityGlintDirect", at = @At("HEAD"), cancellable = true)
    private static void wispwillow$entityGlintDirect(CallbackInfoReturnable<RenderType> cir) {
        if (AwakenedGlint.active()) cir.setReturnValue(WWGlintRenderTypes.ENTITY_GLINT_DIRECT);
    }

    @Inject(method = "armorEntityGlint", at = @At("HEAD"), cancellable = true)
    private static void wispwillow$armorEntityGlint(CallbackInfoReturnable<RenderType> cir) {
        if (AwakenedGlint.active()) cir.setReturnValue(WWGlintRenderTypes.ARMOR_ENTITY_GLINT);
    }
}
