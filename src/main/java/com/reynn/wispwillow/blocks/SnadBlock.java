package com.reynn.wispwillow.blocks;

import com.reynn.wispwillow.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SnadBlock extends ColoredFallingBlock {
    public SnadBlock(Properties properties) {
        super(new ColorRGBA(14406560), properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    private static int ensureRange(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private static int getSpeed() {
        return Config.SNAD_SPEED.get();
    }

    private static int getMaxHeight() {
        return Config.SNAD_MAX_HEIGHT.get();
    }

    private static boolean forceMaxAge() {
        return Config.SNAD_FORCE_MAX_AGE.get();
    }

    private static boolean rollChance(RandomSource random) {
        int p = Config.SNAD_CHANCE_PERMILLE.get();
        if (p >= 1000) return true;
        if (p <= 0) return false;
        return random.nextInt(1000) < p;
    }

    private static void forceAgeToMax(ServerLevel level, BlockPos pos, BlockState state) {
        // If it has *any* integer property named "age", set to its max value.
        for (var prop : state.getProperties()) {
            if (prop instanceof IntegerProperty ip && "age".equals(ip.getName())) {
                int max = ip.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                if (state.getValue(ip) != max) {
                    level.setBlockAndUpdate(pos, state.setValue(ip, max));
                }
                return;
            }
        }
    }

    // The whitelist is parsed from strings in the config. Doing that on every random
    // tick meant allocating ResourceLocations and TagKeys thousands of times a second on
    // a big Snad farm, so parse once and rebuild only when the config list changes.
    private static List<? extends String> parsedFrom;
    private static Set<ResourceLocation> whitelistedIds = Set.of();
    private static List<TagKey<Block>> whitelistedTags = List.of();

    private static void ensureWhitelistParsed() {
        List<? extends String> list = Config.SNAD_WHITELIST.get();
        if (list == parsedFrom) return;

        Set<ResourceLocation> ids = new HashSet<>();
        List<TagKey<Block>> tags = new ArrayList<>();

        for (String entry : list) {
            if (entry == null || entry.isBlank()) continue;
            if (entry.charAt(0) == '#') {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null) tags.add(TagKey.create(Registries.BLOCK, tagId));
            } else {
                ResourceLocation blockId = ResourceLocation.tryParse(entry);
                if (blockId != null) ids.add(blockId);
            }
        }

        whitelistedIds = ids;
        whitelistedTags = tags;
        parsedFrom = list;
    }

    private static boolean isWhitelisted(BlockState state) {
        ensureWhitelistParsed();

        if (!whitelistedIds.isEmpty()
                && whitelistedIds.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
            return true;
        }
        for (TagKey<Block> tag : whitelistedTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private static void maybeForceAge(ServerLevel world, BlockPos pos, BlockState state) {
        if (!forceMaxAge()) return;

        // Generic: set any "age" integer property to max
        forceAgeToMax(world, pos, state);

        // Optional fallback for blocks that don’t use property name "age"
        // (Most vanilla crops do; you can delete this if unnecessary.)
        Block b = state.getBlock();
        if (b == Blocks.BAMBOO && state.hasProperty(BambooStalkBlock.AGE)) {
            world.setBlockAndUpdate(pos, state.setValue(BambooStalkBlock.AGE, 1));
        }
    }


    public static void snadGrow(ServerLevel world, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);
        if (aboveState.isAir()) return;

        Block blockAbove = aboveState.getBlock();
        if (!isWhitelisted(aboveState)) return;

        int speed = getSpeed();

        // Tick the block directly above Snad
        for (int i = 0; i < speed; i++) {
            aboveState.randomTick(world, above, random);
            aboveState = world.getBlockState(above); // refresh state after tick
            maybeForceAge(world, above, aboveState);
        }

        // Now accelerate upward column of the same block type, but bounded by maxHeight
        int maxH = Math.max(1, getMaxHeight());
        for (int height = 2; height <= maxH; height++) {
            BlockPos current = pos.above(height);
            if (current.getY() >= world.getMaxBuildHeight()) break;

            BlockState curState = world.getBlockState(current);
            if (curState.isAir()) break;

            if (curState.getBlock().getClass() != blockAbove.getClass()) break;
            if (!isWhitelisted(curState)) break; // safety

            for (int i = 0; i < speed; i++) {
                curState.randomTick(world, current, random);
                curState = world.getBlockState(current); // refresh
                // This aged the *bottom* block over and over instead of the one we just
                // ticked, so tall crops above the first block never finished growing.
                maybeForceAge(world, current, curState);
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;
        if (!rollChance(random)) return;
        snadGrow(level, pos, random);
    }
}
