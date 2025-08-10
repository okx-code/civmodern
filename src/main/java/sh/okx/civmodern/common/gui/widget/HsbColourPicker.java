package sh.okx.civmodern.common.gui.widget;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import io.wispforest.owo.ui.core.OwoUIPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import sh.okx.civmodern.common.gui.Texture;
import sh.okx.civmodern.common.rendering.CivModernPipelines;

import java.util.function.Consumer;

public class HsbColourPicker extends AbstractWidget {

    private static final ResourceLocation COLOUR_PICKER_ICON = ResourceLocation.fromNamespaceAndPath("civmodern", "gui/colour.png");

    private final GlTexture hueSelector;
    private final GpuTextureView hueSelectorTextureView;
    private final GlTexture saturationBrightnessTexture;
    private final GpuTextureView saturationBrightnessTextureView;
    private final Consumer<Integer> colourConsumer;
    private final Consumer<Integer> previewConsumer;

    private int hue = 0; // [0, 360]
    //private int saturation = 0; // [0, 100]
    //private int brightness = 0; // [0, 100]

    private boolean mouseOverGrid = false;

    private boolean showPalette = false;
    private boolean updateTexture = true;
    private boolean hueMouseDown = false;

    private final Runnable closeable;

    private int renderY;

    private boolean rvisible = true;

    public HsbColourPicker(int x, int y, int width, int height, int colour, Consumer<Integer> colourConsumer, Consumer<Integer> previewConsumer, Runnable closeable) {
        super(x, y, width, height, Component.literal("HSB Colour Picker"));

        this.hue = getHue(colour);

        this.closeable = closeable;
        this.hueSelector = getHueSelector();
        this.hueSelectorTextureView = RenderSystem.getDevice().createTextureView(this.hueSelector);
        this.saturationBrightnessTexture = (GlTexture) RenderSystem.getDevice().createTexture("satb", 5, TextureFormat.RGBA8, 101, 101, 1, 1);
        this.saturationBrightnessTextureView = RenderSystem.getDevice().createTextureView(this.saturationBrightnessTexture);
        this.colourConsumer = colourConsumer;
        this.previewConsumer = previewConsumer;
    }

