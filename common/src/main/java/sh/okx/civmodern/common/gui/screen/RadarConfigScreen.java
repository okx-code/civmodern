package sh.okx.civmodern.common.gui.screen;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.common.gui.DoubleValue;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.ColourProvider;

public class RadarConfigScreen extends Screen implements ScreenCloseable {

    public static final ResourceLocation ROLLBACK_ICON = new ResourceLocation("civmodern", "gui/rollback.png");
    private final AbstractCivModernMod mod;
    private final CivMapConfig config;
    private final Screen parent;
    private int foregroundColourY;
    private int backgroundColourY;

    // for passing move events
    private HsbColourPicker bgPicker;
    private HsbColourPicker fgPicker;

    public RadarConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
        super(Component.translatable("civmodern.screen.radar.title"));
        this.mod = mod;
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 155;
        int centre = left + 80;
        int right = left + 160;
        int offset = this.height / 6 - 18;

        ColourProvider colourProvider = mod.getColourProvider();
        bgPicker = addColourPicker(left, offset + 178, CivMapConfig.DEFAULT_RADAR_BG_COLOUR, config::getRadarBgColour, config::setRadarBgColour,
            colourProvider::setTemporaryRadarBackgroundColour);
        fgPicker = addColourPicker(right, offset + 178, CivMapConfig.DEFAULT_RADAR_FG_COLOUR, config::getRadarColour, config::setRadarColour,
            colourProvider::setTemporaryRadarForegroundColour);

