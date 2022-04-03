package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
    super(new TranslatableComponent("civmodern.screen.radar.title"));
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
    int offset = this.height / 6 - 12;
    addRenderableWidget(new Button(centre, offset, 150, 20, getRadarToggleMessage(), button -> {
      config.setRadarEnabled(!config.isRadarEnabled());
      button.setMessage(getRadarToggleMessage());
    }));
    offset += 24;
    addRenderableWidget(new Button(left, offset, 150, 20, getPingToggleMessage(), button -> {
      config.setPingEnabled(!config.isPingEnabled());
      button.setMessage(getPingToggleMessage());
    }));
    addRenderableWidget(new Button(right, offset, 150, 20, getPingSoundMessage(), button -> {
      config.setPingSoundEnabled(!config.isPingSoundEnabled());
      button.setMessage(getPingSoundMessage());
    }));
    offset += 24;
    addRenderableWidget(new Button(left, offset, 150, 20, new TranslatableComponent("civmodern.screen.radar.alignment", config.getAlignment().toString()), button -> {
      config.setAlignment(config.getAlignment().next());
      button.setMessage(new TranslatableComponent("civmodern.screen.radar.alignment", config.getAlignment().toString()));
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
        return new TranslatableComponent("civmodern.screen.radar.circles",
            Integer.toString((int) value));
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
        return new TranslatableComponent("civmodern.screen.radar.iconsize", format.format(value));
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
        return new TranslatableComponent("civmodern.screen.radar.range", String.valueOf((int) value));
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
        return new TranslatableComponent("civmodern.screen.radar.size",
            Integer.toString((int) value));
      }
    }));
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
        return new TranslatableComponent("civmodern.screen.radar.transparency", format.format(value));
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
        return new TranslatableComponent("civmodern.screen.radar.x", String.valueOf((int) value));
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
        return new TranslatableComponent("civmodern.screen.radar.y", String.valueOf((int) value));
      }
    }));
    offset += 24;

    foregroundColourY = backgroundColourY = offset;

    offset += 12;

    ColourProvider colourProvider = mod.getColourProvider();
    bgPicker = addColourPicker(left, offset, CivMapConfig.DEFAULT_RADAR_BG_COLOUR, config::getRadarBgColour, config::setRadarBgColour,
        colourProvider::setTemporaryRadarBackgroundColour);
    fgPicker = addColourPicker(right, offset, CivMapConfig.DEFAULT_RADAR_FG_COLOUR, config::getRadarColour, config::setRadarColour,
        colourProvider::setTemporaryRadarForegroundColour);

    offset += 48;
    addRenderableWidget(new Button(centre, offset, 150, 20, CommonComponents.GUI_DONE, button -> {
      Minecraft.getInstance().setScreen(parent);
    }));
  }

  private HsbColourPicker addColourPicker(int x, int y, int defaultColour, Supplier<Integer> colourGet, Consumer<Integer> colourSet, Consumer<Integer> preview) {
    int left = (x + 75) - (60 + 8 + 20 + 8 + 20) / 2;
    EditBox widget = new EditBox(font, left, y, 60, 20, TextComponent.EMPTY);
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
      return new TranslatableComponent("civmodern.screen.radar.pings.enable");
    } else {
      return new TranslatableComponent("civmodern.screen.radar.pings.disable");
    }
  }

  private Component getPingSoundMessage() {
    if (config.isPingSoundEnabled()) {
      return new TranslatableComponent("civmodern.screen.radar.sound.enable");
    } else {
      return new TranslatableComponent("civmodern.screen.radar.sound.disable");
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
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    TextComponent lineColour = new TextComponent("Line colour");
    TextComponent backgroundColour = new TextComponent("Background colour");

    this.font.drawShadow(matrices, lineColour, this.width / 2f + 75 - font.width(lineColour) / 2f, foregroundColourY, 0xffffff);
    this.font.drawShadow(matrices, backgroundColour, this.width / 2f - 75 - font.width(backgroundColour) / 2f, backgroundColourY, 0xffffff);

    drawCenteredString(matrices, this.font, this.title, this.width / 2, 15, 0xffffff);

    super.render(matrices, mouseX, mouseY, delta);
  }

  private Component getRadarToggleMessage() {
    if (config.isRadarEnabled()) {
      return new TextComponent("Radar: Enabled");
    } else {
      return new TextComponent("Radar: Disabled");
    }
  }

  @Override
  public void close() {
    fgPicker.close();
    bgPicker.close();
  }
}
