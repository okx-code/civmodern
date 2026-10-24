package sh.okx.civmodern.common.gui.widget;

import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

/**
 * EditBox#setFilter was removed in 26.1; this subclass restores input
 * validation by rejecting typed characters that would produce a value
 * not accepted by the filter.
 */
public class FilteredEditBox extends EditBox {
    private Predicate<String> inputFilter = s -> true;

    public FilteredEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
    }

    public void setInputFilter(Predicate<String> filter) {
        this.inputFilter = filter;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String before = this.getValue();
        if (super.charTyped(event)) {
            if (this.inputFilter.test(this.getValue())) {
                return true;
            }
            this.setValue(before);
        }
        return false;
    }
}
