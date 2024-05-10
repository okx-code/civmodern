package sh.okx.civmodern.common.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;

public class MainConfigScreen extends Screen {

    private final AbstractCivModernMod mod;
    private final CivMapConfig config;

    public MainConfigScreen(AbstractCivModernMod mod, CivMapConfig config) {
        super(Component.translatable("civmodern.screen.main.title"));
        this.mod = mod;
        this.config = config;
    }

    @Override
    protected void init() {
        int col0 = this.width / 2 - 150 / 2;
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.main.compacted"), button -> {
            minecraft.setScreen(new CompactedConfigScreen(mod, config, this));
        }).pos(col0, this.height / 6).size(150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.main.radar"), button -> {
            minecraft.setScreen(new RadarConfigScreen(mod, config, this));
        }).pos(col0, this.height / 6 + 24).size(150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("civmodern.screen.main.ice"), button -> {
            minecraft.setScreen(new IceRoadConfigScreen(mod, config, this));
        }).pos(col0, this.height / 6 + 48).size(150, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(this.font, this.title, (int) (this.width / 2f - this.font.width(this.title) / 2f), 15, 0xffffff);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}
