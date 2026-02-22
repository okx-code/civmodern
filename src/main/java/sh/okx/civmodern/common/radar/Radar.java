package sh.okx.civmodern.common.radar;

import com.google.common.eventbus.Subscribe;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIPipelines;
import io.wispforest.owo.ui.renderstate.CircleElementRenderState;
import io.wispforest.owo.ui.renderstate.LineElementRenderState;
import io.wispforest.owo.ui.renderstate.RingElementRenderState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.rendering.CivModernPipelines;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Radar {
    private final ColourProvider colourProvider;
    private final CivMapConfig config;

    private Set<RemotePlayer> playersInRange = new HashSet<>();

    private int translateX;
    private int translateY;
    private int bgColour;
    private int fgColour;

    public Radar(CivMapConfig config, ColourProvider colourProvider) {
        this.config = config;
        this.colourProvider = colourProvider;
    }

    public static void playPlayerSound(String soundName, UUID playerKey) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace("block.note_block." + soundName));
        if (soundEvent == null) return;

        float pitch = .5f + 1.5f * new Random(playerKey.hashCode()).nextFloat();
        float volume = 1;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch, volume));
    }

    @Subscribe
    private void onClientTick(
        final @NotNull ClientTickEvent event
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            this.playersInRange = Collections.emptySet();
        }
    }

    @Subscribe
    private void onWorldTickPing(
        final @NotNull ClientTickEvent event
    ) {
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
                            "/civmodern_openwaypoint [x:" + pos.getX() + ",y:" + Minecraft.getInstance().player.getBlockY() + ",z:" + pos.getZ() + ",name:"
                                + player.getScoreboardName() + "]";

                        Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("civmodern.radar.enter",
                                    player.getName(),
                                    Component.literal(pos.getX() + " " + pos.getZ())
                                        .withStyle(ChatFormatting.AQUA))
                                .setStyle(Style.EMPTY
                                    .withClickEvent(
                                        new ClickEvent.RunCommand(lastWaypointCommand))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("civmodern.radar.hover")))),
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
                    // TODO: Replace with player waypoints
                    String lastWaypointCommand =
                        "/civmodern_openwaypoint [x:" + pos.getX() + ",y:" + Minecraft.getInstance().player.getBlockY() + ",z:" + pos.getZ() + ",name:"
                            + player.getScoreboardName() + "]";
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("civmodern.radar.leave",
                                player.getName(),
                                Component.literal(pos.getX() + " " + pos.getZ())
                                    .withStyle(s -> s.applyFormat(ChatFormatting.AQUA)))
                            .setStyle(Style.EMPTY
                                .withClickEvent(
                                    new ClickEvent.RunCommand(lastWaypointCommand))
                                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("civmodern.radar.hover")))),
                        false);
                }
            }
        }

        this.playersInRange = newPlayersInRange;
    }

    @Subscribe
    private void onRender(
        final @NotNull PostRenderGameOverlayEvent event
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.debugEntries.isOverlayVisible()) {
            return;
        }

        if (config.isRadarEnabled()) {
            render(event.guiGraphics(), event.deltaTick());
        }
    }

    private int radius() {
        return config.getRadarSize();
    }

    public void render(GuiGraphics guiGraphics, float delta) {
        bgColour = (colourProvider.getBackgroundColour() & 0xFF_FF_FF) | (int) ((1 - config.getBackgroundTransparency()) * 255) << 24;
        fgColour = (colourProvider.getForegroundColour() & 0xFF_FF_FF) | (int) ((1 - config.getTransparency()) * 255) << 24;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        guiGraphics.pose().pushMatrix();

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
        guiGraphics.pose().translate(translateX, translateY);
        renderCircleBackground(guiGraphics);
        for (int i = 1; i <= config.getRadarCircles(); i++) {
            renderCircleBorder(guiGraphics, radius() * (i / (float) config.getRadarCircles()));
        }
        if (config.isNorthUp()) {
            guiGraphics.pose().rotate((float) Math.toRadians(180));
//            renderAngle(guiGraphics.pose(), delta);
        } else {
            guiGraphics.pose().rotate((float) Math.toRadians((-player.getViewYRot(delta)) % 360f));
        }
        renderLines(guiGraphics);
//
        if (config.isShowItems()) {
            renderItems(guiGraphics, delta);
        }
        renderBoatsMinecarts(guiGraphics, delta);
        renderPlayers(guiGraphics, delta);

        guiGraphics.pose().popMatrix();
    }

    private void renderBoatsMinecarts(GuiGraphics guiGraphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof AbstractBoat boat) {
                renderEntity(guiGraphics, minecraft.player, boat, delta, boat.getPickResult(), 1.0f, 0.9f);
            } else if (entity instanceof Minecart minecart) {
                renderEntity(guiGraphics, minecraft.player, minecart, delta, new ItemStack(Items.MINECART, 1), 1.1f, 0.9f);
            }
        }
    }

    private void renderItems(GuiGraphics guiGraphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();

        int i = 0;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) {
                renderEntity(guiGraphics, minecraft.player, item, delta, item.getItem(), 0f, 0.9f);
                i++;
                if (i > 3_000) {
                    return;
                }
            }
        }
    }

    private double rescale(double dx, double dz) {
        if (!config.isRadarLogarithm()) {
            return 1;
        }
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.1) {
            return 1;
        }
        double ld = (Math.log1p(distance) / Math.log1p(config.getRange())) * config.getRange();
        return (1 / distance) * ld;
    }

    private void renderEntity(GuiGraphics guiGraphics, Player player, Entity entity, float delta, ItemStack item, float blit, float entityScale) {
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

        double logscale = rescale(dx, dz);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) (dx * scale * logscale), (float) (dz * scale * logscale));
        if (config.isNorthUp()) {
            guiGraphics.pose().rotate(180);
        } else {
            guiGraphics.pose().rotate((float) Math.toRadians(player.getViewYRot(delta)));
        }
        guiGraphics.pose().scale(config.getIconSize() * entityScale, config.getIconSize() * entityScale);

        guiGraphics.renderFakeItem(item, -8, -8);
        guiGraphics.pose().popMatrix();
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
            double logscale = rescale(dx, dz);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float) (dx * v * logscale), (float) (dz * v * logscale));
            guiGraphics.pose().scale(0.9f, 0.9f);

            PlayerInfo entry = minecraft.player.connection.getPlayerInfo(player.getUUID());
            if (config.isNorthUp()) {
                guiGraphics.pose().rotate((float) Math.toRadians(180));
            } else {
                guiGraphics.pose().rotate((float) Math.toRadians(minecraft.player.getViewYRot(delta)));
            }
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(config.getIconSize(), config.getIconSize());
            Identifier location;
            if (entry != null) {
                location = entry.getSkin().body().texturePath();
            } else {
                location = Identifier.withDefaultNamespace("textures/entity/steve.png");
            }
            PlayerFaceRenderer.draw(guiGraphics, location, -4, -4, 8, true, false, -1);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0, 4.5f * config.getIconSize());
            guiGraphics.pose().scale(0.6f * config.getTextSize(), 0.6f * config.getTextSize());
            Component component = Component.literal(
                player.getScoreboardName() + " (" + ((int) Math.round(Math.sqrt(dx * dx + dz * dz))) + ")");
            guiGraphics.drawCenteredString(minecraft.font, component, 0, 1, -1);

            guiGraphics.pose().popMatrix();
            guiGraphics.pose().popMatrix();
            guiGraphics.pose().popMatrix();
        }
    }

    private void renderCircleBackground(GuiGraphics graphics) {
        graphics.guiRenderState.submitGuiElement(new CircleElementRenderState(
            OwoUIPipelines.GUI_TRIANGLE_FAN,
            new Matrix3x2f(graphics.pose()),
            graphics.scissorStack.peek(),
            0, 0, 0, 360, 180, radius() + 0.5f, Color.ofArgb(bgColour)
        ));
    }

    private void renderCircleBorder(GuiGraphics graphics, float radius) {
        float thickness = radius == radius() ? 1f : 0.5f;
        graphics.guiRenderState.submitGuiElement(new RingElementRenderState(
            CivModernPipelines.GUI_TRIANGLE_STRIP_BLEND,
            new Matrix3x2f(graphics.pose()),
            graphics.scissorStack.peek(),
            0, 0, 0, 360, 180, radius, radius + thickness, Color.ofArgb(fgColour), Color.ofArgb(fgColour)
        ));
    }

    private void renderLines(GuiGraphics graphics) {
        float thickness = 0.5f;
        float radius = radius() + 0.5f;

        int numberOfLines = 4;
        float rotationRadians = (float) Math.PI / numberOfLines;
        graphics.pose().pushMatrix();
        for (int i = 0; i < numberOfLines; i++) {
            graphics.guiRenderState.submitGuiElement(new LineElementRenderState(
                CivModernPipelines.GUI_QUADS,
                new Matrix3x2f(graphics.pose()),
                graphics.scissorStack.peek(),
                (int) -radius, 0, (int) radius, 0, thickness, Color.ofArgb(fgColour)
            ));
            graphics.pose().rotate(rotationRadians);
        }
        graphics.pose().popMatrix();
    }
}
