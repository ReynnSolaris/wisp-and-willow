package com.reynn.wispwillow.client.menu;

import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlockEntity;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AlchemicalCauldronMenu extends AbstractContainerMenu {

    /** Button ids sent by clickMenuButton. Using vanilla's button channel avoids a custom packet. */
    public static final int BUTTON_DUMP_INPUT = 0;
    public static final int BUTTON_DUMP_OUTPUT = 1;
    public static final int BUTTON_RECIRCULATE = 2;

    private static final int INPUT_START = 0;
    private static final int INPUT_END = AlchemicalCauldronBlockEntity.INPUT_SLOTS;
    private static final int OUTPUT_END = INPUT_END + AlchemicalCauldronBlockEntity.OUTPUT_SLOTS;
    private static final int UPGRADE_END = OUTPUT_END + AlchemicalCauldronBlockEntity.UPGRADE_SLOTS;
    private static final int PLAYER_END = UPGRADE_END + 36;

    private final AlchemicalCauldronBlockEntity cauldron;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client constructor. */
    public AlchemicalCauldronMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()),
                new SimpleContainerData(AlchemicalCauldronBlockEntity.DATA_COUNT));
    }

    public AlchemicalCauldronMenu(int id, Inventory inv,
                                  AlchemicalCauldronBlockEntity cauldron, ContainerData data) {
        super(MainRegistry.ALCHEMICAL_CAULDRON_MENU.get(), id);
        this.cauldron = cauldron;
        this.data = data;
        this.access = ContainerLevelAccess.create(cauldron.getLevel(), cauldron.getBlockPos());

        // Solid inputs: a 2x2 cluster, laid out to read as "things going into the pot".
        int k = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new SlotItemHandler(cauldron.inputs(), k++, 44 + col * 18, 26 + row * 18));
            }
        }

        // Outputs are take-only; nothing the player inserts here would ever be read.
        for (int i = 0; i < AlchemicalCauldronBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(cauldron.outputs(), i, 116 + i * 18, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // Upgrade tray, down the right of the machine area, away from the reagent flow.
        for (int i = 0; i < AlchemicalCauldronBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new SlotItemHandler(cauldron.upgrades(), i, 62 + i * 18, 73) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return AlchemicalCauldronBlockEntity.isUpgrade(stack);
                }
                @Override public int getMaxStackSize() { return 16; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    private static AlchemicalCauldronBlockEntity resolve(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof AlchemicalCauldronBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No alchemical cauldron at " + pos);
    }

    public AlchemicalCauldronBlockEntity cauldron() { return cauldron; }

    public int progress()   { return data.get(AlchemicalCauldronBlockEntity.DATA_PROGRESS); }
    public int duration()   { return data.get(AlchemicalCauldronBlockEntity.DATA_DURATION); }
    public int fluxia()     { return data.get(AlchemicalCauldronBlockEntity.DATA_FLUXIA); }
    public int fluxiaMax()  { return data.get(AlchemicalCauldronBlockEntity.DATA_FLUXIA_MAX); }

    /** 0..1, clamped. Returns 0 when idle so the arrow does not stick full between brews. */
    public float progressFraction() {
        int total = duration();
        if (total <= 0) return 0f;
        return Math.min(1f, progress() / (float) total);
    }

    public float fluxiaFraction() {
        int max = fluxiaMax();
        return max <= 0 ? 0f : Math.min(1f, fluxia() / (float) max);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BUTTON_DUMP_INPUT  -> { cauldron.dumpTank(false); yield true; }
            case BUTTON_DUMP_OUTPUT -> { cauldron.dumpTank(true);  yield true; }
            case BUTTON_RECIRCULATE -> { cauldron.recirculate();   yield true; }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();

        if (index < UPGRADE_END) {
            // Out of the machine, into the player.
            if (!moveItemStackTo(inSlot, UPGRADE_END, PLAYER_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(inSlot, copy);
        } else if (AlchemicalCauldronBlockEntity.isUpgrade(inSlot)) {
            // An upgrade shift-clicked from the player's inventory belongs in the tray, not
            // in the pot, however much it looks like a reagent.
            if (!moveItemStackTo(inSlot, OUTPUT_END, UPGRADE_END, false)) return ItemStack.EMPTY;
        } else {
            // Into the machine. Only the input slots ever accept a shift-click.
            if (!moveItemStackTo(inSlot, INPUT_START, INPUT_END, false)) return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (inSlot.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, inSlot);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BlockRegistry.ALCHEMICAL_CAULDRON.get());
    }
}
