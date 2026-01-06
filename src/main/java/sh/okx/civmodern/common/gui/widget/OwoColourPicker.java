package sh.okx.civmodern.common.gui.widget;

import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.NotNull;

public class OwoColourPicker extends ColorPickerComponent {
    private static final int POPUP_WIDTH = 100;
    private static final int POPUP_HEIGHT = 50;

    protected final FlowLayout rootComponent;
    protected IntConsumer colourSetter;
    protected AbstractWidget anchor;

    public OwoColourPicker(
        final @NotNull FlowLayout rootComponent
    ) {
        this.rootComponent = Objects.requireNonNull(rootComponent);
        this.positioning(Positioning.absolute(0, 0));
        this.sizing(
            Sizing.fixed(POPUP_WIDTH),
            Sizing.fixed(POPUP_HEIGHT)
        );
        this.selectorWidth(10);
        this.selectorPadding(2);
        this.onChanged().subscribe((colour) -> {
            final IntConsumer colourSetter = this.colourSetter;
            if (colourSetter != null) {
                colourSetter.accept(colour.rgb());
            }
        });
    }

    @Override
    public void update(
        final float delta,
        final int mouseX,
        final int mouseY
    ) {
        super.update(
            delta,
            mouseX,
            mouseY
        );
        final AbstractWidget anchor = this.anchor;
        if (anchor != null) {
            final int anchorX = anchor.x();
            this.updateX(anchorX);
            final int anchorY = anchor.y();
            final int anchorBottomY = anchorY + anchor.height();
            if (this.rootComponent.isInBoundingBox(anchorX, anchorBottomY + POPUP_HEIGHT)) {
                this.updateY(anchorBottomY + 1);
            }
            else if (this.rootComponent.isInBoundingBox(anchorX, anchorY - POPUP_HEIGHT)) {
                this.updateY(anchorY - POPUP_HEIGHT - 1);
            }
        }
    }

    public void showPopup(
        final @NotNull AbstractWidget anchor,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter
    ) {
        this.anchor = Objects.requireNonNull(anchor);
        this.colourSetter = Objects.requireNonNull(colourSetter);
        this.selectedColor(Color.ofRgb(colourGetter.getAsInt()));
        final var popup = Containers.overlay(this);
        popup.zIndex(Short.MAX_VALUE);
        popup.surface(Surface.BLANK);
        this.rootComponent.child(popup);
        this.rootComponent.focusHandler().focus(
            this,
            FocusSource.MOUSE_CLICK
        );
    }
}
