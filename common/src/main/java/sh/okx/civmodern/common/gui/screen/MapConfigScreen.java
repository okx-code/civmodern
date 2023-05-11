package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
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
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.gui.DoubleValue;
import sh.okx.civmodern.common.gui.widget.DoubleOptionUpdateableSliderWidget;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class MapConfigScreen extends Screen implements ScreenCloseable {

  public static final ResourceLocation ROLLBACK_ICON = new ResourceLocation("civmodern", "gui/rollback.png");
  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private final Screen parent;
  private int chevronColourY;

  // for passing move events
  private HsbColourPicker chevronPicker;

  public MapConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
    super(new TranslatableComponent("civmodern.screen.map.title"));
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
    addRenderableWidget(new Button(centre, offset, 150, 20, getMinimapToggleMessage(), button -> {
      config.setMinimapEnabled(!config.isMinimapEnabled());
      button.setMessage(getMinimapToggleMessage());
    }));
    offset += 24;
    addRenderableWidget(new Button(left, offset, 150, 20, new TranslatableComponent("civmodern.screen.radar.alignment", config.getMinimapAlignment().toString()), button -> {
      config.setMinimapAlignment(config.getMinimapAlignment().next());
      button.setMessage(new TranslatableComponent("civmodern.screen.radar.alignment", config.getMinimapAlignment().toString()));
    }));
    addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 25, 250, 1, new DoubleValue() {
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
        return new TranslatableComponent("civmodern.screen.radar.size",
            Integer.toString((int) value));
      }
    }));
    offset += 24;
    addRenderableWidget(new DoubleOptionUpdateableSliderWidget(left, offset, 150, 20, 0, 300, 1, new DoubleValue() {

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
        return new TranslatableComponent("civmodern.screen.radar.x", String.valueOf((int) value));
      }
    }));
    addRenderableWidget(new DoubleOptionUpdateableSliderWidget(right, offset, 150, 20, 0, 300, 1, new DoubleValue() {

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
        return new TranslatableComponent("civmodern.screen.radar.y", String.valueOf((int) value));
      }
    }));
    offset += 24;

    chevronColourY = offset;

    offset += 12;

    ColourProvider colourProvider = mod.getColourProvider();
    chevronPicker = addColourPicker(centre, offset, CivMapConfig.DEFAULT_CHEVRON_COLOUR, config::getChevronColour, config::setChevronColour,
        colourProvider::setTemporaryChevronColour);

    offset += 36;
    addRenderableWidget(new Button(centre, offset, 150, 20, CommonComponents.GUI_DONE, button -> {
      config.save();
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

  @Override
  public void onClose() {
    config.save();
    mod.getColourProvider().setTemporaryChevronColour(null);
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
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    TextComponent backgroundColour = new TextComponent("Chevron colour");

    this.font.drawShadow(matrices, backgroundColour, this.width / 2f - font.width(backgroundColour) / 2f, chevronColourY, 0xffffff);

    drawCenteredString(matrices, this.font, this.title, this.width / 2, 15, 0xffffff);

    super.render(matrices, mouseX, mouseY, delta);
  }

  private Component getMinimapToggleMessage() {
    if (config.isMinimapEnabled()) {
      return new TextComponent("Minimap: Enabled");
    } else {
      return new TextComponent("Minimap: Disabled");
    }
  }

  @Override
  public void close() {
    chevronPicker.close();
  }
}
