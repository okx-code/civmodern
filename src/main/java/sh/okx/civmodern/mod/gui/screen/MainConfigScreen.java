package sh.okx.civmodern.mod.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MainConfigScreen extends Screen {
    public MainConfigScreen() {
        super(Component.translatable("civmodern.screen.main.title"));
    }

    @Override
    protected void init() {
        int col0 = this.width / 2 - 150 / 2;
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.main.compacted"), button -> {
            minecraft.setScreen(new CompactedConfigScreen(this));
        }).pos(col0, this.height / 6).size(150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.main.ice"), button -> {
            minecraft.setScreen(new IceRoadConfigScreen(this));
        }).pos(col0, this.height / 6 + 24).size(150, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(this.font, this.title, (int) (this.width / 2f - this.font.width(this.title) / 2f), 15, 0xffffff);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}
