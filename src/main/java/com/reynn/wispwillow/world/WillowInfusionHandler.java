package com.reynn.wispwillow.world;

import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

import java.util.List;

/**
 * World-interaction "rituals": items dropped onto the right block transmute.
 *
 * <p>Registered exactly once, from the mod constructor. This class used to carry an
 * {@code @EventBusSubscriber(modid = "wisp_willow")} annotation — a modid that does not
 * exist — <em>and</em> be registered by hand, so the handler ran twice per item tick.</p>
 */
public class WillowInfusionHandler {
    public static final Vector3f SOLROSA_PARTICLE = new Vector3f(80, 80, 255);

    /**
     * Items are ticked every tick, and each check below costs a block lookup plus an
     * entity query. Only the items that have been on the ground a moment need to react,
     * and only every half second.
     */
    private static final int CHECK_INTERVAL = 10;

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        Level level = itemEntity.level();
        if (level.isClientSide()) return;
        if ((itemEntity.tickCount % CHECK_INTERVAL) != 0) return;

        BlockPos pos = itemEntity.blockPosition();
        BlockState here = level.getBlockState(pos);

        //Handle Solrosa
        if (here.is(Blocks.WITHER_ROSE)) {
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                    itemEntity.getBoundingBox().inflate(0.5), ItemEntity::isAlive);
            boolean hasPrismarine = false;
            for (ItemEntity e : items) {
                if (e.getItem().is(Items.PRISMARINE_SHARD)) { hasPrismarine = true; break; }
            }
            if (hasPrismarine) {
                for (ItemEntity e : items) {
                    ItemStack stack = e.getItem();
                    if (stack.is(Items.PRISMARINE_SHARD)) {
                        stack.shrink(1);
                        if (stack.isEmpty()) e.discard();
                    }
                }
                level.setBlockAndUpdate(pos, BlockRegistry.SOLROSA_FLOWER.get().defaultBlockState());
                level.playSound(null, pos, SoundRegistry.ITEM_INFUSION.get(),  SoundSource.BLOCKS);
                if (level instanceof ServerLevel sl) {
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY() + 0.9;
                    double cz = pos.getZ() + 0.5;
                    sl.sendParticles(
                            new DustParticleOptions(SOLROSA_PARTICLE, 1),
                            cx, cy, cz,
                            4,             // count
                            0.15, 0.10, 0.15, // spread
                            0.01           // speed
                    );
                }
            }
        }

        //Handle Sapling
        if (here.is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
            // Look for all ItemEntities in same block
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                    itemEntity.getBoundingBox().inflate(0.5), ItemEntity::isAlive);

            boolean hasSapling = false;
            boolean hasDeepslate = false;

            for (ItemEntity e : items) {
                ItemStack stack = e.getItem();
                if (stack.is(Items.OAK_SAPLING)) hasSapling = true;
                if (stack.is(Blocks.DEEPSLATE.asItem())) hasDeepslate = true;
            }

            if (hasSapling && hasDeepslate) {
                // Consume 1 of each
                for (ItemEntity e : items) {
                    ItemStack stack = e.getItem();
                    if (stack.is(Items.OAK_SAPLING) || stack.is(Blocks.DEEPSLATE.asItem())) {
                        stack.shrink(1);
                        if (stack.isEmpty()) e.discard();
                    }
                }

                // Spawn the new sapling
                ItemEntity willow = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        new ItemStack(BlockRegistry.WILLOW_SAPLING.get()));

                level.addFreshEntity(willow);
                level.playSound(null, pos, SoundRegistry.ITEM_INFUSION.get(),  SoundSource.BLOCKS);
                level.levelEvent(2005, pos, 0); // particle effect like bonemeal
            }
        }
    }
}
