package sh.okx.civmodern.common.gui;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ColorValue implements DoubleValue {
  private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("##%");

  private final String name;
  private final int bits;
  private final Supplier<Integer> supplier;
  private final Consumer<Integer> consumer;

  public ColorValue(String name, int bits, Supplier<Integer> supplier, Consumer<Integer> consumer) {
    this.name = name;
    this.bits = bits;
    this.supplier = supplier;
    this.consumer = consumer;
  }

  @Override
  public double get() {
    return ((supplier.get() >> bits) & 0xFF) / 255d;
  }

  @Override
  public void set(double value) {
    int set = (1 << bits + 8) - (1 << bits);
    consumer.accept((supplier.get() & ~set) | ((int) (value * 255d) << bits));
  }

  @Override
  public Component getText(double value) {
    return new TranslatableComponent(name, PERCENT_FORMAT.format(value));
  }
}
