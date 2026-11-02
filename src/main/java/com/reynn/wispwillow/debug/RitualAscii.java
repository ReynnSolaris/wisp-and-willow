package com.reynn.wispwillow.debug;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.ritual.RitualRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = WispWillow.MODID) // <-- replace with your MODID const if different
public final class RitualAscii {
    private static final Logger LOG = LoggerFactory.getLogger("WispWillow/RitualAscii");
    private static boolean debug = false;
    private RitualAscii() {}

    @SubscribeEvent
    public static void onAddReload(AddReloadListenerEvent e) {
        final ReloadableServerResources serverRes = e.getServerResources();

        // Register a tiny reload listener that runs AFTER recipes have been loaded.
        e.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                                  ResourceManager mgr,
                                                  ProfilerFiller prepProfiler,
                                                  ProfilerFiller applyProfiler,
                                                  Executor prepExec,
                                                  Executor applyExec) {

                // No preparation work; jump to apply after vanilla reloading completes.
                return CompletableFuture.completedFuture(null)
                        .thenCompose(barrier::wait)
                        .thenRunAsync(() -> dumpAll(serverRes.getRecipeManager()), applyExec);
            }
        });
    }

    private static void dumpAll(RecipeManager recipeManager) {
        if (debug) {
            var holders = recipeManager.getAllRecipesFor(RitualRecipe.TYPE);
            LOG.info("[RitualAsciiDump] Found {} ritual recipes.", holders.size());

            for (var holder : holders) {
                RitualRecipe r = holder.value();
                ResourceLocation id = r.getId();

                LOG.info("=== {} | grid={} radius={} ===", id, r.grid(), r.maxRadius());

                for (String line : makeAscii(r)) {
                    LOG.info(line);
                }

                // List exact pattern cells with color/type for clarity
                r.pattern().forEach(c -> LOG.info("({},{}) {} {} {}",
                        c.dx(), c.dz(),
                        c.type().map(Enum::name).orElse("ANY"),
                        c.color().map(Enum::name).orElse("NO-COLOR"),
                        c.required() ? "required" : "optional"));
            }
            LOG.info("Legend: A=Altar  B=BASE  S=STAR  E=ECLIPSE  R=RUNES  ?=any-sigil  '.'=empty");
            LOG.info("        Uppercase=required  lowercase=optional (guide)  color listed below");
        }
    }

    /** Build a top-down ASCII grid with fixed width tokens. */
    private static java.util.List<String> makeAscii(RitualRecipe r) {
        int rad = Math.max(1, r.maxRadius());
        int size = rad * 2 + 1;

        // Use 2-char tokens for alignment (e.g., "B ", "s ", ". ", "A ")
        String[][] grid = new String[size][size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) grid[z][x] = ". ";
        }
        // center altar
        grid[rad][rad] = "A ";

        // place pattern cells
        for (var c : r.pattern()) {
            int gx = rad + c.dx();
            int gz = rad + c.dz();
            if (gx < 0 || gx >= size || gz < 0 || gz >= size) continue;

            char ch = switch (c.type().map(Enum::name).orElse("ANY")) {
                case "BASE"    -> 'B';
                case "STAR"    -> 'S';
                case "ECLIPSE" -> 'E';
                case "RUNES"   -> 'R';
                default        -> '?';
            };
            // optional cells as lowercase
            if (!c.required()) ch = Character.toLowerCase(ch);
            String col = "";
            if (c.color().isPresent()) {
                var d = c.color().get().getName();
                col = "-" + d.substring(0,1).toUpperCase() + d.substring(d.length()-1).toUpperCase();
            }
            grid[gz][gx] = ch + " ";
        }

        var lines = new ArrayList<String>(size + 2);
        // header
        lines.add("Z\\X  " + seq(-rad, rad));
        for (int z = -rad; z <= rad; z++) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%+2d | ", z));
            for (int x = -rad; x <= rad; x++) {
                sb.append(grid[z + rad][x + rad]);
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private static String seq(int a, int b) {
        var sb = new StringBuilder();
        for (int i = a; i <= b; i++) {
            sb.append(String.format("%+d", i));
            if (i < b) sb.append(' ');
        }
        return sb.toString();
    }
}