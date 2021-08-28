package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DecimalFormat;
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
import sh.okx.civmodern.common.compat.CommonFont;
import sh.okx.civmodern.common.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.common.gui.DoubleValue;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;

public class RadarConfigScreen extends Screen {

  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private final Screen parent;
  private CommonFont cFont;

  public RadarConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
    super(new TranslatableComponent("civmodern.screen.radar.title"));
    this.mod = mod;
    this.config = config;
    this.parent = parent;
  }

  @Override
  protected void init() {
    this.cFont = mod.getCompat().provideFont(this.font);

    int centre = this.width / 2 - 98 / 2;
    addButton(new Button(this.width / 2 - 75, this.height / 6 + 24, 150, 20, getRadarToggleMessage(), button -> {
      config.setRadarEnabled(!config.isRadarEnabled());
      button.setMessage(getRadarToggleMessage());
    }));
    addButton(new Button(this.width / 2 - 75, this.height / 6 + 48, 150, 20, getPingToggleMessage(), button -> {
      config.setPingEnabled(!config.isPingEnabled());
      button.setMessage(getPingToggleMessage());
    }));
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 24, 150, 20, 1, 8, 1, new DoubleValue() {
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
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 48, 150, 20, 50, 300, 1, new DoubleValue() {
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
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 72, 150, 20, 0.5, 2, 0.1, new DoubleValue() {
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
    addButton(new Button(this.width / 2 - 75, this.height / 6 + 48 + 96, 150, 20, new TranslatableComponent("civmodern.screen.radar.alignment", config.getAlignment().toString()), button -> {
      config.setAlignment(config.getAlignment().next());
      button.setMessage(new TranslatableComponent("civmodern.screen.radar.alignment", config.getAlignment().toString()));
    }));
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 120, 150, 20, 20, 150, 1, new DoubleValue() {

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
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 144, 150, 20, 0, 300, 1, new DoubleValue() {

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
    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 48 + 168, 150, 20, 0, 300, 1, new DoubleValue() {

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

    addButton(new DoubleOptionUpdateableSliderWidget(this.width / 2 - 75, this.height / 6 + 168 + 48 + 24, 150, 20, 0, 1, 0.01, new DoubleValue() {
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

    int left = width / 2 - (60 + 8 + 20 + 8 + 20) / 2;
    EditBox widget = new EditBox(font, left, height / 6 + 48 + 48 + 168, 60, 20, TextComponent.EMPTY);
    widget.setValue("#" + String.format("%06X", config.getRadarColour()));
    widget.setMaxLength(7);
    Pattern pattern = Pattern.compile("^(#[0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);
    widget.setFilter(string -> pattern.matcher(string).matches());
    widget.setResponder(val -> {
      if (val.length() == 7) {
        int rgb = Integer.parseInt(val.substring(1), 16);
        config.setRadarColour(rgb);
      }
    });
    addButton(widget);

    HsbColourPicker hsb = new HsbColourPicker(left + 60 + 8, height / 6 + 48 + 48 + 168,
        20, 20, config.getRadarColour(), colour -> {
      widget.setValue("#" + String.format("%06X", colour));
      config.setRadarColour(colour);
    });
    addButton(new ImageButton(left + 60 + 8 + 20 + 8, height / 6 + 48 + 48 + 168, 20, 20, new ResourceLocation("civmodern", "gui/rollback.png"), imbg -> {
      int colour = 0x888888;
      widget.setValue("#888888");
      config.setRadarColour(colour);
      hsb.close();
    }));
    addButton(hsb);

    addButton(new Button(centre, this.height / 6 + 24 + 265 + 48, 98, 20, CommonComponents.GUI_DONE, button -> {
      Minecraft.getInstance().setScreen(parent);
    }));
  }

  private Component getPingToggleMessage() {
    if (config.isPingEnabled()) {
      return new TextComponent("Pings: Enabled");
    } else {
      return new TextComponent("Pings: Disabled");
    }
  }

  @Override
  public void onClose() {
    config.save();
    super.onClose();
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    //super.renderBackground(matrices);
    super.render(matrices, mouseX, mouseY, delta);

    this.cFont.drawShadowCentred(matrices, this.title, this.width / 2f, 40, 0xffffff);
  }

  private Component getRadarToggleMessage() {
    if (config.isRadarEnabled()) {
      return new TextComponent("Radar: Enabled");
    } else {
      return new TextComponent("Radar: Disabled");
    }
  }
}
