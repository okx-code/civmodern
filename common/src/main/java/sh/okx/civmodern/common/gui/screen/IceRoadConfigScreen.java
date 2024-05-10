package sh.okx.civmodern.common.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.gui.widget.CyclicButton;

public class IceRoadConfigScreen extends Screen {
    private final AbstractCivModernMod mod;
    private final CivMapConfig config;
    private final Screen parent;

    protected IceRoadConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
        super(Component.translatable("civmodern.screen.ice.title"));
        this.mod = mod;
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 24, 165, 20, config.iceRoadPitchCardinalEnabled() ? 0 : 1, cycl -> {
            config.setIceRoadPitchCardinalEnabled(cycl.getIndex() == 0);
        }, Component.translatable("civmodern.screen.ice.cardinal.pitch.enable"), Component.translatable("civmodern.screen.ice.cardinal.pitch.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 48, 165, 20, config.iceRoadYawCardinalEnabled() ? 0 : 1, cycl -> {
            config.setIceRoadYawCardinalEnabled(cycl.getIndex() == 0);
        }, Component.translatable("civmodern.screen.ice.cardinal.yaw.enable"), Component.translatable("civmodern.screen.ice.cardinal.yaw.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 72, 165, 20, config.isIceRoadAutoEat() ? 0 : 1, cycl -> {
            config.setIceRoadAutoEat(cycl.getIndex() == 0);
        }, Component.translatable("civmodern.screen.ice.eat.enable"), Component.translatable("civmodern.screen.ice.eat.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 96, 165, 20, config.isIceRoadStop() ? 0 : 1, cycl -> {
            config.setIceRoadStop(cycl.getIndex() == 0);
        }, Component.translatable("civmodern.screen.ice.stop.enable"), Component.translatable("civmodern.screen.ice.stop.disable")));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            config.save();
            minecraft.setScreen(parent);
        }).pos(this.width / 2 - 49, this.height / 6 + 169).size(98, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        guiGraphics.drawString(font, this.title, (int) (this.width / 2f - font.width(this.title) / 2f), 15, 0xffffff);
    }

    @Override
    public void onClose() {
        super.onClose();
        config.save();
    }
}
