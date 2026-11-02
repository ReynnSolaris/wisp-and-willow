package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.altar.AltarTier;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.Vigil;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One scene per rite, written from its {@link RitualDefinitions.Def} alone: the foundation
 * it needs, its circle, its reagents, its price, and what it does. A new rite gets a scene
 * the moment it is defined.
 */
final class RitualScenes {
    private RitualScenes() {}

    static PonderStoryBoard of(RitualDefinitions.Def def) {
        return (scene, util) -> program(def, scene, util);
    }

    private static void program(RitualDefinitions.Def def, SceneBuilder scene, SceneBuildingUtil util) {
        int size = PonderLayouts.ritualSize(def);
        BlockPos altar = PonderLayouts.centre(size);
        BlockPos floor = altar.below();
        Map<BlockPos, BlockState> glyphs = PonderLayouts.glyphs(def, altar);

        scene.title(PonderLayouts.ritualId(def), def.displayName());
        scene.configureBasePlate(0, 0, size);
        SceneKit.fit(scene, size);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(altar), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "ritual." + def.path() + ".lore", util.vector().topOf(altar));

        if (def.minTier() > 1) {
            List<BlockPos> runes = new ArrayList<>();
            for (int ring = 1; ring < def.minTier(); ring++) {
                runes.addAll(PonderLayouts.ring(floor, AltarTier.ringRadius(ring)));
            }
            scene.overlay().showOutline(PonderPalette.BLUE, "foundation", SceneKit.select(util, runes), 120);
            SceneKit.say(scene, "ritual.tier", util.vector().topOf(floor.east()),
                    SceneKit.roman(def.minTier()), AltarTier.runesForTier(def.minTier()));
        }

        // Outermost ring first: the order a circle is actually chalked in.
        TreeMap<Integer, List<BlockPos>> rings = new TreeMap<>(Comparator.reverseOrder());
        for (BlockPos p : glyphs.keySet()) {
            int r = Math.max(Math.abs(p.getX() - altar.getX()), Math.abs(p.getZ() - altar.getZ()));
            rings.computeIfAbsent(r, k -> new ArrayList<>()).add(p);
        }
        for (List<BlockPos> ring : rings.values()) {
            scene.world().showSection(SceneKit.select(util, ring), Direction.DOWN);
            scene.idle(8);
        }
        if (!glyphs.isEmpty()) {
            scene.overlay().showOutline(PonderPalette.GREEN, "circle", SceneKit.select(util, glyphs.keySet()), 100);
        }
        SceneKit.say(scene, "ritual.glyphs", null, glyphs.size());

        // Reagents close in; gear to be awakened a little further out, so the two read apart.
        double near = Math.min(Math.max(1.0, def.radius() - 1.0), 2.0);
        double far = Math.min(near + 1.0, def.radius() - 0.5);
        List<ItemStack> gear = def.imbues().map(RitualScenes::imbuable)
                .orElseGet(() -> def.awakens().isPresent() ? awakenable(def) : List.of());
        if (!gear.isEmpty()) {
            SceneKit.scatter(scene, util, altar, gear, far);
            if (def.isImbuing()) {
                SceneKit.say(scene, "ritual.imbue.lay", null);
            } else {
                SceneKit.say(scene, "ritual.awaken.lay", null,
                        def.awakens().get().location().getPath().replace('_', ' '));
            }
        }

        List<ItemStack> reagents = AltarScenes.reagents(def);
        List<ElementLink<EntityElement>> dropped = SceneKit.scatter(scene, util, altar, reagents, near);
        if (!reagents.isEmpty()) SceneKit.say(scene, "ritual.reagents", null, SceneKit.describe(reagents));
        SceneKit.say(scene, "ritual.fluxia", util.vector().topOf(altar), PonderText.num(def.fluxia()));
        SceneKit.cast(scene, util, altar, dropped);

        Vec3 above = util.vector().topOf(altar);
        if (def.vigil().isPresent()) {
            Vigil vigil = def.vigil().get();
            for (int i = 0; i < 3; i++) {
                SceneKit.sparkle(scene, above.add(0, 0.5, 0), 10);
                scene.idle(10);
            }
            SceneKit.say(scene, "ritual.result.vigil", above,
                    Vigil.formatTicks(vigil.durationTicks()), PonderText.num(vigil.upkeepPerSecond()));
        } else if (!gear.isEmpty()) {
            for (int i = 0; i < gear.size(); i++) {
                SceneKit.sparkle(scene, SceneKit.around(util, altar, i, gear.size(), far), 10);
            }
            scene.idle(20);
            if (def.isImbuing()) {
                SceneKit.say(scene, "ritual.result.imbue", null, def.imbues().get().displayName().getString());
            } else {
                SceneKit.say(scene, "ritual.result.awaken", null);
            }
        } else if (def.resultMob().isPresent()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(def.resultMob().get());
            int count = def.resultMobCount();
            // Spaced by the creature's own size, so three withers read as three, not one heap.
            double gap = Math.max(1.8, type.getWidth() * 3);
            for (int i = 0; i < count; i++) {
                SceneKit.spawn(scene, type, above.add((i - (count - 1) / 2.0) * gap, 0.2, 0));
            }
            scene.idle(20);
            String name = type.getDescription().getString();
            SceneKit.say(scene, "ritual.result.summon", above.add(0, 1, 0),
                    count > 1 ? count + "× " + name : name);
        } else if (def.resultItem().isPresent()) {
            SceneKit.drop(scene, util, altar, def.resultItem().get());
            scene.idle(20);
            SceneKit.say(scene, "ritual.result.item", above, SceneKit.describe(def.resultItem().get()));
        }
        scene.markAsFinished();
    }

    /** A couple of awakened pieces of the kind an imbuing suits, glinting as they would in-world. */
    private static List<ItemStack> imbuable(Imbuement imbuement) {
        List<ItemStack> candidates = BuiltInRegistries.ITEM.getTag(TagRegistry.Items.EMPYRITE)
                .map(set -> set.stream().map(ItemStack::new).filter(imbuement::accepts).limit(2).toList())
                .orElse(List.of());
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : candidates) {
            ItemStack awake = stack.copy();
            awake.set(ComponentRegistry.AWAKENED.get(), Unit.INSTANCE);
            out.add(awake);
        }
        return out;
    }

    /** A couple of real pieces from the tag the rite awakens, preferring gear over ingots. */
    private static List<ItemStack> awakenable(RitualDefinitions.Def def) {
        List<ItemStack> gear = BuiltInRegistries.ITEM.getTag(def.awakens().get())
                .map(set -> set.stream().map(ItemStack::new).filter(ItemStack::isDamageableItem).limit(2).toList())
                .orElse(List.of());
        return gear.isEmpty() ? List.of(def.icon().copy()) : gear;
    }
}
