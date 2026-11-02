package com.reynn.wispwillow.client;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.items.Awakening;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Whether the item currently being drawn is awakened, for the few frames of the render
 * pass that need to know.
 *
 * <p>Vanilla picks a glint type from five static, argument-free accessors on
 * {@code RenderType}, so by the time the glint is chosen the stack that wanted it is long
 * out of scope. Rather than redirect each of the call sites that lead there (four of them,
 * in two classes, all high-traffic and heavily mixed by other mods) the stack is announced
 * once at the top of each render and read back at the accessor. Every glint call site,
 * present and future, is then covered by construction.</p>
 *
 * <p>Renders nest — an item inside an item frame, a bundle drawing its contents — so this
 * is a stack rather than a flag, and each push remembers what it displaced. It is touched
 * only from the render thread.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class AwakenedGlint {
    private AwakenedGlint() {}

    /** Depth is render nesting, which is shallow; this never grows to any size. */
    private static boolean[] saved = new boolean[8];
    private static int depth = 0;
    private static boolean active = false;

    public static void push(ItemStack stack) {
        if (depth == saved.length) {
            boolean[] bigger = new boolean[depth * 2];
            System.arraycopy(saved, 0, bigger, 0, depth);
            saved = bigger;
        }
        saved[depth++] = active;
        active = Awakening.isAwakened(stack);
    }

    public static void pop() {
        active = depth > 0 && saved[--depth];
    }

    /** True while the item being drawn should glint gold instead of violet. */
    public static boolean active() {
        return active;
    }

    /**
     * A render that throws between its push and its pop would otherwise leave the flag
     * stuck on and gild every glint in the game until the next reload. One assignment a
     * frame buys immunity to that, and to any future call site that forgets to pop.
     */
    @SubscribeEvent
    static void onFrameStart(RenderFrameEvent.Pre event) {
        depth = 0;
        active = false;
    }
}
