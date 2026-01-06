package sh.okx.civmodern.common.gui.widget;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.Sizing;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.jetbrains.annotations.NotNull;

public class OwoButton extends ButtonComponent {
    public static final Consumer<ButtonComponent> ON_PRESS_NO_OP = (button) -> {};

    public OwoButton(
        final @NotNull Component message,
        final @NotNull Consumer<ButtonComponent> onPress
    ) {
        super(
            message,
            onPress
        );
        this.sizing(
            Sizing.fixed(Button.DEFAULT_WIDTH),
            Sizing.fixed(Button.DEFAULT_HEIGHT)
        );
    }

    public static @NotNull OwoButton toggleButton(
        final @NotNull Component label,
        final @NotNull BooleanSupplier valueGetter,
        final @NotNull BooleanConsumer valueSetter,
        final Tooltip tooltip
    ) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(valueGetter);
        Objects.requireNonNull(valueSetter);
        final var button = new OwoButton(
            valueGetter.getAsBoolean()
                ? Component.translatable("civmodern.button.toggle.on", label)
                : Component.translatable("civmodern.button.toggle.off", label),
            (cb_button) -> {
                final boolean next = !valueGetter.getAsBoolean();
                valueSetter.accept(next);
                cb_button.setMessage(next
                    ? Component.translatable("civmodern.button.toggle.on", label)
                    : Component.translatable("civmodern.button.toggle.off", label)
                );
            }
        );
        button.setTooltip(tooltip);
        return button;
    }

    public static final int IMAGE_BUTTON_SIZE = 20;
    public static final int IMAGE_BUTTON_TEXTURE_WIDTH = 20;
    public static final int IMAGE_BUTTON_TEXTURE_HEIGHT = 40;
    public static @NotNull OwoButton imageButton(
        final @NotNull ResourceLocation texture,
        final @NotNull Consumer<ButtonComponent> onPress
    ) {
        final var button = new OwoButton(
            Component.empty(),
            Objects.requireNonNull(onPress)
        );
        button.sizing(
            Sizing.fixed(IMAGE_BUTTON_SIZE),
            Sizing.fixed(IMAGE_BUTTON_SIZE)
        );
        button.renderer(Renderer.texture(
            Objects.requireNonNull(texture),
            0,
            0,
            IMAGE_BUTTON_TEXTURE_WIDTH,
            IMAGE_BUTTON_TEXTURE_HEIGHT
        ));
        return button;
    }
}
