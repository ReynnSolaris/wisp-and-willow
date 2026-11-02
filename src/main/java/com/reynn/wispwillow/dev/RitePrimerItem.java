package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.blocks.AltarBlockEntity;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.ritual.RitualRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stages whatever rite the altar has recognised: drops its reagents into the circle and
 * fills the altar's reserve, so the next ordinary click performs it.
 *
 * <p>Draw the circle first (the Omni Chalk draws a whole one). Reagents come from the rite's
 * own ingredient list after boon discounts, one stack per unit, so what lands is exactly what
 * the altar is about to look for. Awakening and imbuing rites still want a piece lying in the
 * circle; the primer says so rather than guessing which one.</p>
 */
public class RitePrimerItem extends DevToolItem {

    public RitePrimerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) return InteractionResult.PASS;
        if (!gate(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        altar.forceRecompute();
        RitualRecipe rite = altar.getCachedMatch();
        if (rite == null) {
            chat(player, head("Primer", warn("no rite is drawn here; chalk a circle first")));
            return InteractionResult.FAIL;
        }
        if (altar.hasVigil()) {
            chat(player, head("Primer", warn("the altar is keeping a vigil; sneak-click it to end that first")));
            return InteractionResult.FAIL;
        }

        Vec3 drop = new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
        int dropped = 0;
        for (Ingredient ingredient : altar.effectiveInputs(rite)) {
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) continue;
            ItemStack one = options[0].copyWithCount(1);
            ItemEntity entity = new ItemEntity(server, drop.x, drop.y, drop.z, one);
            entity.setDeltaMovement(Vec3.ZERO);
            // Long enough that the tester standing on the altar does not pocket the reagents
            // before the rite can take them.
            entity.setPickUpDelay(60);
            server.addFreshEntity(entity);
            dropped++;
        }

        int cost = altar.effectiveCost(rite.fluxiaRequired().orElse(0));
        if (altar.getFluxStorage() instanceof SimpleFluxiaStorage tank) tank.setFlux(tank.capacity());
        altar.setChanged();
        altar.pushClient();

        chat(player, head("Primer", Component.translatable(rite.getId().toLanguageKey())));
        chat(player, kv("Reagents", dropped == 0 ? "none needed" : dropped + " dropped into the circle"));
        chat(player, kv("Fluxia", "filled; the rite costs " + GoggleText.num(cost) + " f"));
        int missing = altar.getMissing().size();
        if (missing > 0) chat(player, kv("Glyphs", warn(missing + " still missing; the altar will refuse")));
        if (altar.getFoundation().tier() < rite.minTier()) {
            chat(player, kv("Tier", warn("needs " + rite.minTier() + ", foundation is " + altar.getFoundation().tier())));
        }
        if (rite.isAwakening() || rite.isImbuing()) {
            chat(player, kv("Target", "drop the Empyrite piece to " + (rite.isAwakening() ? "awaken" : "imbue") + " in the circle"));
        }
        chat(player, kv("Next", good("right-click the altar")));

        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.0f);
        return InteractionResult.SUCCESS;
    }
}
