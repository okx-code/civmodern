package sh.okx.civmodern.mod.gui.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CyclicButton extends Button {

    private final OnPress onPress;
    private final Component[] components;
    private int index;

    public CyclicButton(int x, int y, int width, int height, int index, OnPress onPress, Component... components) {
        super(x, y, width, height, components[index], b -> {}, DEFAULT_NARRATION);
        this.index = index;
        this.components = components;
        this.onPress = onPress;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void onPress() {
        index = (index + 1) % components.length;
        this.setMessage(components[index]);
        onPress.onPress(this);
    }

    public interface OnPress {
        void onPress(CyclicButton button);
    }
}
