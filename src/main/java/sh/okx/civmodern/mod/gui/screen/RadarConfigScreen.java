package sh.okx.civmodern.mod.gui.screen;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.mod.CivModernConfig;
import sh.okx.civmodern.mod.ColourProvider;
import sh.okx.civmodern.mod.gui.DoubleValue;
import sh.okx.civmodern.mod.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.mod.gui.widget.HsbColourPicker;
import sh.okx.civmodern.mod.gui.widget.ImageButton;

public class RadarConfigScreen extends Screen implements ScreenCloseable {

    public static final ResourceLocation ROLLBACK_ICON = ResourceLocation.tryBuild("civmodern", "gui/rollback.png");
    private final List<Renderable> renderables = new ArrayList<>(); // copied from Screen because it's private there
    private final Screen parent;
    private int foregroundColourY;
    private int backgroundColourY;

    // for passing move events
    private HsbColourPicker bgPicker;
    private HsbColourPicker fgPicker;

    public RadarConfigScreen(Screen parent) {
        super(Component.translatable("civmodern.screen.radar.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 155;
        int centre = left + 80;
        int right = left + 160;
        int offset = this.height / 6 - 18;

        this.bgPicker = addColourPicker(
            left,
            offset + 178,
            CivModernConfig.DEFAULT_RADAR_BG_COLOUR,
            () -> CivModernConfig.radarBgColour,
            (value) -> CivModernConfig.radarBgColour = value,
            ColourProvider::setTemporaryRadarBackgroundColour
        );
        this.fgPicker = addColourPicker(
            right,
            offset + 178,
            CivModernConfig.DEFAULT_RADAR_FG_COLOUR,
            () -> CivModernConfig.radarFgColour,
            (value) -> CivModernConfig.radarFgColour = value,
            ColourProvider::setTemporaryRadarForegroundColour
        );

        addRenderableWidget(Button.builder(getRadarToggleMessage(), button -> {
            CivModernConfig.radarEnabled = !CivModernConfig.radarEnabled;
            button.setMessage(getRadarToggleMessage());
        }).pos(centre, offset).size(150, 20).build());

        offset += 24;

        addRenderableWidget(Button.builder(getPingToggleMessage(), button -> {
            CivModernConfig.radarPingsEnabled = !CivModernConfig.radarPingsEnabled;
            button.setMessage(getPingToggleMessage());
        }).pos(left, offset).size(150, 20).build());

        addRenderableWidget(Button.builder(getPingSoundMessage(), button -> {
            CivModernConfig.radarPingSoundEnabled = !CivModernConfig.radarPingSoundEnabled;
            button.setMessage(getPingSoundMessage());
        }).pos(right, offset).size(150, 20).build());

        offset += 24;

        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.radar.alignment", CivModernConfig.radarAlignment.toString()), button -> {
            CivModernConfig.radarAlignment = CivModernConfig.radarAlignment.next();
            button.setMessage(Component.translatable("civmodern.screen.radar.alignment", CivModernConfig.radarAlignment.toString()));
        }).pos(left, offset).size(150, 20).build());

        addRenderableWidget(Button.builder(getItemToggleMessage(), button -> {
            CivModernConfig.radarShowItems = !CivModernConfig.radarShowItems;
            button.setMessage(getItemToggleMessage());
        }).pos(right, offset).size(150, 20).build());

        offset += 24;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 1, 0.01, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("##%");
            @Override
            public double get() {
                return CivModernConfig.radarTransparency;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarTransparency = (float) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.transparency", this.format.format(value));
            }
        }));

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 1, 0.01, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("##%");
            @Override
            public double get() {
                return CivModernConfig.radarBackgroundTransparency;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarBackgroundTransparency = (float) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.background_transparency", this.format.format(value));
            }
        }));

        offset += 24;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0.5, 2, 0.1, new DoubleValue() {
            private final DecimalFormat format = new DecimalFormat("#.#");
            @Override
            public double get() {
                return CivModernConfig.radarIconSize;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarIconSize = (float) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.iconsize", this.format.format(value));
            }
        }));

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 20, 150, 1, new DoubleValue() {
            @Override
            public double get() {
                return CivModernConfig.radarRange;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarRange = value;
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
                return CivModernConfig.radarSize;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarSize = (int) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.size", Integer.toString((int) value));
            }
        }));

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 1, 8, 1, new DoubleValue() {
            @Override
            public double get() {
                return CivModernConfig.radarCircles;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarCircles = (int) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.circles", Integer.toString((int) value));
            }
        }));

        offset += 24;

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 300, 1, new DoubleValue() {
            @Override
            public double get() {
                return CivModernConfig.radarX;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarX = (int) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.x", String.valueOf((int) value));
            }
        }));

        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 300, 1, new DoubleValue() {
            @Override
            public double get() {
                return CivModernConfig.radarY;
            }
            @Override
            public void set(double value) {
                CivModernConfig.radarY = (int) value;
            }
            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.y", String.valueOf((int) value));
            }
        }));

        offset += 24;

        this.foregroundColourY = this.backgroundColourY = offset;

        offset += 12;

        offset += 36;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            CivModernConfig.save();
            Minecraft.getInstance().setScreen(this.parent);
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
        if (CivModernConfig.radarPingsEnabled) {
            return Component.translatable("civmodern.screen.radar.pings.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.pings.disable");
        }
    }

    private Component getItemToggleMessage() {
        if (CivModernConfig.radarShowItems) {
            return Component.translatable("civmodern.screen.radar.items.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.items.disable");
        }
    }

    private Component getPingSoundMessage() {
        if (CivModernConfig.radarPingSoundEnabled) {
            return Component.translatable("civmodern.screen.radar.sound.enable");
        } else {
            return Component.translatable("civmodern.screen.radar.sound.disable");
        }
    }

    @Override
    public void onClose() {
        CivModernConfig.save();
        ColourProvider.setTemporaryRadarBackgroundColour(null);
        ColourProvider.setTemporaryRadarForegroundColour(null);
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

        // Don't call super because we don't want the dark background to allow people to see the radar easily
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    private Component getRadarToggleMessage() {
        if (CivModernConfig.radarEnabled) {
            return Component.literal("Radar: Enabled");
        } else {
            return Component.literal("Radar: Disabled");
        }
    }

    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        this.renderables.add(widget);
        return super.addRenderableWidget(widget);
    }

    @Override
    public void close() {
        fgPicker.close();
        bgPicker.close();
    }
}
