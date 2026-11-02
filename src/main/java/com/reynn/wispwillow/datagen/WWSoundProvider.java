package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.SoundRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class WWSoundProvider extends SoundDefinitionsProvider {
    protected WWSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, WispWillow.MODID, helper);
    }

    @Override
    public void registerSounds() {
        add(SoundRegistry.ITEM_INFUSION, SoundDefinition.definition()
                // Add sound objects to the sound definition. Parameter is a vararg.
                .with(
                        // Accepts either a string or a ResourceLocation as the first parameter.
                        // The second parameter can be either SOUND or EVENT, and can be omitted if the former.
                        sound(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "infusion"), SoundDefinition.SoundType.SOUND)
                                .volume(0.8f)
                                .pitch(1.2f)
                                .weight(2)
                                .attenuationDistance(8)

                                .stream(true)

                                .preload(true)
                )
                // Sets the subtitle.
                .subtitle("sound."+WispWillow.MODID+".infusion")
                // Enables replacing.
                .replace(true)
        );
    }
}
