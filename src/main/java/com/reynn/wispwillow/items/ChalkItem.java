package com.reynn.wispwillow.items;

import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ChalkItem extends Item {
    private final DyeColor color;

    public ChalkItem(Properties props, DyeColor color) {
        super(props.durability(128));
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();

        // 1) If clicking an existing sigil: cycle its TYPE (shift = reverse)
        BlockState target = level.getBlockState(clicked);
        if (target.getBlock() == BlockRegistry.SIGIL.get()) {
            if (!level.isClientSide) {
                SigilType cur = target.getValue(SigilBlock.TYPE);
                SigilType next = nextType(cur, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown());
                BlockState newState = target
                        .setValue(SigilBlock.TYPE, next)
                        .setValue(SigilBlock.COLOR, color); // also recolor to this chalk
                level.setBlock(clicked, newState, 3);
                level.sendBlockUpdated(clicked, target, newState, 3);
                damageChalk(ctx);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2) Place only on TOP of a non-air block
        if (face != Direction.UP) return InteractionResult.FAIL;
        if (target.isAir()) return InteractionResult.FAIL;

        BlockPos placePos = clicked.above();
        if (!level.getBlockState(placePos).isAir()) return InteractionResult.FAIL;

        if (!level.isClientSide) {
            BlockState newState = BlockRegistry.SIGIL.get().defaultBlockState()
                    .setValue(SigilBlock.COLOR, color)
                    .setValue(SigilBlock.TYPE, SigilType.BASE);

            var old = level.getBlockState(placePos);
            boolean placed = level.setBlock(placePos, newState, 3);
            if (placed) level.sendBlockUpdated(placePos, old, newState, 3);
            damageChalk(ctx);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static SigilType nextType(SigilType cur, boolean reverse) {
        SigilType[] vals = SigilType.values();
        int i = cur.ordinal();
        return vals[(i + (reverse ? vals.length - 1 : 1)) % vals.length];
    }

    private static void damageChalk(UseOnContext ctx) {
        var player = ctx.getPlayer();
        if (player != null) {
            var slot = (ctx.getHand() == InteractionHand.MAIN_HAND)
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            ctx.getItemInHand().hurtAndBreak(1, player, slot);
        }
    }
}
