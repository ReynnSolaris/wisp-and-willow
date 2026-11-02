package com.reynn.wispwillow.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WWButton extends Button {
    private static final ResourceLocation TEX_NORMAL =
            ResourceLocation.fromNamespaceAndPath("wispwillow", "textures/gui/widgets/button.png");
    private static final ResourceLocation TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("wispwillow", "textures/gui/widgets/button_highlighted.png");
    private static final ResourceLocation TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("wispwillow", "textures/gui/widgets/button_disabled.png");

    private boolean selected;

    public WWButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        boolean hovered = this.isHoveredOrFocused();
        ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered || selected) ? TEX_HOVER : TEX_NORMAL;

        // draw full texture stretched to widget size
        RenderSystem.enableBlend();
        g.blit(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        // center text
        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                color);
    }
}
