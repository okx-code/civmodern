package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.boat.BoatNavigation;
import sh.okx.civmodern.common.gui.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.*;

public class MapScreen extends Screen {

  private static final int BOAT_PREVIEW_LINE_COLOUR = 0xFFFF0000;

  private final AbstractCivModernMod mod;
  private final MapCache mapCache;
  private final BoatNavigation navigation;

  private double x;
  private double y;
  private float zoom = 1; // blocks per pixel
  // TODO fix x and y when zooming based on cursor position

  private boolean boating = false;

  public MapScreen(AbstractCivModernMod mod, MapCache mapCache, BoatNavigation navigation) {
    super(new TranslatableComponent("civmodern.screen.map.title"));
    this.mod = mod;
    this.mapCache = mapCache;
    Window window = Minecraft.getInstance().getWindow();

    x = Minecraft.getInstance().player.getX() - (window.getWidth() * zoom) / 2;
    y = Minecraft.getInstance().player.getZ() - (window.getHeight() * zoom) / 2;
    this.navigation = navigation;
  }

  @Override
  protected void init() {
    addRenderableWidget(new ImageButton(10, 10, 20, 20, new ResourceLocation("civmodern", "gui/boat.png"), imbg -> {
      this.boating = !boating;
    }));
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    float scale = (float) Minecraft.getInstance().getWindow().getGuiScale() * zoom;
    Window window = Minecraft.getInstance().getWindow();

    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT);

    int SIZE = 4096;

    for (int screenX = 0; screenX < (window.getWidth() * zoom) + SIZE; screenX += SIZE) {
      for (int screenY = 0; screenY < (window.getHeight() * zoom) + SIZE; screenY += SIZE) {
        int realX = (int) this.x + screenX;
        int realY = (int) this.y + screenY;

        int renderX = realX - Math.floorMod(realX, SIZE);
        int renderY = realY - Math.floorMod(realY, SIZE);

        RegionKey key = new RegionKey(Math.floorDiv(renderX, SIZE), Math.floorDiv(renderY, SIZE));
        // todo if loading at low zoom, only render downsampled version to save memory
        RegionAtlasTexture texture = mapCache.getTexture(key);
        if (texture != null) {
          texture.draw(matrices, (float) ((renderX - this.x)), (float) ((renderY - this.y)), scale);
        }
      }
    }

    Queue<Vec2> dests = navigation.getDestinations();
    if (boating || !dests.isEmpty()) {
      RenderSystem.enableBlend();
      glEnable(GL_POLYGON_SMOOTH);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      LocalPlayer player = Minecraft.getInstance().player;
//    double playerX = player.getX() / scale;
//    double playerZ = player.getZ() / scale;
//    double screenPlayerX = (playerX - (x / scale));
//    double screenPlayerZ = (playerZ - (y / scale));
//    double dx = mouseX - screenPlayerX;
//    double dy = mouseY - screenPlayerZ;
//    float dist = (float) Mth.length(dx, dy) + 0.5f;

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      List<Vec2> points = new ArrayList<>();
      points.add(new Vec2((float) player.getX(), (float) player.getZ()));
      points.addAll(dests);
      points.add(new Vec2(mouseX * scale + (float) x, mouseY * scale + (float) y));

      for (int i = 0; i < points.size() - 1; i++) {
        Vec2 from = points.get(i);
        Vec2 to = points.get(i + 1);

        double dx = (to.x - x) / scale - (from.x - x) / scale;
        double dy = (to.y - y) / scale - (from.y - y) / scale;
        float dist = (float) Mth.length(dx, dy) + 0.5f;

        matrices.pushPose();
        Matrix4f last = matrices.last().pose();
        matrices.translate((to.x - x) / scale, (to.y - y) / scale, 0);
        last.multiply(Vector3f.ZP.rotation((float) Mth.atan2(dx, -dy)));
        buffer.vertex(last, -0.5f, 0, i / 255f).color(BOAT_PREVIEW_LINE_COLOUR).endVertex();
        buffer.vertex(last, -0.5f, dist, i / 255f).color(BOAT_PREVIEW_LINE_COLOUR).endVertex();
        buffer.vertex(last, 0.5f, dist, i / 255f).color(BOAT_PREVIEW_LINE_COLOUR).endVertex();
        buffer.vertex(last, 0.5f, 0, i / 255f).color(BOAT_PREVIEW_LINE_COLOUR).endVertex();
        matrices.popPose();
      }

      tesselator.end();
      glDisable(GL_POLYGON_SMOOTH);
      RenderSystem.disableBlend();
    }

    super.render(matrices, mouseX, mouseY, delta);
  }

  @Override
  public void onClose() {
    super.onClose();
    mapCache.save();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

   if (boating && button == 1) {
      Window window = Minecraft.getInstance().getWindow();
      float scale = (float) window.getGuiScale() * zoom;

      double mouseWorldX = (mouseX * scale + x);
      double mouseWorldY = (mouseY * scale + y);
      if (Screen.hasShiftDown()) {
        this.navigation.addDestination(new Vec2((float) mouseWorldX, (float) mouseWorldY));
      } else {
        this.navigation.setDestination(new Vec2((float) mouseWorldX, (float) mouseWorldY));
        Minecraft.getInstance().setScreen(null);
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollDir) {
    if (super.mouseScrolled(mouseX, mouseY, scrollDir)) {
      return true;
    }

    if (scrollDir < 0) {
      // zoom out
      if (zoom < 128) {
        Window window = Minecraft.getInstance().getWindow();
        float scale = (float) window.getGuiScale() * zoom;

        double centreX = x + window.getWidth() * zoom * 0.5;
        double centreY = y + window.getHeight() * zoom * 0.5;

        double mouseWorldX = (mouseX * scale + x);
        double mouseWorldY = (mouseY * scale + y);

        zoom *= 2;

        x = (mouseWorldX - (mouseWorldX - centreX) / 0.5 - (window.getWidth() * zoom) / 2);
        y = (mouseWorldY - (mouseWorldY - centreY) / 0.5 - (window.getHeight() * zoom) / 2);
      }
    } else {
      // zoom in
      if (zoom > 0.0625) {

        Window window = Minecraft.getInstance().getWindow();
        float scale = (float) window.getGuiScale() * zoom;

        double centreX = x + window.getWidth() * zoom * 0.5;
        double centreY = y + window.getHeight() * zoom * 0.5;

        double mouseWorldX = (mouseX * scale + x);
        double mouseWorldY = (mouseY * scale + y);

        zoom /= 2;

        x = (mouseWorldX - (mouseWorldX - centreX) * 0.5 - (window.getWidth() * zoom) / 2);
        y = (mouseWorldY - (mouseWorldY - centreY) * 0.5 - (window.getHeight() * zoom) / 2);
      }
    }
    return true;
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double changeX, double changeY) {
    if (super.mouseDragged(x, y, button, changeX, changeY)) {
      return true;
    }

    if (button == 0) {
      double scale = Minecraft.getInstance().getWindow().getGuiScale() * zoom;
      this.x -= changeX * scale;
      this.y -= changeY * scale;
      return true;
    }
    return false;
    // 0 = left
    // 1 = right
    // 2 = middle
  }
}
