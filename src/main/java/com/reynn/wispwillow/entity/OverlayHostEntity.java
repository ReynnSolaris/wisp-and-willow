package com.reynn.wispwillow.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The invisible carrier for the Fluxia currents and the goggle overlay.
 *
 * <p>There is one of these per client, spawned client-side only and kept on the player
 * ({@code ArcaneOverlayRenderer} owns it); the server never sees it, it is never saved
 * and it cannot be summoned. Its whole job is to have a renderer, because an entity
 * renderer is a path every shader pack draws: the beams and gauges used to be drawn from
 * a level-render stage, and under a pack that geometry was composited away, while the
 * portals, drawn by block entity renderers in the same stock render types, survived.
 * Drawing from an entity puts the overlays on the same footing.</p>
 */
public class OverlayHostEntity extends Entity {

    /** Fixed id, far from anything the server hands out, so it never shadows a real entity. */
    public static final int CLIENT_ID = -0x7E57A5;

    public OverlayHostEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setId(CLIENT_ID);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    /** Nothing to simulate: the overlay code moves it, and the base tick would try to fall. */
    @Override public void tick() {}

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldRender(double camX, double camY, double camZ) { return true; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
}
