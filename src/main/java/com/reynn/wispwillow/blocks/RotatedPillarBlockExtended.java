package com.reynn.wispwillow.blocks;

import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RotatedPillarBlockExtended extends RotatedPillarBlock {
    public RotatedPillarBlockExtended(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Tapping: a glass bottle takes the sap and leaves the log stripped.
        if (stack.is(Items.GLASS_BOTTLE) && state.is(BlockRegistry.WILLOW_LOG.get())) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, BlockRegistry.STRIPPED_WILLOW_LOG.get().defaultBlockState()
                        .setValue(AXIS, state.getValue(AXIS)));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                        new ItemStack(ItemRegistry.WILLOW_SAP_BOTTLE.get())));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** Axe stripping for every axis, not just upright logs; vanilla's own axe hook does the rest. */
    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP && state.is(BlockRegistry.WILLOW_LOG.get())) {
            return BlockRegistry.STRIPPED_WILLOW_LOG.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
