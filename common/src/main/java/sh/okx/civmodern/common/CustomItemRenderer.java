package sh.okx.civmodern.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class CustomItemRenderer extends ItemRenderer {

  private final ColourProvider colourProvider;

  public CustomItemRenderer(ItemRenderer old, ColourProvider colourProvider) throws IllegalAccessException {
    // Placeholder values just so it doesn't NPE
    super(Minecraft.getInstance().getTextureManager(), Minecraft.getInstance().getModelManager(),
        null, null);
    this.colourProvider = colourProvider;

    // Steal all the fields from the old item renderer
    // This is necessary otherwise all the items render with the missing texture
    for (Field field : ItemRenderer.class.getDeclaredFields()) {
      if (field.getType() == ItemModelShaper.class
          || field.getType() == ItemColors.class
          || field.getType() == BlockEntityWithoutLevelRenderer.class) {
        field.setAccessible(true);
        field.set(this, field.get(old));
      }
    }
  }

  @Override
  public void renderGuiItemDecorations(Font renderer, ItemStack stack, int x, int y, String label) {
    if (!stack.isEmpty()) {
      PoseStack matrixstack = new PoseStack();
      boolean compacted = isCompacted(stack);

      if (stack.isDamaged()) {
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        //RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        float f = (float)stack.getDamageValue();
        float g = (float)stack.getMaxDamage();
        float h = Math.max(0.0F, (g - f) / g);
        int k = Math.round(13.0F - f * 13.0F / g);
        int l = Mth.hsvToRgb(h / 3.0F, 1.0F, 1.0F);
        this.fillRect(bufferBuilder, x + 2, y + 13, 13, 2, 0, 0, 0, 255);
        this.fillRect(bufferBuilder, x + 2, y + 13, k, 1, l >> 16 & 255, l >> 8 & 255, l & 255, 255);
        RenderSystem.enableBlend();
        //RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
      }

      if (stack.getCount() != 1 || label != null || compacted) {
        matrixstack.translate(0.0D, 0.0D, this.blitOffset + 200.0F);
        MultiBufferSource.BufferSource irendertypebuffer$impl = MultiBufferSource.immediate(
            Tesselator.getInstance().getBuilder());

        int colour = compacted ? colourProvider.getCompactedColour() : 0xffffff;
        String s = label == null ? String.valueOf(stack.getCount()) : label;

        renderer.drawInBatch(s, (float) (x + 19 - 2 - renderer.width(s)), (float) (y + 6 + 3),
            colour, true, matrixstack.last().pose(), irendertypebuffer$impl, false, 0, 15728880);
        irendertypebuffer$impl.endBatch();
      }

      LocalPlayer localPlayer = Minecraft.getInstance().player;
      float m = localPlayer == null ? 0.0F : localPlayer.getCooldowns().getCooldownPercent(stack.getItem(), Minecraft.getInstance().getFrameTime());
      if (m > 0.0F) {
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Tesselator tesselator2 = Tesselator.getInstance();
        BufferBuilder bufferBuilder2 = tesselator2.getBuilder();
        this.fillRect(bufferBuilder2, x, y + Mth.floor(16.0F * (1.0F - m)), 16, Mth.ceil(16.0F * m), 255, 255, 255, 127);
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
      }
    }
  }

  private void fillRect(BufferBuilder buffer, int a, int b, int c, int d, int e, int f, int g,
      int h) {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    buffer.vertex(a, b, 0.0D).color(e, f, g, h).endVertex();
    buffer.vertex(a, b + d, 0.0D).color(e, f, g, h).endVertex();
    buffer.vertex(a + c, b + d, 0.0D).color(e, f, g, h).endVertex();
    buffer.vertex(a + c, b, 0.0D).color(e, f, g, h).endVertex();
    buffer.end();
    BufferUploader.end(buffer);
  }

  private boolean isCompacted(ItemStack item) {
    if (!item.hasTag()) {
      return false;
    }
    CompoundTag displayTag = item.getTagElement("display");
    if (displayTag != null && displayTag.getTagType("Lore") == 9) {
      ListTag listTag = displayTag.getList("Lore", 8);

      for (int i = 0; i < listTag.size(); i++) {
        String lore = listTag.getString(i);
        if (lore.contains("Compacted Item")) {
          return true;
        }
      }
    }
    return false;
  }
}
