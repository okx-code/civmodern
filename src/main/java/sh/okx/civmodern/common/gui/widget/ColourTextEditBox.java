package sh.okx.civmodern.common.gui.widget;

import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class ColourTextEditBox extends TextBoxComponent {
    private static final Pattern HEX_COLOUR_REGEX = Pattern.compile("^#([0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);

    private final IntConsumer colourSetter;

    public ColourTextEditBox(
        final @NotNull Sizing horizontalSizing,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter
    ) {
        super(horizontalSizing);
        this.colourSetter = Objects.requireNonNull(colourSetter);
        this.setMaxLength(7);
        this.setFilter((value) -> HEX_COLOUR_REGEX.matcher(value).matches());
        this.onChanged().subscribe((value) -> {
            // Support 3-digit hex codes
            // https://developer.mozilla.org/en-US/docs/Web/CSS/hex-color
            if (value.length() == 3) {
                final var chars = new char[6];
                chars[0] = chars[1] = value.charAt(0); // r
                chars[2] = chars[3] = value.charAt(1); // g
                chars[4] = chars[5] = value.charAt(2); // b
                value = new String(chars);
            }
            if (value.length() == 6) {
                colourSetter.accept(Integer.parseInt(value, 16));
            }
        });
        this.moveCursorToStart(false);
        this.setColourText(colourGetter.getAsInt());
    }

    public ColourTextEditBox(
        final int x,
        final int y,
        final int width,
        final int height,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter
    ) {
        this(
            Sizing.fixed(width),
            colourGetter,
            colourSetter
        );
        this.verticalSizing(Sizing.fixed(height));
        this.positioning(Positioning.absolute(x, y));
    }

    public void setColour(
        final int colour
    ) {
        this.colourSetter.accept(colour);
        this.setColourText(colour);
    }

    /// This differs from [#setColour(int)] in that it ONLY updates the field's text.
    public void setColourText(
        final int colour
    ) {
        this.value = "#" + "%06X".formatted(0xFF_FF_FF & colour).toUpperCase();
    }
}
