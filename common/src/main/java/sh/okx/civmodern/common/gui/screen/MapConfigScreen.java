package sh.okx.civmodern.common.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.gui.DoubleValue;
import sh.okx.civmodern.common.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class MapConfigScreen extends AbstractConfigScreen {

    public static final ResourceLocation ROLLBACK_ICON = ResourceLocation.fromNamespaceAndPath("civmodern", "gui/rollback.png");
    private int chevronColourY;

    private final ColourProvider colourProvider;

    // for passing move events
    private HsbColourPicker chevronPicker;

    public MapConfigScreen(ColourProvider colourProvider, CivMapConfig config, Screen parent) {
        super(config, parent, Component.translatable("civmodern.screen.map.title"));
        this.colourProvider = colourProvider;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 155;
        int centre = left + 80;
        int right = left + 160;
        int offset = this.height / 6 - 18;
        addRenderableWidget(Button.builder(getMinimapToggleMessage(), button -> {
            config.setMinimapEnabled(!config.isMinimapEnabled());
            button.setMessage(getMinimapToggleMessage());
        }).pos(centre, offset).size(150, 20).build());
        offset += 24;
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.radar.alignment", config.getMinimapAlignment().toString()), button -> {
            config.setMinimapAlignment(config.getMinimapAlignment().next());
            button.setMessage(Component.translatable("civmodern.screen.radar.alignment", config.getMinimapAlignment().toString()));
        }).pos(left, offset).size(150, 20).build());
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 25, 250, new DoubleValue() {
            @Override
            public double get() {
                return config.getMinimapSize();
            }

            @Override
            public void set(double value) {
                config.setMinimapSize((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.size",
                    Integer.toString((int) value));
            }
        }));
        offset += 24;
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 300, new DoubleValue() {

            @Override
            public double get() {
                return config.getMinimapX();
            }

            @Override
            public void set(double value) {
                config.setMinimapX((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.x", String.valueOf((int) value));
            }
        }));
        addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 300, new DoubleValue() {

            @Override
            public double get() {
                return config.getMinimapY();
            }

            @Override
            public void set(double value) {
                config.setMinimapY((int) value);
            }

            @Override
            public Component getText(double value) {
                return Component.translatable("civmodern.screen.radar.y", String.valueOf((int) value));
            }
        }));
        offset += 24;

        chevronColourY = offset;

        offset += 12;

        chevronPicker = addColourPicker(centre, offset, CivMapConfig.DEFAULT_CHEVRON_COLOUR, config::getChevronColour, config::setChevronColour,
            colourProvider::setTemporaryChevronColour);

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
        }, preview, this::closePickers);
        addRenderableWidget(new ImageButton(left + 60 + 8 + 20 + 8, y, 20, 20, ROLLBACK_ICON, imbg -> {
            widget.setValue("#" + String.format("%06X", defaultColour));
            colourSet.accept(defaultColour);
            hsb.close();
        }));
        addRenderableWidget(hsb);
        return hsb;
    }

    private void closePickers() {
        if (chevronPicker != null) {
            chevronPicker.close();
        }
    }

    @Override
    public void onClose() {
        config.save();
        colourProvider.setTemporaryChevronColour(null);
        super.onClose();
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
        if (chevronPicker != null) {
            chevronPicker.mouseMoved(d, e);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Component backgroundColour = Component.literal("Chevron colour");

        graphics.drawString(font, backgroundColour, (int) (this.width / 2f - font.width(backgroundColour) / 2f), chevronColourY, 0xffffff, true);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xffffff);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component getMinimapToggleMessage() {
        if (config.isMinimapEnabled()) {
            return Component.literal("Minimap: Enabled");
        } else {
            return Component.literal("Minimap: Disabled");
        }
    }
}
