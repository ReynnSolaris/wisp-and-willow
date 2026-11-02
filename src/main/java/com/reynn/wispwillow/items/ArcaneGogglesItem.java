package com.reynn.wispwillow.items;

import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Head-slot lenses that let the wearer see Fluxia directly: pool levels, links between
 * nodes, the arcs of a transfer in flight, and a readout for whatever they are looking at.
 *
 * <p>Like most goggles of this kind they also work simply held in a hand, so you are not
 * forced to give up a helmet to inspect a network.</p>
 */
public class ArcaneGogglesItem extends ArmorItem {

    public ArcaneGogglesItem(Holder<ArmorMaterial> material, Properties props) {
        super(material, Type.HELMET, props);
    }

    /** True if this entity is wearing the goggles, or just holding them. */
    public static boolean isActive(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.ARCANE_GOGGLES.get())) return true;
        return entity.getMainHandItem().is(ItemRegistry.ARCANE_GOGGLES.get())
                || entity.getOffhandItem().is(ItemRegistry.ARCANE_GOGGLES.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wispwillow.arcane_goggles.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.arcane_goggles.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.AMETHYST_SHARD) || super.isValidRepairItem(toRepair, repair);
    }
}
