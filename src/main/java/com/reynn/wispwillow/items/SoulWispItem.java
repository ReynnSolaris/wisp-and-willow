package com.reynn.wispwillow.items;

import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The only thing that will send a familiar away.
 *
 * <p>Familiars are otherwise deathless and permanent, which is the point — but that also
 * means there has to be a deliberate, unmistakable way to remove one. Touching a familiar
 * with a Soul Wisp begins a slow dissolve rather than killing it outright.</p>
 */
public class SoulWispItem extends Item {

    public SoulWispItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (!(target instanceof FamiliarEntity familiar)) return InteractionResult.PASS;
        if (familiar.isBanishing()) return InteractionResult.PASS;

        if (!player.level().isClientSide) {
            familiar.beginBanish();
            player.displayClientMessage(
                    Component.translatable("item.wispwillow.soul_wisp.released",
                            familiar.getDisplayName()), true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wispwillow.soul_wisp.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.soul_wisp.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
