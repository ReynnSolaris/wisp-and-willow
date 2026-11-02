package com.reynn.wispwillow.mixin;

import com.reynn.wispwillow.worldgen.fen.WillowFenClimate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Puts the Willow Fen on the overworld's map of climates.
 *
 * <p>NeoForge 21.1 has no API for adding a biome to the overworld: this list is built in vanilla,
 * and a biome that is not in it can never generate. TerraBlender exists to solve that, and taking
 * it as a dependency would mean every player had to install it, so the mod does the one thing it
 * needs by hand instead.</p>
 *
 * <p>The hook takes the finished list rather than appending to it, because appending does not
 * work — see {@link WillowFenClimate} for why a box placed inside vanilla's swamp can never win.
 * Every other entry is passed through untouched. A mod that routes the overworld through
 * TerraBlender replaces this list wholesale; the fen would then be absent rather than broken.</p>
 */
@Mixin(MultiNoiseBiomeSourceParameterList.Preset.class)
public class OverworldPresetMixin {

    @Inject(method = "generateOverworldBiomes", at = @At("RETURN"), cancellable = true)
    private static <T> void wispwillow$claimWettestSwamps(Function<ResourceKey<Biome>, T> valueGetter,
                                                          CallbackInfoReturnable<Climate.ParameterList<T>> cir) {
        if (WillowFenClimate.buildingVanillaRegistries()) return;
        cir.setReturnValue(WillowFenClimate.claimWettestSwamps(cir.getReturnValue(), valueGetter));
    }
}
