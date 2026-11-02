package com.reynn.wispwillow.mixin;

import com.reynn.wispwillow.phasing.Phasing;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A phasing player passes through blocks the way a spectator does.
 *
 * <p>{@code noPhysics} is the flag vanilla already routes every part of this through —
 * movement skips collision, suffocation stops asking, and the in-wall overlay is not drawn
 * — so phasing sets that rather than reimplementing any of it.</p>
 *
 * <p>It has to be set here and not from a tick event. {@code Player.tick} assigns
 * {@code noPhysics = isSpectator()} as its first statement, <em>after</em> NeoForge fires
 * the pre-tick event, so anything set beforehand is wiped before the player moves. This
 * injects immediately after that assignment, which is the only point in the tick where the
 * value both survives and still precedes {@code travel}.</p>
 */
@Mixin(Player.class)
public abstract class PlayerPhasingMixin {
    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void wispwillow$phaseThroughBlocks(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (Phasing.isPhasing(self)) {
            self.noPhysics = true;
        }
    }
}
