package com.reynn.wispwillow.entity.client;

import com.google.common.collect.Maps;
import com.reynn.wispwillow.entity.AxolotlFamiliarEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;

/** One sheet per gemstone variant, the gem on the brow on each one's glow mask. */
public class AxolotlFamiliarRenderer extends FamiliarGeoRenderer<AxolotlFamiliarEntity> {

    private static final Map<AxolotlFamiliarEntity.Variant, ResourceLocation> TEXTURE_BY_TYPE =
            Util.make(Maps.newEnumMap(AxolotlFamiliarEntity.Variant.class), map -> {
                for (AxolotlFamiliarEntity.Variant variant : AxolotlFamiliarEntity.Variant.values()) {
                    map.put(variant, WWGeoModel.sheet("familiars", "axolotl_familiar_" + variant.getName()));
                }
            });

    public AxolotlFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("axolotl_familiar", e -> TEXTURE_BY_TYPE.get(e.getVariant()),
                null, 0f, "head", 0.6f), 0.35f);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
