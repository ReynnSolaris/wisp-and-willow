package com.reynn.wispwillow.client.screen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlockEntity;
import com.reynn.wispwillow.client.menu.AlchemicalCauldronMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class AlchemicalCauldronScreen extends AbstractContainerScreen<AlchemicalCauldronMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/gui/alchemical_cauldron.png");

    // Geometry, all relative to the top-left of the background.
    private static final int TANK_X_IN = 8, TANK_X_OUT = 152, TANK_Y = 18, TANK_W = 16, TANK_H = 54;
    private static final int FLUXIA_X = 30, FLUXIA_Y = 95, FLUXIA_W = 116, FLUXIA_H = 5;
    private static final int ARROW_X = 84, ARROW_Y = 35, ARROW_W = 24, ARROW_H = 17;
    private static final int DUMP_IN_X = 8, DUMP_OUT_X = 152, DUMP_Y = 73, DUMP_SIZE = 16;
    private static final int RECIRC_X = 80, RECIRC_Y = 56, RECIRC_SIZE = 16;

    // Sprite offsets into the sheet's right-hand column of widgets.
    private static final int UV_ARROW_EMPTY_X = 176, UV_ARROW_EMPTY_Y = 0;
    private static final int UV_ARROW_FULL_X = 176, UV_ARROW_FULL_Y = 17;
    private static final int UV_FLUXIA_X = 0, UV_FLUXIA_Y = 206;
    private static final int UV_DUMP_X = 200, UV_DUMP_Y = 34;
    private static final int UV_DUMP_HOVER_X = 216, UV_DUMP_HOVER_Y = 34;
    private static final int UV_RECIRC_X = 232, UV_RECIRC_Y = 34;
    private static final int UV_RECIRC_HOVER_X = 232, UV_RECIRC_HOVER_Y = 50;

    public AlchemicalCauldronScreen(AlchemicalCauldronMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        gfx.blit(BG, x, y, 0, 0, imageWidth, imageHeight);

        AlchemicalCauldronBlockEntity be = menu.cauldron();

        renderTank(gfx, x + TANK_X_IN, y + TANK_Y, be.inputTank().getFluid(),
                be.inputTank().getCapacity());
        renderTank(gfx, x + TANK_X_OUT, y + TANK_Y, be.outputTank().getFluid(),
                be.outputTank().getCapacity());

        // Fluxia bar fills left to right.
        int fluxiaW = Math.round(FLUXIA_W * menu.fluxiaFraction());
        if (fluxiaW > 0) {
            gfx.blit(BG, x + FLUXIA_X, y + FLUXIA_Y, UV_FLUXIA_X, UV_FLUXIA_Y, fluxiaW, FLUXIA_H);
        }

        // Progress arrow, drawn as a left-to-right reveal of the lit version.
        gfx.blit(BG, x + ARROW_X, y + ARROW_Y, UV_ARROW_EMPTY_X, UV_ARROW_EMPTY_Y, ARROW_W, ARROW_H);
        int arrowW = Math.round(ARROW_W * menu.progressFraction());
        if (arrowW > 0) {
            gfx.blit(BG, x + ARROW_X, y + ARROW_Y, UV_ARROW_FULL_X, UV_ARROW_FULL_Y, arrowW, ARROW_H);
        }

        drawDump(gfx, x + DUMP_IN_X, y + DUMP_Y, mouseX, mouseY);
        drawDump(gfx, x + DUMP_OUT_X, y + DUMP_Y, mouseX, mouseY);

        // The recirculate arrow only lights up when there is actually something to pour
        // back, so hovering a dead control does not promise an action it will not perform.
        boolean canRecirculate = !be.outputTank().isEmpty();
        boolean recircHover = inBox(mouseX, mouseY, x + RECIRC_X, y + RECIRC_Y, RECIRC_SIZE, RECIRC_SIZE);
        gfx.blit(BG, x + RECIRC_X, y + RECIRC_Y,
                recircHover && canRecirculate ? UV_RECIRC_HOVER_X : UV_RECIRC_X,
                recircHover && canRecirculate ? UV_RECIRC_HOVER_Y : UV_RECIRC_Y,
                RECIRC_SIZE, RECIRC_SIZE);
    }

    private void drawDump(GuiGraphics gfx, int x, int y, int mouseX, int mouseY) {
        boolean hover = inBox(mouseX, mouseY, x, y, DUMP_SIZE, DUMP_SIZE);
        gfx.blit(BG, x, y,
                hover ? UV_DUMP_HOVER_X : UV_DUMP_X,
                hover ? UV_DUMP_HOVER_Y : UV_DUMP_Y,
                DUMP_SIZE, DUMP_SIZE);
    }

    /**
     * Draws a fluid column using the fluid's own still texture and tint, so any mod's
     * fluid piped in here looks like itself rather than a flat purple rectangle.
     */
    private void renderTank(GuiGraphics gfx, int x, int y, FluidStack fluid, int capacity) {
        if (fluid.isEmpty() || capacity <= 0) return;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTexture = ext.getStillTexture(fluid);
        if (stillTexture == null) return;

        TextureAtlasSprite sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTexture);
        int tint = ext.getTintColor(fluid);

        int filled = Math.max(1, Math.round(TANK_H * (fluid.getAmount() / (float) capacity)));
        int top = y + TANK_H - filled;

        // The sprite is 16x16; tile it vertically and clip the last row.
        for (int drawn = 0; drawn < filled; drawn += 16) {
            int h = Math.min(16, filled - drawn);
            gfx.blit(x, top + drawn, 0, TANK_W, h, sprite, 
                    ((tint >> 16) & 0xFF) / 255f,
                    ((tint >> 8) & 0xFF) / 255f,
                    (tint & 0xFF) / 255f,
                    ((tint >> 24) & 0xFF) / 255f);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltips(gfx, mouseX, mouseY);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderTooltips(GuiGraphics gfx, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        AlchemicalCauldronBlockEntity be = menu.cauldron();

        if (inBox(mouseX, mouseY, x + TANK_X_IN, y + TANK_Y, TANK_W, TANK_H)) {
            gfx.renderComponentTooltip(font, tankLines(be.inputTank().getFluid(),
                    be.inputTank().getCapacity(), "gui.wispwillow.cauldron.input_tank"), mouseX, mouseY);
        } else if (inBox(mouseX, mouseY, x + TANK_X_OUT, y + TANK_Y, TANK_W, TANK_H)) {
            gfx.renderComponentTooltip(font, tankLines(be.outputTank().getFluid(),
                    be.outputTank().getCapacity(), "gui.wispwillow.cauldron.output_tank"), mouseX, mouseY);
        } else if (inBox(mouseX, mouseY, x + FLUXIA_X, y + FLUXIA_Y, FLUXIA_W, FLUXIA_H)) {
            gfx.renderTooltip(font, Component.translatable("gui.wispwillow.cauldron.fluxia",
                    menu.fluxia(), menu.fluxiaMax()), mouseX, mouseY);
        } else if (inBox(mouseX, mouseY, x + DUMP_IN_X, y + DUMP_Y, DUMP_SIZE, DUMP_SIZE)
                || inBox(mouseX, mouseY, x + DUMP_OUT_X, y + DUMP_Y, DUMP_SIZE, DUMP_SIZE)) {
            gfx.renderTooltip(font, Component.translatable("gui.wispwillow.cauldron.dump"), mouseX, mouseY);
        } else if (inBox(mouseX, mouseY, x + RECIRC_X, y + RECIRC_Y, RECIRC_SIZE, RECIRC_SIZE)) {
            gfx.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.wispwillow.cauldron.recirculate"),
                    Component.translatable("gui.wispwillow.cauldron.recirculate.hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        }
    }

    private List<Component> tankLines(FluidStack fluid, int capacity, String labelKey) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(labelKey));
        if (fluid.isEmpty()) {
            lines.add(Component.translatable("gui.wispwillow.cauldron.empty"));
        } else {
            lines.add(fluid.getHoverName());
            lines.add(Component.translatable("gui.wispwillow.cauldron.amount",
                    fluid.getAmount(), capacity));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Left button only. Voiding a tank cannot be undone, so it should not be something
        // a stray right-click while reading the tooltip can do to you.
        if (button == 0) {
            int x = leftPos, y = topPos;
            if (inBox((int) mouseX, (int) mouseY, x + DUMP_IN_X, y + DUMP_Y, DUMP_SIZE, DUMP_SIZE)) {
                press(AlchemicalCauldronMenu.BUTTON_DUMP_INPUT);
                return true;
            }
            if (inBox((int) mouseX, (int) mouseY, x + DUMP_OUT_X, y + DUMP_Y, DUMP_SIZE, DUMP_SIZE)) {
                press(AlchemicalCauldronMenu.BUTTON_DUMP_OUTPUT);
                return true;
            }
            if (inBox((int) mouseX, (int) mouseY, x + RECIRC_X, y + RECIRC_Y, RECIRC_SIZE, RECIRC_SIZE)) {
                press(AlchemicalCauldronMenu.BUTTON_RECIRCULATE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void press(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, 1.0f));
    }

    private static boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
