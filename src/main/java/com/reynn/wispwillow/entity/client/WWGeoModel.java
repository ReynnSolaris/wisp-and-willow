package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.function.Function;

/**
 * The GeckoLib model shared by every generated animal and familiar: {@code geo/entity/<name>.geo.json}
 * with its animations beside it, a texture chosen per entity (the familiars have colour variants),
 * and look-tracking split between a neck bone and a head bone.
 *
 * <p>The tracking is <em>added</em> to whatever the animation left on those bones rather than
 * replacing it, which is what GeckoLib's own defaulted model does; replacing it would freeze every
 * head bob and neck sway in the idle cycles the moment the mob looked at something.</p>
 *
 * <p>Sheets live under {@code textures/entity/<folder>/}, so the defaulted texture path is always
 * overridden.</p>
 */
public class WWGeoModel<T extends LivingEntity & GeoAnimatable> extends DefaultedEntityGeoModel<T> {

    private final Function<T, ResourceLocation> texture;
    private final String neckBone;
    private final float neckShare;
    private final String headBone;
    private final float headShare;

    /**
     * @param name      the geo / animation file stem
     * @param texture   picks the sheet for an entity
     * @param neckBone  bone that takes {@code neckShare} of the look, or null
     * @param headBone  bone that takes {@code headShare} of the look, or null
     */
    public WWGeoModel(String name, Function<T, ResourceLocation> texture,
                      String neckBone, float neckShare, String headBone, float headShare) {
        super(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name), false);
        this.texture = texture;
        this.neckBone = neckBone;
        this.neckShare = neckShare;
        this.headBone = headBone;
        this.headShare = headShare;
    }

    /** A single sheet at {@code textures/entity/<folder>/<file>.png}. */
    public static ResourceLocation sheet(String folder, String file) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                "textures/entity/" + folder + "/" + file + ".png");
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return texture.apply(entity);
    }

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> state) {
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;
        track(neckBone, neckShare, data);
        track(headBone, headShare, data);
    }

    private void track(String bone, float share, EntityModelData data) {
        if (bone == null || share == 0f) return;
        GeoBone b = getAnimationProcessor().getBone(bone);
        if (b == null) return;
        b.setRotX(b.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * share);
        b.setRotY(b.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD * share);
    }

    /** Hide or show a bone (and everything under it), for parts that come and go like the fawn's coat. */
    protected void setHidden(String bone, boolean hidden) {
        GeoBone b = getAnimationProcessor().getBone(bone);
        if (b != null) b.setHidden(hidden);
    }
}
