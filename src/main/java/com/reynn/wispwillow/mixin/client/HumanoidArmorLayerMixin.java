package com.reynn.wispwillow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.client.AwakenedGlint;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Announces the armour piece being drawn so {@link RenderTypeMixin} can pick its glint.
 *
 * <p>Worn armour never goes through the item renderer — the armour layer draws it straight
 * from the humanoid model — so without this an awakened chestplate would glint gold in the
 * inventory and violet on the wearer.</p>
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
    private static final String RENDER_PIECE = "renderArmorPiece("
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/entity/EquipmentSlot;I"
            + "Lnet/minecraft/client/model/HumanoidModel;FFFFFF)V";

    @Inject(method = RENDER_PIECE, at = @At("HEAD"))
    private void wispwillow$pushGlint(PoseStack poseStack, MultiBufferSource bufferSource,
                                      LivingEntity entity, EquipmentSlot slot, int packedLight,
                                      HumanoidModel<?> model, float limbSwing, float limbSwingAmount,
                                      float partialTick, float ageInTicks, float netHeadYaw,
                                      float headPitch, CallbackInfo ci) {
        AwakenedGlint.push(entity.getItemBySlot(slot));
    }

    @Inject(method = RENDER_PIECE, at = @At("RETURN"))
    private void wispwillow$popGlint(PoseStack poseStack, MultiBufferSource bufferSource,
                                     LivingEntity entity, EquipmentSlot slot, int packedLight,
                                     HumanoidModel<?> model, float limbSwing, float limbSwingAmount,
                                     float partialTick, float ageInTicks, float netHeadYaw,
                                     float headPitch, CallbackInfo ci) {
        AwakenedGlint.pop();
    }
}
