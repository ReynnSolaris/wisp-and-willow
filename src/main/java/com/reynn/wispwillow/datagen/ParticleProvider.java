package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.ParticleRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

public class ParticleProvider extends ParticleDescriptionProvider {
    protected ParticleProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        sprite(ParticleRegistry.FLUX_SPARK.get(), ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "test_particle"));
    }
}
