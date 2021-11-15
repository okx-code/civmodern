package sh.okx.civmodern.common.radar;

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.compat.CommonFont;
import sh.okx.civmodern.common.compat.CompatProvider;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;

public class Radar {

  private static final double COS_45 = 0.70710677D;
  private static final int PLAYER_RANGE = 70;

  private final EventBus eventBus;
  private final CivMapConfig config;
  private final CommonFont cFont;

  private Set<RemotePlayer> playersInRange = new HashSet<>();
  private String lastWaypointCommand;


  public Radar(CivMapConfig config, EventBus eventBus, CompatProvider compatProvider) {
    this.config = config;
    this.eventBus = eventBus;
    this.cFont = compatProvider.provideFont(Minecraft.getInstance().font);
  }

  public void init() {
    eventBus.listen(ClientTickEvent.class, this::onClientTick);
    eventBus.listen(ClientTickEvent.class, this::onWorldTickPing);
    eventBus.listen(PostRenderGameOverlayEvent.class, this::onRender);
  }

  public void onClientTick(ClientTickEvent event) {
    Minecraft client = Minecraft.getInstance();
    if (client.level == null) {
      this.playersInRange = Collections.emptySet();
    }
  }

  public void onWorldTickPing(ClientTickEvent event) {
    ClientLevel world = Minecraft.getInstance().level;
    if (world == null || !config.isPingEnabled()) {
      return;
    }

    Set<RemotePlayer> newPlayersInRange = new HashSet<>();
    for (Entity entity : world.entitiesForRendering()) {
      if (entity instanceof RemotePlayer) {
        RemotePlayer player = (RemotePlayer) entity;
        newPlayersInRange.add(player);
        if (!playersInRange.contains(player)) {
          BlockPos pos = player.blockPosition();
          lastWaypointCommand =
              "/newWaypoint x:" + pos.getX() + ",y:" + pos.getY() + ",z:" + pos.getZ() + ",name:"
                  + player.getScoreboardName();
          Minecraft.getInstance().player.displayClientMessage(
              new TranslatableComponent("civmodern.radar.enter",
                  player.getName(),
                  new TextComponent(Integer.toString(pos.getX()))
                      .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)),
                  new TextComponent(Integer.toString(pos.getY()))
                      .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)),
                  new TextComponent(Integer.toString(pos.getZ()))
                      .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)))
                  .setStyle(Style.EMPTY
                      .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, lastWaypointCommand))
                      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                          new TranslatableComponent("civmodern.radar.hover",
                              new KeybindComponent("key.civmodern.highlight"))))),
              false);

        }
      }
    }

    for (RemotePlayer player : playersInRange) {
      if (!newPlayersInRange.contains(player)) {
        BlockPos pos = player.blockPosition();
        lastWaypointCommand =
            "/newWaypoint x:" + pos.getX() + ",y:" + pos.getY() + ",z:" + pos.getZ() + ",name:"
                + player.getScoreboardName();
        Minecraft.getInstance().player.displayClientMessage(
            new TranslatableComponent("civmodern.radar.leave",
                player.getName(),
                new TextComponent(Integer.toString(pos.getX()))
                    .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)),
                new TextComponent(Integer.toString(pos.getY()))
                    .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)),
                new TextComponent(Integer.toString(pos.getZ()))
                    .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)))
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, lastWaypointCommand))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TranslatableComponent("civmodern.radar.hover",
                            new KeybindComponent("key.civmodern.highlight"))))),
            false);
      }
    }

    this.playersInRange = newPlayersInRange;
  }

  public void onRender(PostRenderGameOverlayEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.options.renderDebug) return;

		if (config.isRadarEnabled()) {
			render(event.getPoseStack(), event.getDelta());
		}
  }

  private int radius() {
    return config.getRadarSize();
  }

  public void render(PoseStack matrices, float delta) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();

    Minecraft client = Minecraft.getInstance();
    LocalPlayer player = client.player;
    glPushMatrix();

    int offsetX = config.getX();
    int offsetY = config.getY();
    int z = 1000;

    int height = client.getWindow().getGuiScaledHeight();
    int width = client.getWindow().getGuiScaledWidth();
    switch (config.getAlignment()) {
      case TOP_LEFT:
        glTranslatef(offsetX, offsetY, z);
        break;
      case TOP_RIGHT:
        glTranslatef(width - offsetX, offsetY, z);
        break;
      case BOTTOM_RIGHT:
        glTranslatef(width - offsetX, height - offsetY, z);
        break;
      case BOTTOM_LEFT:
        glTranslatef(offsetX, height - offsetY, z);
        break;
    }
    float yaw = player.getViewYRot(delta);
    glRotatef(-yaw, 0, 0, 1);

    renderCircleBackground();
    for (int i = 1; i <= config.getRadarCircles(); i++) {
      renderCircleBorder(radius() * (i / (double) config.getRadarCircles()));
    }
    //renderRange();
    renderLines();

    renderBoatsMinecarts(matrices, delta);
    renderPlayers(matrices, delta);

    glPopMatrix();
    glColor4f(1.0F, 1.0F, 1.0F, 1);

    RenderSystem.disableBlend();
    RenderSystem.disableDepthTest();
  }

  private void renderRange() {
    glLineWidth(1f);
    glEnable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_LINE_SMOOTH);
    glDisable(GL_LIGHTING);
    glColor4f(1, 1, 1, 1);

    // If config.range < 98, then cut off a bit
    double len = PLAYER_RANGE * (config.getRadarSize() / config.getRange());
    double corner = config.getRadarSize() * COS_45;
    len = Math.min(len, corner);

    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    buffer.begin(GL_LINES, DefaultVertexFormat.POSITION);

    buffer.vertex(-len, -len, 0f).endVertex();
    buffer.vertex(-len, len, 0f).endVertex();

    buffer.vertex(-len, len, 0f).endVertex();
    buffer.vertex(len, len, 0f).endVertex();

    buffer.vertex(len, len, 0f).endVertex();
    buffer.vertex(len, -len, 0f).endVertex();

    buffer.vertex(len, -len, 0f).endVertex();
    buffer.vertex(-len, -len, 0f).endVertex();

    tessellator.end();

    glDisable(GL_BLEND);
    glDisable(GL_LINE_SMOOTH);
    glEnable(GL_TEXTURE_2D);
  }

  private void renderBoatsMinecarts(PoseStack matrices, float delta) {
    Minecraft minecraft = Minecraft.getInstance();

    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (entity instanceof Boat) {
        Boat boat = (Boat) entity;

        double scale = config.getRadarSize() / config.getRange();

        double px = minecraft.player.xOld
            + (minecraft.player.getX() - minecraft.player.xOld) * delta;
        double pz = minecraft.player.zOld
            + (minecraft.player.getZ() - minecraft.player.zOld) * delta;
        double x = boat.xOld + (boat.getX() - boat.xOld) * delta;
        double z = boat.zOld + (boat.getZ() - boat.zOld) * delta;
        double dx = px - x;
        double dz = pz - z;
        if (dx * dx + dz * dz > config.getRange() * config.getRange()) {
          continue;
        }
        glEnable(GL_BLEND);
        glColor4f(1.0F, 1.0F, 1.0F, 1);

        glPushMatrix();
        glTranslated(dx * scale, dz * scale, 0);
        glRotatef(minecraft.player.getViewYRot(delta), 0, 0, 1);

        glPushMatrix();
        matrices.pushPose();
        matrices.scale(config.getIconSize(), config.getIconSize(), 0);
        minecraft.getTextureManager().bind(
            new ResourceLocation("textures/item/" + boat.getBoatType().getName() + "_boat.png"));
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.blit(matrices, -4, -4, 8, 8, 0, 0, 16, 16, 16, 16);
        matrices.popPose();

        glPopMatrix();
        glPopMatrix();
      } else if (entity instanceof Minecart) {
        Minecart minecart = (Minecart) entity;

        double scale = config.getRadarSize() / config.getRange();

        double px =
            minecraft.player.xOld + (minecraft.player.getX() - minecraft.player.xOld) * delta;
        double pz =
            minecraft.player.zOld + (minecraft.player.getZ() - minecraft.player.zOld) * delta;
        double x = minecart.xOld + (minecart.getX() - minecart.xOld) * delta;
        double z = minecart.zOld + (minecart.getZ() - minecart.zOld) * delta;
        double dx = px - x;
        double dz = pz - z;
        if (dx * dx + dz * dz > config.getRange() * config.getRange()) {
          continue;
        }
        glEnable(GL_BLEND);
        glColor4f(1.0F, 1.0F, 1.0F, 1);

        glPushMatrix();
        glTranslated(dx * scale, dz * scale, 0);
        glRotatef(minecraft.player.getViewYRot(delta), 0, 0, 1);

        glPushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        matrices.pushPose();
        matrices.scale(config.getIconSize(), config.getIconSize(), 0);
        minecraft.getTextureManager().bind(new ResourceLocation("textures/item/minecart.png"));
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.blit(matrices, -4, -4, 8, 8, 0, 0, 16, 16, 16, 16);
        matrices.popPose();

        glPopMatrix();
        glDisable(GL_BLEND);
        glPopMatrix();
      }
    }
  }

  private void renderPlayers(PoseStack matrices, float delta) {
    Minecraft minecraft = Minecraft.getInstance();

    glPushMatrix();

    for (RemotePlayer player : playersInRange) {
      if (!player.isAlive()) {
        continue;
      }
      double v = config.getRadarSize() / config.getRange();

      double px = minecraft.player.xOld + (minecraft.player.getX() - minecraft.player.xOld) * delta;
      double pz = minecraft.player.zOld + (minecraft.player.getZ() - minecraft.player.zOld) * delta;
      double x = player.xOld + (player.getX() - player.xOld) * delta;
      double z = player.zOld + (player.getZ() - player.zOld) * delta;
      double dx = px - x;
      double dz = pz - z;
      if (dx * dx + dz * dz > config.getRange() * config.getRange()) {
        continue;
      }
      glEnable(GL_BLEND);
      glColor4f(1.0F, 1.0F, 1.0F, 1);

      glPushMatrix();
      glTranslated(dx * v, dz * v, 0);
      glRotatef(minecraft.player.getViewYRot(delta), 0, 0, 1);

      PlayerInfo entry = minecraft.player.connection.getPlayerInfo(player.getUUID());
      glPushMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      matrices.pushPose();
      matrices.scale(config.getIconSize(), config.getIconSize(), 0);
      if (entry != null) {
        minecraft.getTextureManager().bind(entry.getSkinLocation());
      } else {
        minecraft.getTextureManager().bind(new ResourceLocation("textures/entity/steve.png"));
      }
      Gui.blit(matrices, -4, -4, 8, 8, 8.0F, 8, 8, 8, 64, 64);
      matrices.pushPose();
      glDisable(GL_BLEND);
      matrices.scale(0.6f, 0.6f, 0);
      TextComponent component = new TextComponent(
          player.getScoreboardName() + " (" + ((int) player.getY() + ")"));
      this.cFont.draw(matrices, component, -minecraft.font.width(component) / 2f, 7, 0xffffff);
      matrices.popPose();
      matrices.popPose();

      glPopMatrix();
      glPopMatrix();
    }

    glPopMatrix();
  }

  private void renderCircleBackground() {
    glLineWidth(1);
    glEnable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glColor4f((config.getRadarColour() >> 16 & 0xFF) / 255f,
        (config.getRadarColour() >> 8 & 0xFF) / 255f, (config.getRadarColour() & 0xFF) / 255f,
        1 - config.getTransparency());

    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    buffer.begin(GL_TRIANGLE_FAN, DefaultVertexFormat.POSITION);

    for (int i = 0; i <= 360; i++) {
      double x = Math.sin(i * Math.PI / 180.0D) * radius();
      double y = Math.cos(i * Math.PI / 180.0D) * radius();
      buffer.vertex(x, y, 0.0D).endVertex();
    }
    tessellator.end();

    glEnable(GL_TEXTURE_2D);
    glDisable(GL_BLEND);
  }

  private void renderCircleBorder(double radius) {
    glEnable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_POLYGON_SMOOTH);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    buffer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

    double thicknesss = radius == radius() ? 1 : 0.5;

    for (int i = 0; i <= 360; i++) {
      glColor4f((config.getRadarColour() >> 16 & 0xFF) / 255f,
          (config.getRadarColour() >> 8 & 0xFF) / 255f, (config.getRadarColour() & 0xFF) / 255f, 1);
      double x0 = Math.sin(i * Math.PI / 180.0D) * radius;
      double y0 = Math.cos(i * Math.PI / 180.0D) * radius;

      double x1 = Math.sin(i * Math.PI / 180.0D) * (radius + thicknesss);
      double y1 = Math.cos(i * Math.PI / 180.0D) * (radius + thicknesss);
      buffer.vertex(x0, y0, 0.0D).endVertex();
      buffer.vertex(x1, y1, 0).endVertex();
    }
    tessellator.end();

    glDisable(GL_POLYGON_SMOOTH);
    glEnable(GL_TEXTURE_2D);
    glDisable(GL_BLEND);
  }

  private void renderLines() {
    glLineWidth(1f);
    glEnable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_LINE_SMOOTH);
    glDisable(GL_LIGHTING);
    glColor4f((config.getRadarColour() >> 16 & 0xFF) / 255f,
        (config.getRadarColour() >> 8 & 0xFF) / 255f, (config.getRadarColour() & 0xFF) / 255f, 1);

    int scale = 0;
    float radius = radius() + 0.5f;
    double diagonalInner = COS_45;
    double diagonalOuter = COS_45 * radius;

    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    buffer.begin(GL_LINES, DefaultVertexFormat.POSITION);

    buffer.vertex(0, -radius, 0f).endVertex();
    buffer.vertex(0, radius, 0f).endVertex();

    buffer.vertex(-radius, 0, 0f).endVertex();
    buffer.vertex(radius, 0, 0f).endVertex();

    buffer.vertex(-diagonalOuter, -diagonalOuter, 0f).endVertex();
    buffer.vertex(diagonalOuter, diagonalOuter, 0f).endVertex();

    buffer.vertex(-diagonalOuter, diagonalOuter, 0f).endVertex();
    buffer.vertex(diagonalOuter, -diagonalOuter, 0f).endVertex();

    tessellator.end();

    glDisable(GL_BLEND);
    glDisable(GL_LINE_SMOOTH);
    glEnable(GL_TEXTURE_2D);
  }
}
