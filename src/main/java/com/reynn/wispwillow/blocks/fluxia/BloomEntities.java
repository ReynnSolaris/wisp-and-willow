package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/** The three functional blooms. Each is a small, self-contained job. */
public final class BloomEntities {
    private BloomEntities() {}

    // ==================================================================
    // Verdant Bloom — accelerates growth
    // ==================================================================

    public static class Verdant extends FunctionalBloomEntity {
        private static final int RADIUS = 5;

        public Verdant(BlockPos pos, BlockState state) {
            super(BlockEntityRegistry.VERDANT_BLOOM.get(), pos, state, 4000, 200);
        }

        @Override protected int interval() { return 40; }
        @Override protected int costPerAction() { return 60; }
        @Override public int radius() { return RADIUS; }

        @Override
        protected Component describeWork() {
            return Component.translatable("goggles.wispwillow.effect.verdant");
        }

        /**
         * Bonemeals one random growable in range per attempt. One at a time keeps the
         * Fluxia cost honest and spreads the work out instead of spiking a tick.
         */
        @Override
        protected boolean performWork(ServerLevel level, BlockPos pos) {
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos target = pos.offset(
                        level.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                        level.random.nextInt(3) - 1,
                        level.random.nextInt(RADIUS * 2 + 1) - RADIUS);

                BlockState state = level.getBlockState(target);
                if (!(state.getBlock() instanceof BonemealableBlock growable)) continue;
                if (!growable.isValidBonemealTarget(level, target, state)) continue;
                if (!growable.isBonemealSuccess(level, level.random, target, state)) continue;

                growable.performBonemeal(level, level.random, target, state);
                level.levelEvent(2005, target, 0);
                return true;
            }
            return false;
        }
    }

    // ==================================================================
    // Gathering Bloom — vacuums drops into an adjacent inventory
    // ==================================================================

    public static class Gathering extends FunctionalBloomEntity {
        private static final int RADIUS = 6;

        public Gathering(BlockPos pos, BlockState state) {
            super(BlockEntityRegistry.GATHERING_BLOOM.get(), pos, state, 4000, 200);
        }

        @Override protected int interval() { return 10; }
        @Override protected int costPerAction() { return 10; }
        @Override public int radius() { return RADIUS; }

        @Override
        protected Component describeWork() {
            return Component.translatable("goggles.wispwillow.effect.gathering");
        }

        /**
         * Sweeps loose items into the inventory directly below. With nothing below it, the
         * bloom just gathers them to itself, which is still useful for a mob farm.
         */
        @Override
        protected boolean performWork(ServerLevel level, BlockPos pos) {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(pos).inflate(RADIUS), ItemEntity::isAlive);
            if (drops.isEmpty()) return false;

            IItemHandler below = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, pos.below(), net.minecraft.core.Direction.UP);

            boolean did = false;
            for (ItemEntity drop : drops) {
                ItemStack stack = drop.getItem();
                if (stack.isEmpty()) continue;

                if (below != null) {
                    ItemStack remaining = stack.copy();
                    for (int slot = 0; slot < below.getSlots() && !remaining.isEmpty(); slot++) {
                        remaining = below.insertItem(slot, remaining, false);
                    }
                    if (remaining.getCount() != stack.getCount()) {
                        did = true;
                        if (remaining.isEmpty()) drop.discard();
                        else drop.setItem(remaining);
                    }
                } else if (drop.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 1.5) {
                    // No inventory: pull the drops in so they at least pile up somewhere.
                    drop.setPos(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);
                    drop.setDeltaMovement(0, 0, 0);
                    did = true;
                }
            }
            return did;
        }
    }

    // ==================================================================
    // Warding Bloom — keeps hostiles out
    // ==================================================================

    public static class Warding extends FunctionalBloomEntity {
        private static final int RADIUS = 12;

        public Warding(BlockPos pos, BlockState state) {
            super(BlockEntityRegistry.WARDING_BLOOM.get(), pos, state, 6000, 200);
        }

        @Override protected int interval() { return 20; }
        @Override protected int costPerAction() { return 40; }
        @Override public int radius() { return RADIUS; }

        @Override
        protected Component describeWork() {
            return Component.translatable("goggles.wispwillow.effect.warding");
        }

        /**
         * Removes newly-spawned hostiles in range.
         *
         * <p>Only mobs that spawned naturally and are still at full health are cleared, so
         * a warded grove stays quiet without deleting anything a player dragged home or
         * anything mid-fight.</p>
         */
        @Override
        protected boolean performWork(ServerLevel level, BlockPos pos) {
            List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                    new AABB(pos).inflate(RADIUS),
                    m -> m.isAlive()
                            && m.getHealth() >= m.getMaxHealth()
                            && !m.isPersistenceRequired()
                            && m.getType().getCategory() == MobCategory.MONSTER);

            if (hostiles.isEmpty()) return false;

            for (Monster m : hostiles) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        m.getX(), m.getY() + 0.5, m.getZ(), 8, 0.2, 0.4, 0.2, 0.01);
                m.discard();
            }
            return true;
        }
    }
}
