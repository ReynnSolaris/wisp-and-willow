package com.reynn.wispwillow.entity.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.entity.WispFamiliarEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * The wisp is made of light: its sheet carries the glow, it is drawn translucent, and it is
 * lit at full bright so the block it happens to be floating over never dims it.
 */
public class WispFamiliarRenderer extends FamiliarGeoRenderer<WispFamiliarEntity> {

    private static final Map<WispFamiliarEntity.Variant, ResourceLocation> TEXTURE_BY_TYPE =
            Util.make(Maps.newEnumMap(WispFamiliarEntity.Variant.class), map -> {
                for (WispFamiliarEntity.Variant variant : WispFamiliarEntity.Variant.values()) {
                    map.put(variant, WWGeoModel.sheet("familiars", "wisp_familiar_" + variant.getName()));
                }
            });

    public WispFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<WispFamiliarEntity>("wisp_familiar", e -> TEXTURE_BY_TYPE.get(e.getVariant()),
                null, 0f, "head", 0.8f) {
            @Override
            public RenderType getRenderType(WispFamiliarEntity wisp, ResourceLocation texture) {
                return RenderType.entityTranslucent(texture);
            }
        }, 0.2f);
    }

    @Override
    public void render(WispFamiliarEntity wisp, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(wisp, yaw, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT);
    }
}
