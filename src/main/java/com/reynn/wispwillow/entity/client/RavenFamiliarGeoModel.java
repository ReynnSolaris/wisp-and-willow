package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.entity.RavenFamiliarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The raven's GeckoLib model: {@code geo/entity/raven_familiar.geo.json} with its animations
 * beside it, and the head bone tracking the entity's look.
 *
 * <p>The texture is the one thing the defaulted paths get wrong: every familiar's skin lives
 * under {@code textures/entity/familiars/}, not straight under {@code textures/entity/}.</p>
 */
public class RavenFamiliarGeoModel extends DefaultedEntityGeoModel<RavenFamiliarEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WispWillow.MODID, "textures/entity/familiars/raven_familiar.png");

    public RavenFamiliarGeoModel() {
        // true: the geo has a bone literally named "head", so let it follow the look vector.
        super(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "raven_familiar"), true);
    }

    @Override
    public ResourceLocation getTextureResource(RavenFamiliarEntity raven) {
        return TEXTURE;
    }
}
