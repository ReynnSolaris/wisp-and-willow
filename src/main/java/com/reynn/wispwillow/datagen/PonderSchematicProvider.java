package com.reynn.wispwillow.datagen;

import com.google.common.hash.Hashing;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.compat.ponder.PonderLayouts;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the structures the Ponder scenes are built on, as the structure-template NBT Ponder
 * loads from {@code assets/<mod>/ponder/}.
 *
 * <p>Generated rather than saved by hand from a world, for the same reason the grimoire is: the
 * layouts come from the tables the blocks themselves check, so a scene cannot drift from the
 * rules it is teaching.</p>
 */
public class PonderSchematicProvider implements DataProvider {
    private final PackOutput.PathProvider paths;

    public PonderSchematicProvider(PackOutput output) {
        this.paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "ponder");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (PonderLayouts.Layout layout : PonderLayouts.all()) {
            byte[] bytes = encode(layout);
            Path path = paths.file(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, layout.id()), "nbt");
            writes.add(CompletableFuture.runAsync(() -> {
                try {
                    cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Util.backgroundExecutor()));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static byte[] encode(PonderLayouts.Layout layout) {
        // Sorted, so the same layout always writes the same bytes and the cache stays quiet.
        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(layout.blocks().entrySet());
        entries.sort(Map.Entry.comparingByKey());

        Map<BlockState, Integer> palette = new LinkedHashMap<>();
        ListTag blocks = new ListTag();
        for (Map.Entry<BlockPos, BlockState> e : entries) {
            BlockPos pos = e.getKey();
            CompoundTag block = new CompoundTag();
            block.put("pos", ints(pos.getX(), pos.getY(), pos.getZ()));
            block.putInt("state", palette.computeIfAbsent(e.getValue(), s -> palette.size()));
            blocks.add(block);
        }
        ListTag paletteTag = new ListTag();
        palette.keySet().forEach(state -> paletteTag.add(NbtUtils.writeBlockState(state)));

        CompoundTag root = new CompoundTag();
        root.put("size", ints(layout.size(), layout.height(), layout.size()));
        root.put("palette", paletteTag);
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        NbtUtils.addCurrentDataVersion(root);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag();
        for (int v : values) list.add(IntTag.valueOf(v));
        return list;
    }

    @Override
    public String getName() {
        return "Ponder schematics";
    }
}
