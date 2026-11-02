package com.reynn.wispwillow.entity.client;

import com.google.common.collect.Maps;
import com.reynn.wispwillow.entity.BatFamiliarEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;

/** One sheet per variant; the eyes are on the glow mask so they catch in the dark. */
public class BatFamiliarRenderer extends FamiliarGeoRenderer<BatFamiliarEntity> {

    private static final Map<BatFamiliarEntity.Variant, ResourceLocation> TEXTURE_BY_TYPE =
            Util.make(Maps.newEnumMap(BatFamiliarEntity.Variant.class), map -> {
                for (BatFamiliarEntity.Variant variant : BatFamiliarEntity.Variant.values()) {
                    map.put(variant, WWGeoModel.sheet("familiars", "bat_familiar_" + variant.getName()));
                }
            });

    public BatFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("bat_familiar", e -> TEXTURE_BY_TYPE.get(e.getVariant()),
                null, 0f, "head", 0.5f), 0.25f);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
