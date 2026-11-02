package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockModelGenerator extends BlockModelProvider {
    public BlockModelGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WispWillow.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

    }
}
