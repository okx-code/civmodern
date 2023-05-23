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
import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class MapScreen extends Screen {

  private static final int BOAT_PREVIEW_LINE_COLOUR = 0xFFFF0000;

  private final AbstractCivModernMod mod;
  private final MapCache mapCache;
  private final BoatNavigation navigation;

  private double x;
  private double y;
  private static float zoom = 1; // blocks per pixel

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
    /*addRenderableWidget(new ImageButton(10, 10, 20, 20, new ResourceLocation("civmodern", "gui/boat.png"), imbg -> {
      this.boating = !boating;
    }));*/
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    float scale = (float) Minecraft.getInstance().getWindow().getGuiScale() * zoom;
    Window window = Minecraft.getInstance().getWindow();

    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT);

    for (int screenX = 0; screenX < (window.getWidth() * zoom) + SIZE; screenX += SIZE) {
      for (int screenY = 0; screenY < (window.getHeight() * zoom) + SIZE; screenY += SIZE) {
        float realX = (float) this.x + screenX;
        float realY = (float) this.y + screenY;

        float renderX = realX - floatMod(realX, SIZE);
        float renderY = realY - floatMod(realY, SIZE);

        RegionKey key = new RegionKey(Math.floorDiv((int) renderX, SIZE), Math.floorDiv((int) renderY, SIZE));
        // todo if loading at low zoom, only render downsampled version to save memory
        RegionAtlasTexture texture = mapCache.getTexture(key);
        if (texture != null) {
          texture.draw(matrices, renderX - (float) this.x, renderY - (float) this.y, scale);
        }
      }
    }

    LocalPlayer player = Minecraft.getInstance().player;
    float prx = (float) (player.getX() - this.x) / scale;
    float pry = (float) (player.getZ() - this.y) / scale;
    matrices.pushPose();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    glEnable(GL_POLYGON_SMOOTH);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
    bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
    matrices.translate(prx, pry, 0);
    matrices.scale(4, 4, 0);
    matrices.mulPose(Vector3f.ZP.rotationDegrees(player.getViewYRot(delta) % 360f));
    int chevron = 0xFF000000 | mod.getColourProvider().getChevronColour();
    Matrix4f pose = matrices.last().pose();
    bufferBuilder.vertex(pose, -1, -1.5f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, -1, -1f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, 0, -0.5f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, 0, 0f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, 0, -0.5f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, 1, -1f, 0).color(chevron).endVertex();
    bufferBuilder.vertex(pose, 1, -1.5f, 0).color(chevron).endVertex();
    bufferBuilder.end();
    BufferUploader.end(bufferBuilder);
    glDisable(GL_POLYGON_SMOOTH);
    RenderSystem.disableBlend();
    matrices.popPose();


    Queue<Vec2> dests = navigation.getDestinations();
    if (boating || !dests.isEmpty()) {
      RenderSystem.enableBlend();
      glEnable(GL_POLYGON_SMOOTH);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      List<Vec2> points = new ArrayList<>();
      float px;
      float pz;
      if (player.getVehicle() != null) {
        px = (float) Mth.lerp(delta, player.getVehicle().xOld, player.getVehicle().getX());
        pz = (float) Mth.lerp(delta, player.getVehicle().zOld, player.getVehicle().getZ());
      } else {
        px = (float) player.getX();
        pz = (float) player.getZ();
      }
      points.add(new Vec2(px, pz));
      points.addAll(dests);
      if (boating) {
        points.add(new Vec2(mouseX * scale + (float) x, mouseY * scale + (float) y));
      }

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
      if (zoom < 32) {
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
      if (zoom > 0.03125) {

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

  private float floatMod(float x, float y){
    // x mod y behaving the same way as Math.floorMod but with floats
    return (x - (float)Math.floor(x/y) * y);
  }
}
