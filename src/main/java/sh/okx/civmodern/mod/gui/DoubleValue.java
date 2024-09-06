package sh.okx.civmodern.mod.gui;

import net.minecraft.network.chat.Component;

public interface DoubleValue {
    double get();
    void set(double value);
    Component getText(double value);
}
