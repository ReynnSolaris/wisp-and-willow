package com.reynn.wispwillow.compat.jei;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.gateway.GatewayRecipe;
import com.reynn.wispwillow.infusion.InfusionRecipe;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.registry.RecipeRegistry;
import com.reynn.wispwillow.ritual.RitualRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * JEI pages for the three recipe families JEI cannot read on its own: ritual circles, Fluxia
 * infusion and cauldron brews. This package is only loaded by JEI, so the mod runs without it.
 *
 * <p>The recipes come straight from the client's recipe manager, which on 1.21.1 holds
 * everything the server has (the 26.2 build needs a sync cache for this; this one does not).</p>
 */
@JeiPlugin
public final class WispWillowJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new RitualJeiCategory(gui),
                new InfusionJeiCategory(gui),
                new AlchemyJeiCategory(gui),
                new GatewayJeiCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RecipeManager recipes = level.getRecipeManager();
        var rituals = recipes.getAllRecipesFor(RitualRecipe.TYPE);
        var infusions = recipes.getAllRecipesFor(InfusionRecipe.TYPE);
        var brews = recipes.getAllRecipesFor(RecipeRegistry.ALCHEMY_TYPE.get());
        var gateways = recipes.getAllRecipesFor(GatewayRecipe.TYPE);
        registration.addRecipes(RitualJeiCategory.TYPE, rituals);
        registration.addRecipes(InfusionJeiCategory.TYPE, infusions);
        registration.addRecipes(AlchemyJeiCategory.TYPE.get(), brews);
        registration.addRecipes(GatewayJeiCategory.TYPE, gateways);
        // One line per JEI start, so "the page is empty" can be told apart from "nothing arrived".
        WispWillow.LOGGER.info("JEI: {} rituals, {} infusions, {} brews, {} gateway castings",
                rituals.size(), infusions.size(), brews.size(), gateways.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(RitualJeiCategory.TYPE, BlockRegistry.ALTAR.get());
        // Chalk is what you actually spend on a rite, so let it point at the category too.
        for (var chalk : ItemRegistry.CHALK.values()) {
            registration.addRecipeCatalysts(RitualJeiCategory.TYPE, chalk.get());
        }
        registration.addRecipeCatalysts(InfusionJeiCategory.TYPE, BlockRegistry.MANA_POOL.get());
        registration.addRecipeCatalysts(AlchemyJeiCategory.TYPE.get(), BlockRegistry.ALCHEMICAL_CAULDRON.get());
        registration.addRecipeCatalysts(GatewayJeiCategory.TYPE, BlockRegistry.GATEWAY_CORE.get());
        registration.addRecipeCatalysts(GatewayJeiCategory.TYPE, ItemRegistry.SEALED_SOUL_PACT.get());
    }
}
