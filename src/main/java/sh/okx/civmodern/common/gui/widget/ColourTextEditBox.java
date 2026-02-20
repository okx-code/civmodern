package sh.okx.civmodern.common.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ColourTextEditBox extends EditBox {
    private static final Pattern HEX_COLOUR_REGEX = Pattern.compile("^(#[0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);

    public ColourTextEditBox(
        Sizing horizontalSizing,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter
    ) {
        this(Minecraft.getInstance().font, 0, 0, 0, 0, colourGetter, colourSetter);

        this.sizing(horizontalSizing, Sizing.content());
//        setColourFromInt(colourGetter.getAsInt());
    }

    protected CursorStyle owo$preferredCursorStyle() {
        return CursorStyle.TEXT;
    }

    public ColourTextEditBox(
        final @NotNull Font font,
        final int x,
        final int y,
        final int width,
        final int height,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter
    ) {
        super(font, x, y, width, height, Component.empty());
        setColourFromInt(colourGetter.getAsInt());
        setMaxLength(7);
        setFilter((string) -> HEX_COLOUR_REGEX.matcher(string).matches());
        setResponder((val) -> {
            if (val.length() <= 1) {
                return;
            }
            val = val.substring(1);
            // Support 3-digit hex codes
            // https://developer.mozilla.org/en-US/docs/Web/CSS/hex-color
            if (val.length() == 3) {
                final var chars = new char[6];
                chars[0] = chars[1] = val.charAt(0); // r
                chars[2] = chars[3] = val.charAt(1); // g
                chars[4] = chars[5] = val.charAt(2); // b
                val = new String(chars);
            }
            if (val.length() == 6) {
                colourSetter.accept(Integer.parseInt(val, 16));
            }
        });
    }

    public void setColourFromInt(
        final int colour
    ) {
        setValue("#" + "%06X".formatted(colour));
    }
}
