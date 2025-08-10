package sh.okx.civmodern.common.gui.screen;

import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.CivMapConfig;

abstract class AbstractConfigScreen extends Screen {
    private static final int PADDING_Y = 15;
    private static final int DEFAULT_WIDGET_HEIGHT = 20;

    protected final Screen parent;
    protected final CivMapConfig config;

    AbstractConfigScreen(
        final @NotNull CivMapConfig config,
        final Screen parent,
        final @NotNull Component title
    ) {
        super(Objects.requireNonNull(title));
        this.config = Objects.requireNonNull(config);
        this.parent = parent;
    }

    protected int centreX;

    /**
     * Override this but call super first!
     */
    @Override
    protected void init() {
        this.centreX = this.width / 2;
    }

    /**
     * Returns where the header text should go.
     */
    protected int getHeaderY() {
        return PADDING_Y;
    }

    /**
     * Returns the starting-Y of the body (1/6th of the height down from the top), or 10 below the header, whichever is
     * lower on-screen.
     */
    protected int getBodyY() {
        return getBodyY(this.height / 6);
    }

    /**
     * Returns the starting-Y of the body, returning either the provided preferred Y, or 10 below the header, whichever
     * is lower on-screen.
     */
    protected int getBodyY(
        final int preferredOffsetY
    ) {
        return Math.max(
            preferredOffsetY,
            getHeaderY() + this.font.lineHeight + 10 // 10-padding below the header
        );
    }

    /**
     * Returns the Y for any done-button or similar, choosing either 15 from the bottom, or the provided Y, whichever is
     * lower on-screen.
     */
    protected int getFooterY(
        final int offsetY
    ) {
        return Math.max(offsetY, this.height - PADDING_Y - DEFAULT_WIDGET_HEIGHT);
    }

    @Override
    public void onClose() {
        // Do not call super: it's just this but with a null instead of this.parent
        //super.onClose();
        this.minecraft.setScreen(this.parent);
    }
}
