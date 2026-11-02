package com.reynn.wispwillow.client;

import com.reynn.wispwillow.phasing.Phasing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/**
 * The client's half of phasing: how you drift, and what it looks like from inside a rock.
 *
 * <p>The drift is steered here because the jump key is here. A player's movement is decided
 * on their own machine and the server takes it, so setting the velocity anywhere else would
 * only fight the client for it.</p>
 */
// Registered by hand from WispWillowClient rather than by annotation: the drift is the one
// part of phasing with no server-side fallback, so it registers the same explicit way the
// mod's other game-bus handlers do instead of depending on annotation scanning.
public final class PhasingClient {
    private PhasingClient() {}

    /** Fog while embedded in stone, in blocks. Close enough to be a haze, not a window. */
    private static final float FOG_NEAR = 0.1f;
    private static final float FOG_FAR = 4.5f;
    private static final Vector3f FOG_COLOR = new Vector3f(0.09f, 0.15f, 0.24f);

    /**
     * Sinking is the default and the whole point; jump pulls you back up, sneak drops you
     * faster. Vertical control is total, horizontal control is what little the fluid or the
     * stone allows, which is what keeps this a way down and not a way through someone's wall.
     */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!Phasing.isPhasing(player)
                || player.isSpectator()
                || player.getAbilities().flying
                || player.isPassenger()) {
            return;
        }

        boolean inFluxia = Phasing.touchingFluxia(player);
        double drift;
        if (player.input.jumping) {
            drift = Phasing.RISE;
        } else if (player.isShiftKeyDown()) {
            drift = -Phasing.DIVE;
        } else {
            drift = inFluxia ? -Phasing.DRIFT_IN_FLUXIA : -Phasing.DRIFT_IN_STONE;
        }

        // Bedrock is warded, but the bottom of the world is not a block — stop short of it
        // by hand, or a deep enough pool would drift someone out of the world entirely.
        if (drift < 0 && player.getY() + drift < player.level().getMinBuildHeight() + 1) {
            drift = 0;
        }

        double drag = inFluxia ? 1.0 : Phasing.STONE_DRAG;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x * drag, drift, velocity.z * drag);
        player.setOnGround(false);
        player.resetFallDistance();
    }

    // ------------------------------------------------------------------
    // Inside the stone
    // ------------------------------------------------------------------

    /**
     * Vanilla draws the in-wall overlay only for players who are <em>not</em> {@code noPhysics},
     * so a phasing player would otherwise see out through the rock as a spectator does. Close
     * fog puts the rock back: it reads as being inside something, and it is not an x-ray.
     */
    @SubscribeEvent
    static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!embedded()) return;
        event.setNearPlaneDistance(FOG_NEAR);
        event.setFarPlaneDistance(FOG_FAR);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!embedded()) return;
        event.setRed(FOG_COLOR.x());
        event.setGreen(FOG_COLOR.y());
        event.setBlue(FOG_COLOR.z());
    }

    private static boolean embedded() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && Phasing.isPhasing(player) && Phasing.insideBlocks(player);
    }
}
