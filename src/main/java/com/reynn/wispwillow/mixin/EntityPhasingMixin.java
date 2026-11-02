package com.reynn.wispwillow.mixin;

import com.reynn.wispwillow.phasing.Phasing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Warded blocks still stop a phasing player.
 *
 * <p>With {@code noPhysics} set, {@code Entity#move} adds the movement to the position and
 * returns — no collision of any kind. Trimming the vector on the way in is therefore the
 * one place a ward can be honoured without giving up the flag that makes everything else
 * about phasing work.</p>
 *
 * <p>Both sides run it. The client so the ward feels solid under the player's own hands,
 * and the server because it moves the player itself when validating a movement packet, so
 * this is also what keeps the ward true for a client that has been told otherwise.</p>
 */
@Mixin(Entity.class)
public abstract class EntityPhasingMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 wispwillow$wardsStillCollide(Vec3 movement) {
        Entity self = (Entity) (Object) this;
        // noPhysics first: it is a field read, and it is false for all but a handful of entities.
        if (!self.noPhysics || !(self instanceof Player player) || !Phasing.isPhasing(player)) {
            return movement;
        }
        return Phasing.clampAgainstWards(self, movement);
    }
}
