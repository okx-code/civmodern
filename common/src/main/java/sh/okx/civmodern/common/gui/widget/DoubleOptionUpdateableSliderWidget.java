package sh.okx.civmodern.common.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import sh.okx.civmodern.common.gui.DoubleValue;

public class DoubleOptionUpdateableSliderWidget extends AbstractSliderButton {
  private final DoubleValue param;
  private final double min;
  private final double max;
  private final double step;

  public DoubleOptionUpdateableSliderWidget(int x, int y,
      int width, int height, double min, double max, double step, DoubleValue param) {
    super(x, y, width, height, TextComponent.EMPTY, (param.get() - min) / (max - min));
    this.param = param;
    this.min = min;
    this.max = max;
    this.step = step;
    this.updateMessage();
  }

  public void update() {
    updateMessage();
    this.value = (param.get() - min) / (max - min);
  }

  @Override
  protected void updateMessage() {
    this.setMessage(this.param.getText(this.param.get()));
  }

  @Override
  protected void applyValue() {
    param.set(Mth.lerp(this.value, min, max));
  }
}
