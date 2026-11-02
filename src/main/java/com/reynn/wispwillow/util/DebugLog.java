package com.reynn.wispwillow.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;

/** Centralized, rate-limited debug logging for importer/exporter. */
public final class DebugLog {
    private static final Logger LOG = LoggerFactory.getLogger("WispWillow/Debug");

    /** Flip this in code while testing. Suggest default false for release builds. */
    public static volatile boolean ENABLED = false;

    /** simple per-position throttle to reduce spam */
    private static final WeakHashMap<Level, Long> lastDumpWorld = new WeakHashMap<>();
    private static final WeakHashMap<Object, Long> lastKey = new WeakHashMap<>();

    private DebugLog(){}

    public static void d(String fmt, Object... args) {
        if (!ENABLED) return;
        LOG.info("[AA] " + fmt, args);
    }

    /** Throttle by arbitrary key (e.g., a BE instance) in world time units. */
    public static void dEvery(Object key, long now, long intervalTicks, String fmt, Object... args) {
        if (!ENABLED) return;
        Long last = lastKey.get(key);
        if (last != null && now - last < intervalTicks) return;
        lastKey.put(key, now);
        LOG.info("[AA] " + fmt, args);
    }

    public static void foundHandler(Level level, BlockPos pos, Direction toward, Object handler) {
        if (!ENABLED) return;
        String kind = handler == null ? "NONE" : handler.getClass().getSimpleName();
        d("handler@{} {} -> {} : {}", pos, toward, pos.relative(toward), kind);
    }

    public static void move(String who, BlockPos pos, String phase, int tried, int moved, int leftover) {
        d("{} {} tried={}, moved={}, leftover={}", who, phaseAt(pos, phase), tried, moved, leftover);
    }

    public static void reason(String who, BlockPos pos, String msg) {
        d("{} {} {}", who, box(pos), msg);
    }

    private static String box(BlockPos p) { return "["+p.getX()+","+p.getY()+","+p.getZ()+"]"; }
    private static String phaseAt(BlockPos p, String phase) { return box(p) + " " + phase; }
}
