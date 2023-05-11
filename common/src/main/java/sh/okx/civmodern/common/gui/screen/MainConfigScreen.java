package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;

public class MainConfigScreen extends Screen {

  private final AbstractCivModernMod mod;
  private final CivMapConfig config;

  public MainConfigScreen(AbstractCivModernMod mod, CivMapConfig config) {
    super(new TranslatableComponent("civmodern.screen.main.title"));
    this.mod = mod;
    this.config = config;
  }

  @Override
  protected void init() {
    int col0 = this.width / 2 - 150 / 2;
    addRenderableWidget(new Button(col0, this.height / 6, 150, 20, new TranslatableComponent("civmodern.screen.main.compacted"), button -> {
      minecraft.setScreen(new CompactedConfigScreen(mod, config, this));
    }));
    addRenderableWidget(new Button(col0, this.height / 6 + 24, 150, 20, new TranslatableComponent("civmodern.screen.main.radar"), button -> {
      minecraft.setScreen(new RadarConfigScreen(mod, config, this));
    }));
    addRenderableWidget(new Button(col0, this.height / 6 + 48, 150, 20, new TranslatableComponent("civmodern.screen.main.ice"), button -> {
      minecraft.setScreen(new IceRoadConfigScreen(mod, config, this));
    }));
    addRenderableWidget(new Button(col0, this.height / 6 + 72, 150, 20, new TranslatableComponent("civmodern.screen.main.map"), button -> {
      minecraft.setScreen(new MapConfigScreen(mod, config, this));
    }));
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    super.renderBackground(matrices);

    font.drawShadow(matrices, this.title, this.width / 2f - this.font.width(this.title) / 2f, 15, 0xffffff);
    super.render(matrices, mouseX, mouseY, delta);
  }
}
