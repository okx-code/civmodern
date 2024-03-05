package sh.okx.civmodern.common.radar;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;

import java.net.URL;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class Radar {

    private static boolean hideY;

    static {
        URL resource = Radar.class.getResource("/civmc");
        if (resource != null) {
            hideY = true;
        } else {
            hideY = false;
        }
    }

    private final EventBus eventBus;
    private final ColourProvider colourProvider;
    private final CivMapConfig config;

    private Set<RemotePlayer> playersInRange = new HashSet<>();

    private int translateX;
    private int translateY;
    private int bgColour;
    private int fgColour;

    public Radar(CivMapConfig config, EventBus eventBus, ColourProvider colourProvider) {
        this.config = config;
        this.eventBus = eventBus;
        this.colourProvider = colourProvider;
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

    private boolean hideY() {
        return hideY;
    }

    public void onWorldTickPing(ClientTickEvent event) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        Set<RemotePlayer> newPlayersInRange = new HashSet<>();
        for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof RemotePlayer) {
                RemotePlayer player = (RemotePlayer) entity;

                newPlayersInRange.add(player);
                if (!playersInRange.contains(player)) {
                    if (config.isPingEnabled()) {
                        BlockPos pos = player.blockPosition();
                        String lastWaypointCommand =
                            "/newWaypoint x:" + pos.getX() + ",y:" + (hideY() ? Minecraft.getInstance().player.getBlockY() : pos.getY()) + ",z:" + pos.getZ() + ",name:"
                                + player.getScoreboardName();

                        Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("civmodern.radar.enter",
                                    player.getName(),
                                    Component.literal(hideY() ? (pos.getX() + " " + pos.getZ()) : (pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                                        .withStyle(ChatFormatting.AQUA))
                                .setStyle(Style.EMPTY
                                    .withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, lastWaypointCommand))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("civmodern.radar.hover")))),
                            false);
                    }
                    if (config.isPingSoundEnabled()) {
                        playPlayerSound("pling", player.getUUID());
                    }
                }
            }
        }

        if (config.isPingEnabled()) {
            for (RemotePlayer player : playersInRange) {
                if (!newPlayersInRange.contains(player)) {
                    BlockPos pos = player.blockPosition();
                    String lastWaypointCommand =
                        "/newWaypoint x:" + pos.getX() + ",y:" + (hideY() ? Minecraft.getInstance().player.getBlockY() : pos.getY()) + ",z:" + pos.getZ() + ",name:"
                            + player.getScoreboardName();
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("civmodern.radar.leave",
                                player.getName(),
                                Component.literal(hideY() ? (pos.getX() + " " + pos.getZ()) : (pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                                    .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)))
                            .setStyle(Style.EMPTY
                                .withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, lastWaypointCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("civmodern.radar.hover")))),
                        false);
                }
            }
        }

        this.playersInRange = newPlayersInRange;
    }

    public void onRender(PostRenderGameOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        if (config.isRadarEnabled()) {
            render(event.getGuiGraphics(), event.getDelta());
        }
    }

    private int radius() {
        return config.getRadarSize();
    }

    public void render(GuiGraphics guiGraphics, float delta) {
        bgColour = (colourProvider.getBackgroundColour() & 0xFF_FF_FF) | (int) ((1 - config.getBackgroundTransparency()) * 255) << 24;
        fgColour = (colourProvider.getForegroundColour() & 0xFF_FF_FF) | (int) ((1 - config.getTransparency()) * 255) << 24;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        guiGraphics.pose().pushPose();

        int offsetX = config.getX() + config.getRadarSize();
        int offsetY = config.getY() + config.getRadarSize();

        int height = client.getWindow().getGuiScaledHeight();
        int width = client.getWindow().getGuiScaledWidth();
        switch (config.getAlignment()) {
            case TOP_LEFT -> {
                translateX = offsetX;
                translateY = offsetY;
            }
            case TOP_RIGHT -> {
                translateX = width - offsetX;
                translateY = offsetY;
            }
            case BOTTOM_RIGHT -> {
                translateX = width - offsetX;
                translateY = height - offsetY;
            }
            case BOTTOM_LEFT -> {
                translateX = offsetX;
                translateY = height - offsetY;
            }
        }
        guiGraphics.pose().translate(translateX, translateY, 100);
        renderCircleBackground(guiGraphics.pose());
        for (int i = 1; i <= config.getRadarCircles(); i++) {
            renderCircleBorder(guiGraphics.pose(), radius() * (i / (float) config.getRadarCircles()));
        }
        if (config.isNorthUp()) {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
            renderAngle(guiGraphics.pose(), delta);
        } else {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((-player.getViewYRot(delta)) % 360f));
        }
        renderLines(guiGraphics.pose());

        if (config.isShowItems()) {
            renderItems(guiGraphics, delta);
        }
        renderBoatsMinecarts(guiGraphics, delta);
        renderPlayers(guiGraphics, delta);

        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    private void renderBoatsMinecarts(GuiGraphics guiGraphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Boat boat) {
                renderEntity(guiGraphics, minecraft.player, boat, delta, boat.getPickResult(), 1.0f);
            } else if (entity instanceof Minecart minecart) {
                renderEntity(guiGraphics, minecraft.player, minecart, delta, new ItemStack(Items.MINECART, 1), 1.1f);
            }
        }
    }

    private void renderItems(GuiGraphics guiGraphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) {
                renderEntity(guiGraphics, minecraft.player, item, delta, item.getItem(), 0f);
            }
        }
    }

    private void renderEntity(GuiGraphics guiGraphics, Player player, Entity entity, float delta, ItemStack item, float blit) {
        double scale = config.getRadarSize() / config.getRange();

        double px = player.xOld + (player.getX() - player.xOld) * delta;
        double pz =
            player.zOld + (player.getZ() - player.zOld) * delta;
        double x = entity.xOld + (entity.getX() - entity.xOld) * delta;
        double z = entity.zOld + (entity.getZ() - entity.zOld) * delta;
        double dx = px - x;
        double dz = pz - z;
        if (dx * dx + dz * dz > config.getRange() * config.getRange()) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(dx * scale, dz * scale, 0);
        if (config.isNorthUp()) {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
        } else {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(player.getViewYRot(delta)));
        }
        guiGraphics.pose().scale(config.getIconSize(), config.getIconSize(), 0);

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(item, player.level(), player, 0);
        guiGraphics.pose().mulPoseMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        guiGraphics.pose().scale(16.0f, 16.0f, 16.0f);

        boolean notUseBlockLight = !bakedModel.usesBlockLight();

        if (notUseBlockLight)
            Lighting.setupForFlatItems();

        Minecraft.getInstance().getItemRenderer().render(item, ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, bakedModel);
        guiGraphics.bufferSource().endBatch();

        if (notUseBlockLight)
            Lighting.setupFor3DItems();

        guiGraphics.pose().popPose();
    }

    private void renderPlayers(GuiGraphics guiGraphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();

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
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(dx * v, dz * v, 0);
            guiGraphics.pose().scale(0.9f, 0.9f, 0);

            PlayerInfo entry = minecraft.player.connection.getPlayerInfo(player.getUUID());
            guiGraphics.pose().scale(config.getIconSize(), config.getIconSize(), 0);
            if (config.isNorthUp()) {
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(minecraft.player.getViewYRot(delta)));
            } else {
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
            }
            ResourceLocation location;
            if (entry != null) {
                location = entry.getSkin().texture();
            } else {
                location = new ResourceLocation("textures/entity/steve.png");
            }
            guiGraphics.blit(location, -4, -4, 8, 8, 8.0F, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            guiGraphics.pose().scale(0.6f, 0.6f, 0);
            Component component = Component.literal(
                player.getScoreboardName() + " (" + (hideY() ? ((int) Math.round(Math.sqrt(dx * dx + dz * dz))) : (int) player.getY()) + ")");
            minecraft.font.drawInBatch(component, -minecraft.font.width(component) / 2f, 7, 0xffffff, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 0);

            guiGraphics.pose().popPose();
        }
    }

    private void renderCircleBackground(PoseStack stack) {
        RenderSystem.lineWidth(1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        float radius = radius() + 0.5f;
        for (int i = 0; i <= 360; i++) {
            float x = (float) Math.sin(i * Math.PI / 180.0D) * radius;
            float y = (float) Math.cos(i * Math.PI / 180.0D) * radius;
            buffer.vertex(stack.last().pose(), x, y, 0).color(bgColour).endVertex();
        }
        tessellator.end();

        RenderSystem.disableBlend();
    }

    private void renderCircleBorder(PoseStack stack, float radius) {
        RenderSystem.enableBlend();
        glEnable(GL_POLYGON_SMOOTH);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float thickness = radius == radius() ? 1f : 0.5f;

        Matrix4f pose = stack.last().pose();
        for (int i = 0; i <= 360; i++) {
            float x0 = (float) Math.sin(i * Math.PI / 180.0D) * radius;
            float y0 = (float) Math.cos(i * Math.PI / 180.0D) * radius;

            float x1 = (float) Math.sin(i * Math.PI / 180.0D) * (radius + thickness);
            float y1 = (float) Math.cos(i * Math.PI / 180.0D) * (radius + thickness);
            buffer.vertex(pose, x0, y0, 0).color(fgColour).endVertex();
            buffer.vertex(pose, x1, y1, 0).color(fgColour).endVertex();
        }
        tessellator.end();

        glDisable(GL_POLYGON_SMOOTH);
        RenderSystem.disableBlend();
    }

    private void renderAngle(PoseStack matrixStack, float delta) {
        RenderSystem.enableBlend();
        glEnable(GL_POLYGON_SMOOTH);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float radius = radius() + 0.5f;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float thickness = 1f;
        float left = -thickness / 2;
        float right = thickness / 2;

        matrixStack.pushPose();
        Matrix4f last = matrixStack.last().pose();

        last.mul(Axis.ZP.rotationDegrees(Minecraft.getInstance().player.getViewYRot(delta)).get(new Matrix4f()));
        buffer.vertex(last, left, -radius, 0).color(fgColour).endVertex();
        buffer.vertex(last, left, 0, 0).color(fgColour).endVertex();
        buffer.vertex(last, right, 0, 0).color(fgColour).endVertex();
        buffer.vertex(last, right, -radius, 0).color(fgColour).endVertex();

        matrixStack.popPose();
        tesselator.end();
        glDisable(GL_POLYGON_SMOOTH);
        RenderSystem.disableBlend();
    }

    private void renderLines(PoseStack matrixStack) {
        RenderSystem.enableBlend();
        glEnable(GL_POLYGON_SMOOTH);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float radius = radius() + 0.5f;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float thickness = 0.5f;
        float left = -thickness / 2;
        float right = thickness / 2;

        matrixStack.pushPose();
        Matrix4f last = matrixStack.last().pose();
        int numberOfLines = 4;
        float rotationRadians = (float) Math.PI / numberOfLines;
        for (int i = 0; i < numberOfLines; i++) {
            buffer.vertex(last, left, -radius, 0f).color(fgColour).endVertex();
            buffer.vertex(last, left, radius, 0f).color(fgColour).endVertex();
            buffer.vertex(last, right, radius, 0f).color(fgColour).endVertex();
            buffer.vertex(last, right, -radius, 0f).color(fgColour).endVertex();
            Matrix4f mul = new Matrix4f();
            last.mul(Axis.ZP.rotation(rotationRadians).get(mul));
        }
        matrixStack.popPose();

        tesselator.end();

        glDisable(GL_POLYGON_SMOOTH);
        RenderSystem.disableBlend();
    }

    public static void playPlayerSound(String soundName, UUID playerKey) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("block.note_block." + soundName));
        if (soundEvent == null) return;

        float pitch = .5f + 1.5f * new Random(playerKey.hashCode()).nextFloat();
        float volume = 1;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch, volume));
    }
}
