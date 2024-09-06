package sh.okx.civmodern.mod.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import sh.okx.civmodern.mod.CivModernConfig;
import sh.okx.civmodern.mod.gui.widget.CyclicButton;

public class IceRoadConfigScreen extends Screen {
    private final Screen parent;

    protected IceRoadConfigScreen(Screen parent) {
        super(Component.translatable("civmodern.screen.ice.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 24, 165, 20, CivModernConfig.iceRoadPitchCardinalEnabled ? 0 : 1, cycl -> {
            CivModernConfig.iceRoadPitchCardinalEnabled = cycl.getIndex() == 0;
        }, Component.translatable("civmodern.screen.ice.cardinal.pitch.enable"), Component.translatable("civmodern.screen.ice.cardinal.pitch.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 48, 165, 20, CivModernConfig.iceRoadYawCardinalEnabled ? 0 : 1, cycl -> {
            CivModernConfig.iceRoadYawCardinalEnabled = cycl.getIndex() == 0;
        }, Component.translatable("civmodern.screen.ice.cardinal.yaw.enable"), Component.translatable("civmodern.screen.ice.cardinal.yaw.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 72, 165, 20, CivModernConfig.iceRoadAutoEat ? 0 : 1, cycl -> {
            CivModernConfig.iceRoadAutoEat = cycl.getIndex() == 0;
        }, Component.translatable("civmodern.screen.ice.eat.enable"), Component.translatable("civmodern.screen.ice.eat.disable")));

        addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 96, 165, 20, CivModernConfig.iceRoadStop ? 0 : 1, cycl -> {
            CivModernConfig.iceRoadStop = cycl.getIndex() == 0;
        }, Component.translatable("civmodern.screen.ice.stop.enable"), Component.translatable("civmodern.screen.ice.stop.disable")));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            CivModernConfig.save();
            minecraft.setScreen(parent);
        }).pos(this.width / 2 - 49, this.height / 6 + 169).size(98, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.drawString(font, this.title, (int) (this.width / 2f - font.width(this.title) / 2f), 15, 0xffffff);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public void onClose() {
        super.onClose();
        CivModernConfig.save();
    }
}