package com.reynn.wispwillow.entity.familiar;

import com.reynn.wispwillow.Config;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLLoader;

import java.util.UUID;

/**
 * Who counts as a developer, for the content that is gated to one.
 *
 * <p>There is no per-player recipe visibility in Minecraft — the recipe book can hide a
 * recipe behind an advancement, but any external recipe viewer still lists it and anyone who
 * knows the shape can still make it. So the gate that actually holds is on the
 * <em>interaction</em>: a check against the player standing there, at the moment they act.
 * Everything developer-only in this mod goes through this class.</p>
 */
public final class Developers {

    private Developers() {}

    /** The author. Additional UUIDs go in the server config rather than here. */
    private static final UUID AUTHOR = UUID.fromString("1303d110-c5fc-4530-8552-de5829b86491");

    /**
     * The name a dev-environment player is given by default.
     *
     * <p>Only honoured outside production, so a player who happens to be called Dev on a
     * real server gets nothing.</p>
     */
    private static final String DEV_ENV_NAME = "Dev";

    public static boolean is(Player player) {
        if (player == null) return false;

        if (AUTHOR.equals(player.getUUID())) return true;

        if (!FMLLoader.isProduction() && DEV_ENV_NAME.equalsIgnoreCase(player.getGameProfile().getName())) {
            return true;
        }

        return matchesConfig(player.getUUID());
    }

    public static boolean is(UUID uuid) {
        if (uuid == null) return false;
        return AUTHOR.equals(uuid) || matchesConfig(uuid);
    }

    private static boolean matchesConfig(UUID uuid) {
        // The config spec is not loaded during datagen or early startup, and asking a
        // ConfigValue for its value before then throws rather than returning a default.
        try {
            for (String entry : Config.DEVELOPER_UUIDS.get()) {
                UUID parsed = parse(entry);
                if (parsed != null && parsed.equals(uuid)) return true;
            }
        } catch (IllegalStateException ignored) {
            // Config not loaded yet.
        }
        return false;
    }

    /**
     * Accepts a UUID with or without dashes.
     *
     * <p>Every tool that hands a player their UUID formats it differently, and being told
     * "invalid" because you pasted the version your launcher gave you would be a silly way
     * to lose ten minutes.</p>
     */
    private static UUID parse(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            if (trimmed.length() == 32 && trimmed.indexOf('-') < 0) {
                trimmed = trimmed.substring(0, 8) + "-" + trimmed.substring(8, 12) + "-"
                        + trimmed.substring(12, 16) + "-" + trimmed.substring(16, 20) + "-"
                        + trimmed.substring(20);
            }
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }
}
