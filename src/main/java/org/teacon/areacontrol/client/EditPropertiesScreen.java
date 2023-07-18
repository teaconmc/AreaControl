/*
 * This file is a modified version of org.teacon.voteme.screen.VoterScreen, licensed under BSD-3-Clause.
 * A copy of the original file can be found at:
 * https://github.com/teaconmc/VoteMe/blob/1.19-forge/src/main/java/org/teacon/voteme/screen/VoterScreen.java
 */
package org.teacon.areacontrol.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.network.ACShowPropEditScreen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EditPropertiesScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("area_control:textures/gui/edit_properties.png");

    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFF000000 | DyeColor.BLACK.getTextColor();
    private static final int HINT_COLOR = 0xFF000000 | DyeColor.WHITE.getTextColor();

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 304;

    private static final float ARTIFACT_SCALE_FACTOR = 1.5F;
    private final String areaName;

    private final Map<String, Boolean> states;
    private final List<ACShowPropEditScreen.Info> infoCollection;

    private int slideBottom, slideTop;

    public EditPropertiesScreen(String areaName, List<ACShowPropEditScreen.Info> infos) {
        super(GameNarrator.NO_TITLE);
        this.areaName = areaName;
        this.states = new LinkedHashMap<>(infos.size());
        this.infoCollection = ImmutableList.copyOf(infos);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new SideSlider(this.width / 2 - 103, this.height / 2 - 55, 24 * this.infoCollection.size(), this::onSlideClick, this::onSliderChange, Component.literal("Slider")));
        this.addRenderableWidget(new BottomButton(this.width / 2 + 52, this.height / 2 + 82, false, this::onOKButtonClick, Component.translatable("area_control.screen.ok")));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        this.drawGuiContainerBackgroundLayer(guiGraphics, partialTicks, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.drawGuiContainerForegroundLayer(guiGraphics, partialTicks, mouseX, mouseY);
        this.drawTooltips(guiGraphics, partialTicks, mouseX, mouseY);
    }

    @Override
    public void tick() {
        // Nothing to tick
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        // Nothing to sync
    }

    private void onOKButtonClick(Button button) {
        this.onClose();
    }

    private void onSlideClick(double dx, double dy) {
        int current = Mth.floor((this.slideTop + dy) / 24);
        if (current >= 0 && current < this.infoCollection.size()) {
            // FIXME 偶尔会触发到别的属性上去，原因未知
            int offsetX = Mth.floor((dx - 91) / 25);
            int offsetY = Mth.floor((this.slideTop + dy - current * 24 - 4) / 15);
            if (offsetX >= 1 && offsetX <= 5 && offsetY == 0) {
                Boolean newState = offsetX == 1 ? Boolean.FALSE : offsetX == 2 ? null : Boolean.TRUE;
                var prop = this.infoCollection.get(current).prop();
                this.states.put(prop, newState);
                // Execute command on behalf of player
                String commandToExec;
                if (newState == null) {
                    commandToExec = "ac current properties unset " +  prop;
                } else {
                    commandToExec = "ac current properties set " + prop + " " + newState;
                }
                var mc = this.minecraft;
                if (mc != null) {
                    var p = mc.player;
                    if (p != null) {
                        p.connection.sendCommand(commandToExec);
                    }
                }
            }
        }
    }

    private void onSliderChange(int top, int bottom) {
        this.slideTop = top;
        this.slideBottom = bottom;
    }

    private void drawTooltips(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int dx = mouseX - this.width / 2, dy = mouseY - this.height / 2;
        if (dx >= -103 && dy >= -55 && dx < -6 && dy < 77) {
            int current = (this.slideTop + dy + 55) / 24;
            if (current >= 0 && current < this.infoCollection.size()) {
                // FIXME[3TUSK] 别装了，这里没有 tooltip
                //var descList = this.font.split(Component.literal("装作这里有 Tooltip"), 191);
                //this.renderTooltip(matrixStack, descList.subList(0, Math.min(7, descList.size())), mouseX, mouseY);
                this.setTooltipForNextRenderPass(Component.literal("装作这里有 Tooltip"));
            }
        }
    }

    private void drawGuiContainerBackgroundLayer(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.width / 2 - 111, this.height / 2 - 55, 0, 42, 234, 132, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        this.drawCategoriesInSlide(guiGraphics);
        guiGraphics.blit(TEXTURE, this.width / 2 - 111, this.height / 2 - 97, 0, 0, 234, 42, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        guiGraphics.blit(TEXTURE, this.width / 2 - 111, this.height / 2 + 77, 0, 174, 234, 32, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void drawGuiContainerForegroundLayer(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        this.drawArtifactName(guiGraphics, this.font);
    }

    private void drawCategoriesInSlide(GuiGraphics guiGraphics) {
        int infoSize = this.infoCollection.size();
        if (infoSize > 0) {
            int top = Math.max(0, this.slideTop / 24);
            int bottom = Math.min(infoSize, (this.slideBottom + 24) / 24);
            for (int i = top; i < bottom; ++i) {
                int offset = i * 24 - this.slideTop;
                int x0 = this.width / 2 - 103, y0 = this.height / 2 - 55 + offset;
                // draw button group background
                guiGraphics.blit(TEXTURE, x0, y0, 8, 256, 192, 24, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                ACShowPropEditScreen.Info info = this.infoCollection.get(i);
                // draw category string
                int x1 = x0 + 48 - font.width(info.prop()) / 2, y1 = y0 + 8;
                guiGraphics.drawString(this.font, info.prop(), x1, y1, TEXT_COLOR, false);
                // FIXME[3TUSK]: Draw 3 buttons: Allow, Unset, Deny
                Boolean state = this.states.get(info.prop());
                if (state == null) {
                    // Unset is selected
                    guiGraphics.blit(TEXTURE, x0 + 130, y0 + 4, 138, 284, 28, 16, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                } else if (state) {
                    // Allow is selected
                    guiGraphics.blit(TEXTURE, x0 + 159, y0 + 4, 167, 284, 28, 16, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                } else {
                    // Deny is selected
                    guiGraphics.blit(TEXTURE, x0 + 101, y0 + 4, 109, 284, 28, 16, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                }
            }
        } else {
            MutableComponent next = Component.translatable("area_control.screen.no_properties.hints");
            int x1 = this.width / 2 - 7, dx1 = this.font.width(next) / 2, y1 = this.height / 2 + 15;
            guiGraphics.drawString(this.font, next, x1 - dx1, y1, HINT_COLOR, false);

            guiGraphics.pose().pushPose();
            float scale = ARTIFACT_SCALE_FACTOR;
            guiGraphics.pose().scale(scale, scale, scale);

            MutableComponent prev = Component.translatable("area_control.screen.no_properties");
            int x2 = this.width / 2 - 7, dx2 = this.font.width(prev) / 2, y2 = this.height / 2 - 9;
            guiGraphics.drawString(this.font, prev, (int)(x2 / scale - dx2), (int)(y2 / scale), HINT_COLOR, false);

            guiGraphics.pose().popPose();
        }
    }

    private void drawArtifactName(GuiGraphics guiGraphics, Font font) {
        guiGraphics.pose().pushPose();
        float scale = ARTIFACT_SCALE_FACTOR;
        guiGraphics.pose().scale(scale, scale, scale);
        int x3 = this.width / 2 + 1, y3 = this.height / 2 - 82, dx = font.width(this.areaName) / 2;
        guiGraphics.drawString(font, Component.literal(this.areaName), (int)(x3 / scale - dx), (int)(y3 / scale), TEXT_COLOR, false);
        guiGraphics.pose().popPose();
    }

    private class BottomButton extends Button {
        private final boolean isRed;

        public BottomButton(int x, int y, boolean isRed, Button.OnPress onPress, Component title) {
            super(x, y, 51, 19, title, onPress, DEFAULT_NARRATION);
            this.isRed = isRed;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // render button texture
            int u0 = (this.isRed ? 7 : 60) + (this.isHovered ? 106 : 0), v0 = 234;
            guiGraphics.blit(TEXTURE, this.getX(), this.getY(), u0, v0, this.width, this.height, EditPropertiesScreen.TEXTURE_WIDTH, EditPropertiesScreen.TEXTURE_HEIGHT);
            // render button text
            float dx = EditPropertiesScreen.this.font.width(this.getMessage()) / 2F;
            float x = this.getX() + (this.width + 1) / 2F - dx, y = this.getY() + (this.height - 8) / 2F;
            guiGraphics.drawString(EditPropertiesScreen.this.font, this.getMessage(), (int)x, (int)y, BUTTON_TEXT_COLOR, false);
        }
    }

    private static class SideSlider extends AbstractWidget {
        private final ChangeListener changeListener;
        private final ClickListener clickListener;

        private final int halfSliderHeight;
        private final int totalHeight;

        private double slideCenter;

        public SideSlider(int x, int y, int totalHeight, ClickListener clickListener, ChangeListener changeListener, Component title) {
            super(x, y, 205, 132, title);
            this.totalHeight = totalHeight;
            this.clickListener = clickListener;
            this.changeListener = changeListener;
            this.halfSliderHeight = Mth.clamp(Math.round(132F / totalHeight * 60F), 10, 60);
            this.slideCenter = 6 + this.halfSliderHeight;
            changeListener.onChange(0, 132);
        }

        private void changeSlideCenter(double center) {
            int min = 6 + this.halfSliderHeight, max = 126 - this.halfSliderHeight;
            center = Mth.clamp(center, min, max);
            if (this.slideCenter != center) {
                double ratio = Mth.inverseLerp(this.slideCenter = center, min, max);
                int top = Math.toIntExact(Math.round(ratio * (this.totalHeight - 132)));
                this.changeListener.onChange(top, top + 132);
            }
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            double dx = mouseX - this.getX(), dy = mouseY - this.getY() - this.slideCenter;
            int x0 = this.getX() + 192, y0 = Math.toIntExact(Math.round(mouseY - dy));
            int v0 = this.isHovered && dx >= 192 && dy < this.halfSliderHeight && dy >= -this.halfSliderHeight ? 133 : 4;
            guiGraphics.blit(TEXTURE, x0, y0 - this.halfSliderHeight, 239, v0, 13, this.halfSliderHeight - 8, EditPropertiesScreen.TEXTURE_WIDTH, EditPropertiesScreen.TEXTURE_HEIGHT);
            guiGraphics.blit(TEXTURE, x0, y0 - 8, 239, v0 + 52, 13, 16, EditPropertiesScreen.TEXTURE_WIDTH, EditPropertiesScreen.TEXTURE_HEIGHT);
            guiGraphics.blit(TEXTURE, x0, y0 + 8, 239, v0 + 128 - this.halfSliderHeight, 13, this.halfSliderHeight - 8, EditPropertiesScreen.TEXTURE_WIDTH, EditPropertiesScreen.TEXTURE_HEIGHT);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (delta != 0) {
                this.changeSlideCenter(this.slideCenter - 12 * delta);
                return true;
            }
            return false;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            double dx = mouseX - this.getX(), dy = mouseY - this.getY();
            if (dx >= 192 && dy >= this.slideCenter + this.halfSliderHeight) {
                this.changeSlideCenter(this.slideCenter + 1);
            }
            if (dx >= 192 && dy < this.slideCenter - this.halfSliderHeight) {
                this.changeSlideCenter(this.slideCenter - 1);
            }
            if (dx >= 0 && dx < 192) {
                this.clickListener.onClick(dx, dy);
            }
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            double dx = mouseX - this.getX(), dy = mouseY - this.getY() - this.slideCenter;
            if (dx >= 192 && dy < this.halfSliderHeight && dy >= -this.halfSliderHeight) {
                this.changeSlideCenter(this.slideCenter + dragY);
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.playDownSound(Minecraft.getInstance().getSoundManager());
        }

        @Override
        public void playDownSound(@NotNull SoundManager handler) {
            // do nothing
        }

        @Override
        protected @NotNull MutableComponent createNarrationMessage() {
            return Component.translatable("gui.narrate.slider", this.getMessage());
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.createNarrationMessage());
            if (this.active) {
                if (this.isFocused()) {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused"));
                } else {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
                }
            }
        }

        @FunctionalInterface
        public interface ClickListener {
            void onClick(double dx, double dy);
        }

        @FunctionalInterface
        public interface ChangeListener {
            void onChange(int top, int bottom);
        }
    }
}
