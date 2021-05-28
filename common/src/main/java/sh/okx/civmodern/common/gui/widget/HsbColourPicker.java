package sh.okx.civmodern.common.gui.widget;

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.gui.Texture;

public class HsbColourPicker extends AbstractWidget {

  private final Texture hueSelector;
  private final Texture saturationBrightnessTexture;
  private final Consumer<Integer> colourConsumer;

  private int hue = 0; // [0, 360]
  private int saturation = 0; // [0, 100]
  private int brightness = 0; // [0, 100]

  private boolean showPalette = false;
  private boolean updateTexture = true;
  private boolean hueMouseDown = false;


  public HsbColourPicker(int x, int y, int width, int height, int colour, Consumer<Integer> colourConsumer) {
    super(x, y, width, height, new TextComponent("HSB Colour Picker"));

    this.hue = Math.round(Color.RGBtoHSB(colour >> 16 & 0xFF, colour >> 8 & 0xFF, colour & 0xFF, null)[0] * 360);

    this.hueSelector = HsbColourPicker.getHueSelector();
    this.saturationBrightnessTexture = new Texture(128, 128);
    this.colourConsumer = colourConsumer;
  }

  @Override
  public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.getTextureManager().bind(new ResourceLocation("civmodern", "gui/colour.png"));
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
    Gui.blit(matrixStack, this.x, this.y, this.getBlitOffset(), 0, isHovered() ? 20 : 0, this.width,
        this.height, 40, 20);

    if (showPalette) {
      RenderSystem.enableTexture();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      if (this.updateTexture) {
        updateTexture(this.saturationBrightnessTexture, this.hue);
        this.updateTexture = false;
      } else {
        this.saturationBrightnessTexture.bind();
      }

      glPushMatrix();
      glTranslatef(0, 0, 1000);
      // Saturation and brightness selector
      Gui.blit(matrixStack, this.x, this.y + height, 0, 0, 0, 101, 101, 128, 128);

      // Hue selector
      hueSelector.bind();
      Gui.blit(matrixStack, this.x + 106, this.y + height, 10, 101, 0, 0, 1, 360, 1, 360);

      RenderSystem.disableTexture();
      // Render cursor
      glEnable(GL_LINE_SMOOTH);
      glLineWidth(2f);

      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(GL_LINES, DefaultVertexFormat.POSITION);

      double hueOffset = (this.hue / 360f) * 101;
      buffer.vertex(this.x + 106, this.y + height + hueOffset, 0f).endVertex();
      buffer.vertex(this.x + 116, this.y + height + hueOffset, 0f).endVertex();

      buffer.end();
      BufferUploader.end(buffer);
      glDisable(GL_LINE_SMOOTH);

      glPopMatrix();

      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
      RenderSystem.disableAlphaTest();

    }
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    showPalette = !showPalette;
  }

  @Override
  protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    if (this.hueMouseDown) {
      setHue(mouseX, mouseY, 0, true);
    }
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

  private boolean selectColour(double mouseX, double mouseY, int button) {
    if (active && visible && button == 0 && showPalette) {
      if (mouseX >= this.x && mouseX < this.x + 101
          && mouseY >= this.y + height && mouseY < this.y + height + 101) {
        this.saturation = (int) (mouseX - this.x);
        this.brightness = (int) (mouseY - this.y - height);
        colourConsumer.accept(toRgb());
        this.showPalette = false;
        return true;
      }
    }
    return false;
  }

  private boolean setHue(double mouseX, double mouseY, int button, boolean force) {
    // Cursor selector
    if (active && visible && button == 0 && showPalette) {
      if (!(mouseY >= this.y + height && mouseY <= this.y + height + 101)) {
        return false;
      }

      if (force || (mouseX >= this.x + 106 && mouseX <= this.x + 106 + 10)) {
        this.hueMouseDown = true;
        double yOffset = mouseY - (this.y + height);
        int newHue = (int) ((yOffset / 101) * 360);
        if (newHue != this.hue) {
          this.hue = newHue;
          this.updateTexture = true;
        }
        return true;
      }
    }
    return false;
  }

  private void updateTexture(Texture texture, int hue) {
    int[] rgbaValues = new int[128 * 128];
    for (int saturation = 0; saturation <= 100; saturation++) {
      for (int brightness = 0; brightness <= 100; brightness++) {
        int rgb = Color.HSBtoRGB(hue / 360f, saturation / 100f, brightness / 100f) & 0xFFFFFF;
        rgbaValues[(brightness * 128) + saturation] = rgb << 8 | 0xFF;
      }
    }
    texture.setPixels(rgbaValues);
    texture.update();
  }

  private int toRgb() {
    return Color.HSBtoRGB(this.hue / 360f, this.saturation / 100f, this.brightness / 100f) & 0xFFFFFF;
  }

  public static Texture getHueSelector() {
    Texture hueSelector = new Texture(1, 360);
    int[] rgbaValues = new int[360];
    for (int i = 0; i < 360; i++) {
      int rgb = Color.HSBtoRGB(i / 360f, 1, 1);
      rgbaValues[i] = rgb << 8 | 0xFF;
    }
    hueSelector.setPixels(rgbaValues);
    hueSelector.update();
    return hueSelector;
  }

  public void close() {
    this.showPalette = false;
  }
}
