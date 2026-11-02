package com.reynn.wispwillow.entity.client;

import com.google.common.collect.Maps;
import com.reynn.wispwillow.entity.FrogFamiliarEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * One sheet per variant. Unlike the wisp this is not drawn full-bright: a frog is a creature,
 * not a mote of light, so it takes the light of the block it is sitting on.
 */
public class FrogFamiliarRenderer extends FamiliarGeoRenderer<FrogFamiliarEntity> {

    private static final Map<FrogFamiliarEntity.Variant, ResourceLocation> TEXTURE_BY_TYPE =
            Util.make(Maps.newEnumMap(FrogFamiliarEntity.Variant.class), map -> {
                for (FrogFamiliarEntity.Variant variant : FrogFamiliarEntity.Variant.values()) {
                    map.put(variant, WWGeoModel.sheet("familiars", "frog_familiar_" + variant.getName()));
                }
            });

    public FrogFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("frog_familiar", e -> TEXTURE_BY_TYPE.get(e.getVariant()),
                null, 0f, "head", 0.7f), 0.3f);
    }
}
