package sh.okx.civmodern.common.gui.screen;

import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.gui.widget.TextRenderable;

public final class MainConfigScreen extends AbstractConfigScreen {
    private static final int TITLE_Y = 15;

    private final ColourProvider colourProvider;

    public MainConfigScreen(
        final @NotNull CivMapConfig config,
        final @NotNull ColourProvider colourProvider,
        final Screen parent
    ) {
        super(
            config,
            parent,
            Component.translatable("civmodern.screen.main.title")
        );
        this.colourProvider = Objects.requireNonNull(colourProvider);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableOnly(new TextRenderable.CentreAligned(
            this.font,
            this.centreX,
            getHeaderY(),
            this.title
        ));

        final int offsetX = this.centreX - Button.DEFAULT_WIDTH / 2;
        int offsetY = Math.max(
            this.height / 6,
            TITLE_Y + this.font.lineHeight + 10 // 10padding below the title
        );

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("civmodern.screen.main.compacted"),
                    (button) -> this.minecraft.setScreen(new CompactedConfigScreen(this.config, this.colourProvider, this))
                )
                .pos(offsetX, offsetY)
                .build()
        );
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("civmodern.screen.main.radar"),
                    (button) -> this.minecraft.setScreen(new RadarConfigScreen(this.config, this.colourProvider, this))
                )
                .pos(offsetX, offsetY)
                .build()
        );
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("civmodern.screen.main.ice"),
                    (button) -> this.minecraft.setScreen(new IceRoadConfigScreen(this.config, this))
                )
                .pos(offsetX, offsetY)
                .build()
        );
    }
}
