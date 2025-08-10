package sh.okx.civmodern.common.gui.widget;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.jetbrains.annotations.NotNull;

public class ToggleButton extends Button {
    public static final int DEFAULT_BUTTON_WIDTH = 150;
    public static final CreateNarration DEFAULT_NARRATION = Button.DEFAULT_NARRATION;

    private final Component label;
    private final BooleanSupplier valueGetter;
    private final BooleanConsumer valueSetter;

    public ToggleButton(
        final int x,
        final int y,
        final int width,
        final @NotNull Component label,
        final BooleanSupplier valueGetter,
        final BooleanConsumer valueSetter,
        final Tooltip tooltip,
        final @NotNull CreateNarration createNarration
    ) {
        super(x, y, width, DEFAULT_HEIGHT, Component.empty(), null, createNarration);
        this.label = Objects.requireNonNull(label);
        this.valueGetter = Objects.requireNonNull(valueGetter);
        this.valueSetter = Objects.requireNonNull(valueSetter);
        setMessage(generateLabel());
        setTooltip(tooltip);
    }

    protected final @NotNull Component generateLabel() {
        return Component.translatable(
            "civmodern.button.toggle",
            this.label,
            this.valueGetter.getAsBoolean()
                ? Component.translatable("civmodern.button.toggle.on")
                : Component.translatable("civmodern.button.toggle.off")
        );
    }

    @Override
    public void onPress() {
        this.valueSetter.accept(!this.valueGetter.getAsBoolean());
        setMessage(generateLabel());
    }
}
