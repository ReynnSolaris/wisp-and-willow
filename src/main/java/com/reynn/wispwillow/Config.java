package com.reynn.wispwillow;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {

    // -------------------------
    // CLIENT
    // -------------------------
    public static final ModConfigSpec CLIENT_SPEC;

    // -------------------------
    // SERVER
    // -------------------------
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.BooleanValue FLUXIA_PHASING;

    public static final ModConfigSpec.IntValue SNAD_SPEED;
    public static final ModConfigSpec.IntValue SNAD_MAX_HEIGHT;
    public static final ModConfigSpec.IntValue SNAD_CHANCE_PERMILLE;
    public static final ModConfigSpec.BooleanValue SNAD_FORCE_MAX_AGE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SNAD_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEVELOPER_UUIDS;

    static {
        // ---------- client ----------
        ModConfigSpec.Builder c = new ModConfigSpec.Builder();

        CLIENT_SPEC = c.build();

        // ---------- server ----------
        ModConfigSpec.Builder s = new ModConfigSpec.Builder();

        s.push("fluxia");
        FLUXIA_PHASING = s.comment(
                        "Whether going under in Liquid Fluxia makes a player phase through blocks.",
                        "Off means Fluxia is just a liquid, and the Fluxia Oasis keeps its treasure.")
                .define("phasing", true);
        s.pop();

        s.push("snad");
        SNAD_SPEED = s.comment("How many extra randomTick calls to apply (1..64). Default 16.")
                .defineInRange("speed", 16, 1, 64);

        SNAD_MAX_HEIGHT = s.comment("How many blocks upward to scan and accelerate. Default 16.")
                .defineInRange("maxHeight", 16, 1, 256);

        SNAD_CHANCE_PERMILLE = s.comment("Chance per random tick to activate (0..1000). Default 1000 (always).")
                .defineInRange("chancePermille", 1000, 0, 1000);

        SNAD_FORCE_MAX_AGE = s.comment("If true, sets AGE to max for known crops after ticking.")
                .define("forceMaxAge", true);

        SNAD_WHITELIST = s.comment("Blocks/tags to accelerate. Use 'modid:block' or '#modid:tag'.")
                .defineListAllowEmpty(
                        "whitelist",
                        List.of(
                                "minecraft:sugar_cane",
                                "minecraft:cactus",
                                "minecraft:bamboo",
                                "wispwillow:amethyst_sugar_cane",
                                "#minecraft:bamboo_blocks" // example
                        ),
                        o -> o instanceof String str && (str.contains(":") || (str.startsWith("#") && str.substring(1).contains(":")))
                );
        s.pop();

        s.push("developer");
        DEVELOPER_UUIDS = s.comment(
                        "UUIDs permitted to summon developer familiars.",
                        "The mod author's own UUID is always permitted and does not need listing.",
                        "Dashed or undashed both work.")
                .defineListAllowEmpty(
                        "uuids",
                        List.of(),
                        o -> o instanceof String str && !str.isBlank()
                );
        s.pop();

        SERVER_SPEC = s.build();
    }
}
