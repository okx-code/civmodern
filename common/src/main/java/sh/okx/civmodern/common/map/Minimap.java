package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;

import static org.lwjgl.opengl.GL11.*;
import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class Minimap {

  private final MapCache cache;
  private final CivMapConfig config;
  private final ColourProvider provider;
  private float zoom = 1;


  public Minimap(MapCache cache, CivMapConfig config, ColourProvider provider) {
    this.cache = cache;
    this.config = config;
    this.provider = provider;
  }

  public void onRender(PostRenderGameOverlayEvent event) {
    if (!config.isMinimapEnabled()) {
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    if (mc.options.hideGui || mc.options.renderDebug) {
      return;
    }

    float size = config.getMinimapSize();

    PoseStack matrices = event.poseStack();

    matrices.pushPose();

    int offsetX = config.getMinimapX();
    int offsetY = config.getMinimapY();

    int translateX;
    int translateY;

    int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    switch (config.getMinimapAlignment()) {
      case TOP_LEFT -> {
        translateX = offsetX;
        translateY = offsetY;
      }
      case TOP_RIGHT -> {
        translateX = width - offsetX - config.getMinimapSize();
        translateY = offsetY;
      }
      case BOTTOM_RIGHT -> {
        translateX = width - offsetX - config.getMinimapSize();
        translateY = height - offsetY - config.getMinimapSize();
      }
      default -> {
        translateX = offsetX;
        translateY = height - offsetY - config.getMinimapSize();
      }
    }

    matrices.translate(translateX, translateY, 0);

    LocalPlayer player = Minecraft.getInstance().player;
    float px = (float) Mth.lerp(event.delta(), player.xo, player.getX());
    float pz = (float) Mth.lerp(event.delta(), player.zo, player.getZ());
    float x = px - (size * zoom) / 2;
    float y = pz - (size * zoom) / 2;

    float drawnX = 0;
    float drawnY = 0;
    for (float screenX = 0; screenX < (size * zoom) + SIZE; screenX += SIZE) {
      float tmp = 0;
      for (float screenY = 0; screenY < (size * zoom) + SIZE; screenY += SIZE) {
        float realX = x + screenX;
        float realY = y + screenY;

        float renderX = realX - floatMod(realX, SIZE);
        float renderY = realY - floatMod(realY, SIZE);

        RegionKey key = new RegionKey(Math.floorDiv((int)renderX, SIZE), Math.floorDiv((int)renderY, SIZE));
        RegionAtlasTexture texture = cache.getTexture(key);
        float xOff = (renderX - x) + 4096;
        float yOff = (renderY - y) + 4096;
        if (texture != null) {
          texture.drawLinear(matrices, drawnX, drawnY, zoom, screenX == 0 ? SIZE - xOff : 0, screenY == 0 ? SIZE - yOff : 0, SIZE, SIZE, Math.max(0, size * zoom - drawnX), Math.max(0, size * zoom - drawnY));
          drawnY += screenY == 0 ? yOff : SIZE;
        }
        tmp += xOff;
      }
      drawnY = 0;
      drawnX += screenX == 0 ? tmp / 2: SIZE;
    }

    matrices.pushPose();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    glEnable(GL_POLYGON_SMOOTH);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
    bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
    matrices.translate(size / 2, size / 2, 0);
    matrices.mulPose(Vector3f.ZP.rotationDegrees(player.getViewYRot(event.delta()) % 360f));
    matrices.scale(4, 4, 0);
    int chevronColour = provider.getChevronColour() | 0xFF000000;
    Matrix4f pose = matrices.last().pose();
    bufferBuilder.vertex(pose, -1, -1.5f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, -1, -1f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, 0, -0.5f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, 0, 0f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, 0, -0.5f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, 1, -1f, 0).color(chevronColour).endVertex();
    bufferBuilder.vertex(pose, 1, -1.5f, 0).color(chevronColour).endVertex();
    bufferBuilder.end();
    BufferUploader.end(bufferBuilder);
    glDisable(GL_POLYGON_SMOOTH);
    RenderSystem.disableBlend();
    matrices.popPose();

    matrices.popPose();
  }

  private float floatMod(float x, float y){
    // x mod y behaving the same way as Math.floorMod but with floats
    return (x - (float)Math.floor(x/y) * y);
  }

  public void cycleZoom() {
    zoom *= 2;
    if (zoom >= 32) {
      zoom = 0.5f;
    }
  }
}
