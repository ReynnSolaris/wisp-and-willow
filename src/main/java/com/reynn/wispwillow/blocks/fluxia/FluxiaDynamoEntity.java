package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Burns fuel for Fluxia, hard.
 *
 * <p>Several times a Luxinias flower, and it will eat anything a furnace would, so a coal
 * farm or a tree farm feeds it. This is the workhorse that makes a Soul Gateway sustainable:
 * one dynamo running steadily covers the gateway's standing upkeep with room to spare.</p>
 */
public class FluxiaDynamoEntity extends AbstractFluxiaGenerator {

    public static final int FLUXIA_PER_SECOND = 300;
    private static final int CAPACITY = 60_000;
    private static final int THROUGHPUT = 1_000;

    private static final String BURN_TAG = "BurnTime";

    /** Ticks of fuel left. */
    private int burnTime;

    public FluxiaDynamoEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FLUXIA_DYNAMO.get(), pos, state, CAPACITY, THROUGHPUT, THROUGHPUT);
    }

    @Override
    public boolean isRunning() { return burnTime > 0; }

    @Override
    protected int currentRate() { return FLUXIA_PER_SECOND; }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (burnTime > 0) {
            burnTime--;
            if (burnTime == 0) setLit(level, pos, state, false);
            return;
        }
        // Only look for fuel four times a second, and never while already full.
        if (level.getGameTime() % 5 != 0) return;
        if (tank.amount() >= tank.capacity()) return;
        tryEatFuel(level, pos, state);
    }

    @Override
    protected int generatePerSecond(ServerLevel level, BlockPos pos, BlockState state) {
        return burnTime > 0 ? FLUXIA_PER_SECOND : 0;
    }

    /** Swallows one item from the pile sitting on top of the dynamo. */
    private void tryEatFuel(ServerLevel level, BlockPos pos, BlockState state) {
        AABB box = new AABB(pos).inflate(0.5, 0.75, 0.5).move(0, 0.5, 0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box, e -> !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();

            // NeoForge's burn time, so the mod's own furnace-fuel data map is honoured and
            // Amethyst Coal burns for as long here as it does in a furnace.
            int fuel = stack.getBurnTime(null);
            if (fuel <= 0) continue;

            stack.shrink(1);
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);

            burnTime = fuel;
            setLit(level, pos, state, true);
            level.levelEvent(1004, pos, 0); // fire charge "whoosh"
            setChanged();
            return;
        }
    }

    private void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(FluxiaDynamoBlock.LIT) && state.getValue(FluxiaDynamoBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(FluxiaDynamoBlock.LIT, lit), 3);
        }
    }

    @Override
    protected void addExtraGoggleLines(java.util.List<Component> lines) {
        lines.add(GoggleText.line("goggles.wispwillow.fuel",
                burnTime > 0 ? GoggleText.seconds(burnTime) : "—"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(BURN_TAG, burnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt(BURN_TAG);
    }
}
