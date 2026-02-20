package sh.okx.civmodern.common.gui.screen;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.gui.Alignment;
import sh.okx.civmodern.common.gui.DoubleValue;
import sh.okx.civmodern.common.gui.widget.ColourTextEditBox;
import sh.okx.civmodern.common.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.gui.widget.TextRenderable;
import sh.okx.civmodern.common.gui.widget.ToggleButton;
import sh.okx.civmodern.common.mixins.ScreenAccessor;

final class RadarConfigScreen extends AbstractConfigScreen {
    public static final Identifier ROLLBACK_ICON = Identifier.tryBuild("civmodern", "gui/rollback.png");

    private final ColourProvider colourProvider;

    // for passing move events
    private HsbColourPicker bgPicker;
    private HsbColourPicker fgPicker;

    public RadarConfigScreen(
        final @NotNull CivMapConfig config,
        final @NotNull ColourProvider colourProvider,
        final @NotNull MainConfigScreen parent
    ) {
        super(
            config,
            Objects.requireNonNull(parent),
            Component.translatable("civmodern.screen.radar.title")
        );
        this.colourProvider = Objects.requireNonNull(colourProvider);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableOnly(new TextRenderable.CentreAligned(
            this.font,
            this.centreX,
            getHeaderY(),
            this.title
        ));

        final int leftSideX = this.centreX - 5 - Button.DEFAULT_WIDTH;
        final int rightSideX = this.centreX + 5;

        int offsetY = getBodyY(this.height / 8);

        // Colour pickers must be renderered first as they are displayed on top of other buttons
        this.bgPicker = addColourPicker(
            leftSideX,
            offsetY + 190,
            CivMapConfig.DEFAULT_RADAR_BG_COLOUR,
            Component.literal("Background colour"),
            this.config::getRadarBgColour,
            this.config::setRadarBgColour,
            this.colourProvider::setTemporaryRadarBackgroundColour
        );
        this.fgPicker = addColourPicker(
            rightSideX,
            offsetY + 190,
            CivMapConfig.DEFAULT_RADAR_FG_COLOUR,
            Component.literal("Line colour"),
            this.config::getRadarColour,
            this.config::setRadarColour,
            this.colourProvider::setTemporaryRadarForegroundColour
        );

        addRenderableWidget(new ToggleButton(
            this.centreX - (Button.DEFAULT_WIDTH / 2),
            offsetY,
            Button.DEFAULT_WIDTH,
            Component.translatable("civmodern.screen.radar.enabled"),
            this.config::isRadarEnabled,
            this.config::setRadarEnabled,
            Tooltip.create(Component.translatable("civmodern.screen.radar.enabled.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new ToggleButton(
            leftSideX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.radar.messages"),
            this.config::isPingEnabled,
            this.config::setPingEnabled,
            Tooltip.create(Component.translatable("civmodern.screen.radar.messages.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        addRenderableWidget(new ToggleButton(
            rightSideX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.radar.pings"),
            this.config::isPingSoundEnabled,
            this.config::setPingSoundEnabled,
            Tooltip.create(Component.translatable("civmodern.screen.radar.pings.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("civmodern.screen.radar.alignment", this.config.getAlignment().toString()),
                    (button) -> {
                        final Alignment next = this.config.getAlignment().next();
                        this.config.setAlignment(next);
                        button.setMessage(Component.translatable("civmodern.screen.radar.alignment", next.toString()));
                    }
                )
                .pos(leftSideX, offsetY)
                .build()
        );
        addRenderableWidget(new ToggleButton(
            rightSideX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.radar.items"),
            this.config::isShowItems,
            this.config::setShowItems,
            Tooltip.create(Component.translatable("civmodern.screen.radar.items.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            rightSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0, 1,
            new DoubleValue() {
                private final DecimalFormat format = new DecimalFormat("##%");
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getTransparency();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setTransparency((float) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.transparency", this.format.format(value));
                }
            }
        ));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            leftSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0, 1,
            new DoubleValue() {
                private final DecimalFormat format = new DecimalFormat("##%");
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getBackgroundTransparency();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setBackgroundTransparency((float) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.background_transparency", this.format.format(value));
                }
            }
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            leftSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0.5, 2,
            new DoubleValue() {
                private final DecimalFormat format = new DecimalFormat("#.#");
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getIconSize();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setIconSize((float) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.iconsize", this.format.format(value));
                }
            }
        ));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            rightSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0, 2,
            new DoubleValue() {
                private final DecimalFormat format = new DecimalFormat("#.#");
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getTextSize();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setTextSize((float) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.textsize", this.format.format(value));
                }
            }
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            leftSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            25, 250,
            new DoubleValue() {
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getRadarSize();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setRadarSize((int) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.size", Integer.toString((int) value));
                }
            }
        ));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            rightSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            1, 8,
            new DoubleValue() {
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getRadarCircles();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setRadarCircles((int) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.circles", Integer.toString((int) value));
                }
            }
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            leftSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0, 300,
            new DoubleValue() {
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getX();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setX((int) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.x", String.valueOf((int) value));
                }
            }
        ));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            rightSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            0, 300,
            new DoubleValue() {
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getY();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setY((int) value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.y", String.valueOf((int) value));
                }
            }
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(
            leftSideX,
            offsetY,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            20, 150,
            new DoubleValue() {
                @Override
                public double get() {
                    return RadarConfigScreen.this.config.getRange();
                }
                @Override
                public void set(final double value) {
                    RadarConfigScreen.this.config.setRange(value);
                }
                @Override
                public @NotNull Component getText(final double value) {
                    return Component.translatable("civmodern.screen.radar.range", String.valueOf((int) value));
                }
            }
        ));
        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("civmodern.screen.radar.log", this.config.isRadarLogarithm() ? Component.translatable("civmodern.screen.radar.log.logarithmic") : Component.translatable("civmodern.screen.radar.log.linear")),
                    (button) -> {
                        this.config.setRadarLogarithm(!this.config.isRadarLogarithm());
                        button.setMessage(Component.translatable("civmodern.screen.radar.log", this.config.isRadarLogarithm() ? Component.translatable("civmodern.screen.radar.log.logarithmic") : Component.translatable("civmodern.screen.radar.log.linear")));
                    }
                )
                .pos(rightSideX, offsetY)
                .build());
        offsetY += Button.DEFAULT_HEIGHT + 4;