    public void setRVisible(boolean rvisible) {
        this.rvisible = rvisible;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!rvisible) {
            return;
        }
        // Render colour picker above button if it would exceed the screen height otherwise
        this.renderY = (this.getY() + 101 > Minecraft.getInstance().getWindow().getGuiScaledHeight()) ? this.getY() - 101 - this.height : this.getY();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, COLOUR_PICKER_ICON, this.getX(), this.getY(), 0, isHoveredOrFocused() ? 20 : 0, this.width,
            this.height, 20, 40, -1);

        if (showPalette) {
            if (this.updateTexture) {
                updateTexture(this.hue);
                this.updateTexture = false;
            }

            guiGraphics.pose().pushMatrix();
            // Saturation and brightness selector

            guiGraphics.pose().translate(this.getX(), this.renderY + height);
            guiGraphics.guiRenderState
                .submitGuiElement(
                    new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED,
                        TextureSetup.singleTexture(this.saturationBrightnessTextureView),
                        new Matrix3x2f(guiGraphics.pose()), 0, 0, 101, 101, 0, 1f, 0, 1f, -1, guiGraphics.scissorStack.peek()
                    )
                );

            guiGraphics.guiRenderState
                .submitGuiElement(
                    new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED,
                        TextureSetup.singleTexture(this.hueSelectorTextureView),
                        new Matrix3x2f(guiGraphics.pose()), 106, 0, 116, 101, 0, 1, 0, 1, -1, guiGraphics.scissorStack.peek()
                    )
                );

            float hueOffset = (this.hue / 360f) * 100;
            int cursorX = 106;
            int cursorY = (int) hueOffset;
            guiGraphics.fill(cursorX, cursorY, cursorX + 10, cursorY + 1, 0xFFFFFFFF);
            guiGraphics.pose().popMatrix();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (previewConsumer != null) {
            if (active && visible && showPalette && isOverGrid(mouseX, mouseY)) {
                int saturation = (int) (mouseX - this.getX());
                int brightness = (int) (mouseY - renderY - height);
                mouseOverGrid = true;
                previewConsumer.accept(toRgb(hue, saturation, brightness));
            } else if (mouseOverGrid) {
                previewConsumer.accept(null);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!showPalette) {
            closeable.run();
        }
        showPalette = !showPalette;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.hueMouseDown) {
            setHue(mouseX, mouseY, 0, true);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        return active && visible && showPalette && (isOverGrid(mouseX, mouseY) || (mouseY >= renderY + height && mouseY <= renderY + height + 101 && mouseX >= this.getX() + 106 && mouseX <= this.getX() + 106 + 10));
    }

    @Override
    public void onRelease(double d, double e) {
        this.hueMouseDown = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return selectColour(mouseX, mouseY, button)
            || setHue(mouseX, mouseY, button, false)
            || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    private boolean selectColour(double mouseX, double mouseY, int button) {
        if (active && visible && button == 0 && showPalette && isOverGrid(mouseX, mouseY)) {
            int saturation = (int) (mouseX - this.getX());
            int brightness = (int) (mouseY - renderY - height);
            colourConsumer.accept(toRgb(hue, saturation, brightness));
            this.showPalette = false;
            return true;
        }
        return false;
    }

    private boolean setHue(double mouseX, double mouseY, int button, boolean force) {
        // Cursor selector
        if (active && visible && button == 0 && showPalette) {
            if (!force && !(mouseY >= renderY + height && mouseY <= renderY + height + 101)) {
                return false;
            }

            if (force || (mouseX >= this.getX() + 106 && mouseX <= this.getX() + 106 + 10)) {
                this.hueMouseDown = true;
                double yOffset = mouseY - (renderY + height);
                int newHue = Mth.clamp((int) ((yOffset / 102) * 360), 0, 360);
                if (newHue != this.hue) {
                    this.hue = newHue;
                    this.updateTexture = true;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isOverGrid(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + 101
            && mouseY >= renderY + height && mouseY < renderY + height + 101;
    }

    private void updateTexture(int hue) {
        NativeImage n = new NativeImage(101, 101, false);
        for (int saturation = 0; saturation <= 100; saturation++) {
            for (int brightness = 0; brightness <= 100; brightness++) {
                int rgb = toRgb(hue, saturation, brightness) & 0xFFFFFF;
                n.setPixel(saturation, brightness, 0xff << 24 | rgb);
            }
        }
        saturationBrightnessTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
        saturationBrightnessTexture.setUseMipmaps(false);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(saturationBrightnessTexture, n);
    }

    private int toRgb(int hue, int sat, int bright) {
        double[] rgbArr = HUSLColorConverter.hsluvToRgb(new double[]{hue, sat, bright});
        return ((int) (rgbArr[0] * 255) << 16) | ((int) (rgbArr[1] * 255) << 8) | ((int) (rgbArr[2] * 255));
    }

    private int getHue(int colour) {
        int r = colour >> 16 & 0xFF;
        int g = colour >> 8 & 0xFF;
        int b = colour & 0xFF;
        return (int) Math.round(HUSLColorConverter.rgbToHsluv(new double[]{r / 255d, g / 255d, b / 255d})[0]);
    }

    private GlTexture getHueSelector() {
        GlTexture hueSelector = (GlTexture) RenderSystem.getDevice().createTexture("hue", 5, TextureFormat.RGBA8, 1, 360, 1, 1);
        int[] rgbaValues = new int[360];
        for (int i = 0; i < 360; i++) {
            int rgb = toRgb(i, 100, 50);
            rgbaValues[i] = rgb << 8 | 0xFF;
        }
        Texture t = new Texture(hueSelector.glId(), 1, 360);
        t.setPixels(rgbaValues);
        t.update();
        return hueSelector;
    }

    public void close() {
        this.showPalette = false;
        this.saturationBrightnessTextureView.close();
    }
}
