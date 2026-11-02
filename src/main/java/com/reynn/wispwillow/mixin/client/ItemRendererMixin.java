package com.reynn.wispwillow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.client.AwakenedGlint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Announces the stack being drawn so {@link RenderTypeMixin} can pick its glint.
 *
 * <p>Covers every way an item reaches the screen through the item renderer: inventory
 * slots, both hands, dropped stacks, item frames and armour stands.</p>
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;Z"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;II"
            + "Lnet/minecraft/client/resources/model/BakedModel;)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void wispwillow$pushGlint(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                                      PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                      int overlay, BakedModel model, CallbackInfo ci) {
        AwakenedGlint.push(stack);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void wispwillow$popGlint(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                                     PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                     int overlay, BakedModel model, CallbackInfo ci) {
        AwakenedGlint.pop();
    }
}