        addRenderableWidget(Button.builder(getRadarToggleMessage(), button -> {
            config.setRadarEnabled(!config.isRadarEnabled());
            button.setMessage(getRadarToggleMessage());
        }).pos(centre, offset).size(150, 20).build());
        offset += 24;
        addRenderableWidget(Button.builder(getPingToggleMessage(), button -> {
            config.setPingEnabled(!config.isPingEnabled());
            button.setMessage(getPingToggleMessage());
        }).pos(left, offset).size(150, 20).build());
        addRenderableWidget(Button.builder(getPingSoundMessage(), button -> {
            config.setPingSoundEnabled(!config.isPingSoundEnabled());
            button.setMessage(getPingSoundMessage());
        }).pos(right, offset).size(150, 20).build());
        offset += 24;
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.radar.alignment", config.getAlignment().toString()), button -> {
            config.setAlignment(config.getAlignment().next());
            button.setMessage(Component.translatable("civmodern.screen.radar.alignment", config.getAlignment().toString()));
        }).pos(left, offset).size(150, 20).build());
        addRenderableWidget(Button.builder(getItemToggleMessage(), button -> {
            config.setShowItems(!config.isShowItems());
            button.setMessage(getItemToggleMessage());
        }).pos(right, offset).size(150, 20).build());
        offset += 24;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 1, 0.01, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("##%");

            @Override
            public double get() {
                return config.getTransparency();
            }

            @Override
            public void set(double value) {
                config.setTransparency((float) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.transparency", format.format(value));
            }
        }));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 1, 0.01, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("##%");

            @Override
            public double get() {
                return config.getBackgroundTransparency();
            }

            @Override
            public void set(double value) {
                config.setBackgroundTransparency((float) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.background_transparency", format.format(value));
            }
        }));
        offset += 24;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0.5, 2, 0.1, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("#.#");

            @Override
            public double get() {
                return config.getIconSize();
            }

            @Override
            public void set(double value) {
                config.setIconSize((float) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.iconsize", format.format(value));
            }
        }));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 20, 150, 1, new DoubleValue() {

            @Override
            public double get() {
                return config.getRange();
            }

            @Override
            public void set(double value) {
                config.setRange((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.range", String.valueOf((int) value));
            }
        }));
        offset += 24;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 25, 250, 1, new DoubleValue() {
            @Override
            public double get() {
                return config.getRadarSize();
            }

            @Override
            public void set(double value) {
                config.setRadarSize((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.size",
                    Integer.toString((int) value));
            }
        }));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 1, 8, 1, new DoubleValue() {
            @Override
            public double get() {
                return config.getRadarCircles();
            }

            @Override
            public void set(double value) {
                config.setRadarCircles((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.circles",
                    Integer.toString((int) value));
            }
        }));
        offset += 24;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 300, 1, new DoubleValue() {

            @Override
            public double get() {
                return config.getX();
            }

            @Override
            public void set(double value) {
                config.setX((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.x", String.valueOf((int) value));
            }
        }));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 300, 1, new DoubleValue() {

            @Override
            public double get() {
                return config.getY();
            }

            @Override
            public void set(double value) {
                config.setY((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.y", String.valueOf((int) value));
            }
        }));
        offset += 24;

        foregroundColourY = backgroundColourY = offset;

        offset += 12;

        offset += 36;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            config.save();
            Minecraft.getInstance().setScreen(parent);
        }).pos(centre, offset).size(150, 20).build());
    }

    private HsbColourPicker addColourPicker(int x, int y, int defaultColour, Supplier<Integer> colourGet, Consumer<Integer> colourSet, Consumer<Integer> preview) {
        int left = (x + 75) - (60 + 8 + 20 + 8 + 20) / 2;
        EditBox widget = new EditBox(font, left, y, 60, 20, Component.empty());
        widget.setValue("#" + String.format("%06X", colourGet.get()));
        widget.setMaxLength(7);
        Pattern pattern = Pattern.compile("^(#[0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);
        widget.setFilter(string -> pattern.matcher(string).matches());
        widget.setResponder(val -> {
            if (val.length() == 7) {
                int rgb = Integer.parseInt(val.substring(1), 16);
                colourSet.accept(rgb);
            }
        });
        addRenderableWidget(widget);

        HsbColourPicker hsb = new HsbColourPicker(left + 60 + 8, y,
            20, 20, colourGet.get(), colour -> {
            widget.setValue("#" + String.format("%06X", colour));
            colourSet.accept(colour);
        }, preview, this);
        addRenderableWidget(new ImageButton(left + 60 + 8 + 20 + 8, y, 20, 20, ROLLBACK_ICON, imbg -> {
            widget.setValue("#" + String.format("%06X", defaultColour));
            colourSet.accept(defaultColour);
            hsb.close();
        }));
        addRenderableWidget(hsb);
        return hsb;
    }

    private Component getPingToggleMessage() {
        if (config.isPingEnabled()) {
            return Component.translatable("civmodern.screen.radar.pings.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.pings.disable");
        }
    }

    private Component getItemToggleMessage() {
        if (config.isShowItems()) {
            return Component.translatable("civmodern.screen.radar.items.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.items.disable");
        }
    }

    private Component getPingSoundMessage() {
        if (config.isPingSoundEnabled()) {
            return Component.translatable("civmodern.screen.radar.sound.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.sound.disable");
        }
    }

    @Override
    public void onClose() {
        config.save();
        mod.getColourProvider().setTemporaryRadarBackgroundColour(null);
        mod.getColourProvider().setTemporaryRadarForegroundColour(null);
        super.onClose();
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
        if (fgPicker != null) {
            fgPicker.mouseMoved(d, e);
        }
        if (bgPicker != null) {
            bgPicker.mouseMoved(d, e);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Component lineColour = Component.literal("Line colour");
        Component backgroundColour = Component.literal("Background colour");

        this.font.drawInBatch(lineColour, this.width / 2f + 75 - font.width(lineColour) / 2f, foregroundColourY, 0xffffff, true, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
        this.font.drawInBatch(backgroundColour, this.width / 2f - 75 - font.width(backgroundColour) / 2f, backgroundColourY, 0xffffff, true, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xffffff);

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    private Component getRadarToggleMessage() {
        if (config.isRadarEnabled()) {
            return Component.literal("Radar: Enabled");
        } else {
            return Component.literal("Radar: Disabled");
        }
    }

    @Override
    public void close() {
        fgPicker.close();
        bgPicker.close();
    }
}
