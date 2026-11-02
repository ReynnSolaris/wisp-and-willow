package com.reynn.wispwillow.registry;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.SimpleTier;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Material tiers for the mod's tools.
 *
 * <p>Empyrite is the first tier Wisp &amp; Willow has ever added, and it deliberately arrives
 * <em>after</em> netherite rather than beside it: it cannot be mined, smelted or traded for,
 * only brewed, so putting it below the top vanilla tier would have made the whole Alchemical
 * Cauldron optional. It is a small step up in every number and a large one in enchantability,
 * because the interesting thing about it is what you can put on it, not what it does bare.</p>
 */
public final class ToolTiers {

    private ToolTiers() {}

    public static final Tier EMPYRITE = new SimpleTier(
            // Nothing is beyond it: it mines whatever netherite mines.
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2_800,          // uses — netherite is 2031
            10.0f,          // mining speed — netherite is 9.0
            4.5f,           // attack damage bonus — netherite is 4.0
            22,             // enchantability — netherite is 15; this is the point of the tier
            () -> Ingredient.of(ItemRegistry.EMPYRITE_INGOT.get())
    );
}