        offsetY += 30 + 2;

        addRenderableWidget(
            Button
                .builder(
                    CommonComponents.GUI_DONE,
                    (button) -> {
                        this.config.save();
                        Minecraft.getInstance().setScreen(this.parent);
                    }
                )
                .pos(this.centreX - (Button.DEFAULT_WIDTH / 2), getFooterY(offsetY))
                .build()
        );
    }

    private @NotNull HsbColourPicker addColourPicker(
        final int offsetX,
        int offsetY,
        final int defaultColour,
        final @NotNull Component label,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter,
        final @NotNull Consumer<Integer> preview
    ) {
        final int innerCenterX = offsetX + (Button.DEFAULT_WIDTH / 2);

        addRenderableOnly(new TextRenderable.CentreAligned(
            this.font,
            innerCenterX,
            offsetY,
            label
        ));
        offsetY += this.font.lineHeight + 2;

        final var colourEditBox = addRenderableWidget(new ColourTextEditBox(
            this.font,
            innerCenterX - 30,
            offsetY,
            60,
            Button.DEFAULT_HEIGHT,
            colourGetter,
            colourSetter
        ));

        final var hsb = addRenderableWidget(new HsbColourPicker(
            innerCenterX - 30 - 4 - 20,
            offsetY,
            20,
            20,
            colourGetter.getAsInt(),
            (colour) -> {
                colourEditBox.setColourFromInt(colour);
                colourSetter.accept(colour);
            },
            preview,
            this::closePickers
        ));

        addRenderableWidget(new ImageButton(
            innerCenterX + 30 + 4,
            offsetY,
            20,
            20,
            ROLLBACK_ICON,
            (button) -> {
                colourEditBox.setColourFromInt(defaultColour);
                colourSetter.accept(defaultColour);
                hsb.close();
            }
        ));

        return hsb;
    }

    @Override
    public void onClose() {
        this.config.save();
        this.colourProvider.setTemporaryRadarBackgroundColour(null);
        this.colourProvider.setTemporaryRadarForegroundColour(null);
        super.onClose();
    }

    @Override
    public void mouseMoved(
        final double mouseX,
        final double mouseY
    ) {
        super.mouseMoved(mouseX, mouseY);
        if (this.fgPicker != null) {
            this.fgPicker.mouseMoved(mouseX, mouseY);
        }
        if (this.bgPicker != null) {
            this.bgPicker.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public void render(
        final @NotNull GuiGraphics guiGraphics,
        final int mouseX,
        final int mouseY,
        final float partialTick
    ) {
        // Don't call super since we don't want the dark or blurred background to obscure changes to the radar
        for (final Renderable renderable : ((ScreenAccessor) (Object) this).civmodern$getRenderables()) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void closePickers() {
        if (this.fgPicker != null) {
            this.fgPicker.close();
        }
        if (this.bgPicker != null) {
            this.bgPicker.close();
        }
    }
}
