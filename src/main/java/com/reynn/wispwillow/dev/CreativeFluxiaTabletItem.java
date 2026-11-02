package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.fluxia.BottomlessFluxiaStorage;
import com.reynn.wispwillow.items.FluxiaTabletItem;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A tablet that is never empty. Creative-tab only.
 *
 * <p>Dropped into a pool it charges the pool at the pool's own pace and is still full when
 * it is picked back up. Where the ordinary tablet draws Fluxia out of the player on
 * sneak-use, this one pours it in: sneak-use fills the player's own reserve, which is the
 * quickest way to test anything that spends it.</p>
 *
 * <p>The bottomless behaviour lives in {@link FluxiaTabletItem#readStorage}, keyed on
 * {@link #bottomless()}, so every path that reads a tablet (the pool, the tooltip, the bar)
 * sees the same thing without knowing this class exists.</p>
 */
public class CreativeFluxiaTabletItem extends FluxiaTabletItem {

    public CreativeFluxiaTabletItem(Properties props) {
        super(BottomlessFluxiaStorage.CAPACITY, BottomlessFluxiaStorage.CAPACITY, props);
    }

    @Override
    public boolean bottomless() {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!DevToolItem.gate(player)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            var reserve = player.getData(AttachmentRegistry.FLUXIA.get());
            if (reserve != null) {
                // The reserve syncs itself to the HUD once a second; nothing to send here.
                reserve.set(reserve.capacity());
                player.displayClientMessage(Component.translatable(
                        "item.wispwillow.creative_fluxia_tablet.filled", reserve.capacity()), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD070F0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wispwillow.creative_fluxia_tablet.charge")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.wispwillow.creative_fluxia_tablet.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.creative_fluxia_tablet.sneak")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.dev_tool")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
