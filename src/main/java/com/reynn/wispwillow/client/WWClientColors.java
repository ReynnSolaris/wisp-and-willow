package com.reynn.wispwillow.client;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.items.ChalkItem;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = WispWillow.MODID,value = Dist.CLIENT)
public final class WWClientColors {
    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block e) {
        e.getBlockColors().register((state, lvl, pos, tintIndex) -> {
            if (tintIndex < 0 || state == null || !state.hasProperty(SigilBlock.COLOR)) return 0xFFFFFFFF;
            DyeColor c = state.getValue(SigilBlock.COLOR);
            return 0xFF000000 | c.getTextColor();
        }, BlockRegistry.SIGIL.get());
    }

    /** How long the Omni Chalk takes to walk the whole hue circle, in milliseconds. */
    private static final float RAINBOW_PERIOD = 2600f;

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item e) {
        // Every chalk shares one greyscale sprite and is tinted by its dye; the omni stick
        // is the same sprite tinted by the clock instead, which is the whole rainbow for
        // the price of no new art. Wall time rather than game time so it keeps moving in
        // an inventory, where nothing is ticking.
        e.getItemColors().register((stack, layer) -> {
            if (layer != 0) return 0xFFFFFFFF;
            float hue = (System.currentTimeMillis() % (long) RAINBOW_PERIOD) / RAINBOW_PERIOD;
            return 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(hue, 0.85f, 1.0f);
        }, ItemRegistry.OMNI_CHALK.get());

        ItemRegistry.CHALK.values().forEach(ro ->
                e.getItemColors().register((stack, layer) -> {
                    if (layer != 0) return 0xFFFFFFFF;
                    if (stack.getItem() instanceof ChalkItem ci) {
                        return 0xFF000000 | ci.getColor().getTextColor();
                    }
                    return 0xFFFFFFFF;
                }, ro.get())
        );
        e.register((stack, tintIndex) -> {
                    if (tintIndex == 1) {
                        return -1;
                    } else {
                        return e.getItemColors().getColor(new ItemStack(Items.BUCKET), 0);
                    }
                },
                ItemRegistry.FLUXIA_FLUID_BUCKET.get()
        );
    }
}
