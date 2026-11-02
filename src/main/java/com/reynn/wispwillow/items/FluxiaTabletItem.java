package com.reynn.wispwillow.items;

import com.reynn.wispwillow.fluxia.BottomlessFluxiaStorage;
import com.reynn.wispwillow.fluxia.FluxiaTransfers;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Portable Fluxia storage.
 *
 * <p>Capacity and throughput are per-item now rather than global constants, so the
 * Greater and Grand tablets are the same class with bigger numbers. The stored amount
 * lives in the stack's custom data, which means a tablet's charge travels with it.</p>
 */
public class FluxiaTabletItem extends Item {

    /** Kept for compatibility with existing references to the base tablet's size. */
    public static final int CAPACITY = 20000;
    public static final int IN_RATE = 2000;
    public static final int OUT_RATE = 2000;

    private final int capacity;
    private final int rate;

    public FluxiaTabletItem(Properties props) {
        this(CAPACITY, IN_RATE, props);
    }

    public FluxiaTabletItem(int capacity, int rate, Properties props) {
        super(props);
        this.capacity = capacity;
        this.rate = rate;
    }

    public int capacity() { return capacity; }
    public int rate() { return rate; }

    /**
     * Whether this tablet never runs dry. The creative tablet in the developer kit says
     * yes; {@link #readStorage} then hands out a {@link BottomlessFluxiaStorage} and
     * {@link #writeStorage} keeps nothing, so no caller has to know the difference.
     */
    public boolean bottomless() { return false; }

    // ----- storage helpers (NBT-backed) -----

    public static SimpleFluxiaStorage readStorage(ItemStack stack) {
        if (stack.getItem() instanceof FluxiaTabletItem bottomless && bottomless.bottomless()) {
            return new BottomlessFluxiaStorage(bottomless.rate());
        }
        int cap = CAPACITY, rate = IN_RATE;
        if (stack.getItem() instanceof FluxiaTabletItem tablet) {
            cap = tablet.capacity();
            rate = tablet.rate();
        }
        var s = new SimpleFluxiaStorage(cap, rate, rate, 0);

        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag root = cd.copyTag(); // safe copy
            if (root.contains("Fluxia", Tag.TAG_COMPOUND)) {
                s.load(root.getCompound("Fluxia"));
                // An upgraded tablet keeps its charge but must adopt the new vessel's size.
                s.setMax(cap);
            }
        }
        return s;
    }

    public static void writeStorage(ItemStack stack, SimpleFluxiaStorage s) {
        if (s instanceof BottomlessFluxiaStorage) return;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = (cd != null) ? cd.copyTag() : new CompoundTag();

        CompoundTag fluxiaTag = new CompoundTag();
        s.save(fluxiaTag);
        root.put("Fluxia", fluxiaTag);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static int amount(ItemStack stack) { return readStorage(stack).amount(); }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof FluxiaTabletItem t ? t.capacity() : CAPACITY;
    }

    /** Pulls from the player's own reserve into the tablet on sneak + right-click. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide) {
            var playerFx = player.getData(AttachmentRegistry.FLUXIA.get());
            if (playerFx != null) {
                var tab = readStorage(stack);
                int moved = FluxiaTransfers.move(playerFx, tab, playerFx.amount(), false);
                if (moved > 0) {
                    writeStorage(stack, tab);
                    player.displayClientMessage(
                            Component.translatable("item.wispwillow.tablet.stored", moved), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("item.wispwillow.tablet.empty"), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ----- durability bar shows stored flux -----
    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override public int getBarWidth(ItemStack stack) {
        int cur = amount(stack), cap = capacity(stack);
        return Math.round(13f * cur / Math.max(1, cap));
    }

    @Override public int getBarColor(ItemStack stack) {
        return 0x35C6FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wispwillow.tablet.charge",
                amount(stack), capacity(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.wispwillow.tablet.hint_draw")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wispwillow.tablet.hint_pool")
                .withStyle(ChatFormatting.GRAY));
    }
}
