package sh.okx.civmodern.common.gui.screen;

import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.gui.widget.TextRenderable;
import sh.okx.civmodern.common.gui.widget.ToggleButton;

final class IceRoadConfigScreen extends AbstractConfigScreen {
    IceRoadConfigScreen(
        final @NotNull CivMapConfig config,
        final @NotNull MainConfigScreen parent
    ) {
        super(
            config,
            Objects.requireNonNull(parent),
            Component.translatable("civmodern.screen.ice.title")
        );
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
        int offsetY = getBodyY();

        addRenderableWidget(new ToggleButton(
            offsetX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.ice.cardinal.pitch"),
            this.config::iceRoadPitchCardinalEnabled,
            this.config::setIceRoadPitchCardinalEnabled,
            Tooltip.create(Component.translatable("civmodern.screen.ice.cardinal.pitch.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new ToggleButton(
            offsetX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.ice.cardinal.yaw"),
            this.config::iceRoadYawCardinalEnabled,
            this.config::setIceRoadYawCardinalEnabled,
            Tooltip.create(Component.translatable("civmodern.screen.ice.cardinal.yaw.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new ToggleButton(
            offsetX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.ice.eat"),
            this.config::isIceRoadAutoEat,
            this.config::setIceRoadAutoEat,
            null,
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new ToggleButton(
            offsetX,
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.ice.stop"),
            this.config::isIceRoadStop,
            this.config::setIceRoadStop,
            Tooltip.create(Component.translatable("civmodern.screen.ice.stop.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(
            Button
                .builder(
                    CommonComponents.GUI_DONE,
                    (button) -> {
                        this.config.save();
                        this.minecraft.setScreen(this.parent);
                    }
                )
                .width(98)
                .pos(
                    this.centreX - 49,
                    getFooterY(offsetY)
                )
                .build()
        );
    }

    @Override
    public void onClose() {
        this.config.save();
        super.onClose();
    }
}
