package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.ParentComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class Modal<T extends ParentComponent> implements Renderable, GuiEventListener, NarratableEntry {
    protected final OwoUIAdapter<T> layout;
    protected boolean visible = false;

    protected Modal(OwoUIAdapter<T> layout) {
        this.layout = layout;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (visible) {
            this.layout.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setFocused(boolean bl) {
        this.layout.setFocused(bl);
    }

    @Override
    public boolean isFocused() {
        return this.layout.isFocused();
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (!visible) {
            return false;
        }
        return this.layout.mouseClicked(d - this.layout.x(), e - this.layout.y(), i);
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        if (!visible) {
            return false;
        }
        return this.layout.mouseReleased(d - this.layout.x(), e - this.layout.y(), i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (!visible) {
            return false;
        }
        return this.layout.mouseDragged(d - this.layout.x(), e - this.layout.y(), i, f, g);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (!visible) {
            return false;
        }
        return this.layout.mouseScrolled(d - this.layout.x(), e - this.layout.y(), f, g);
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        if (!visible) {
            return false;
        }
        return this.layout.isMouseOver(d, e);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (!visible) {
            return false;
        }
        return this.layout.keyPressed(i, j, k);
    }

    @Override
    public boolean charTyped(char c, int i) {
        if (!visible) {
            return false;
        }
        return this.layout.charTyped(c, i);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return this.layout.narrationPriority();
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        this.layout.updateNarration(narrationElementOutput);
    }
}
