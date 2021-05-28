package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.compat.CommonFont;

public class MainConfigScreen extends Screen {

  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private CommonFont cFont;

  public MainConfigScreen(AbstractCivModernMod mod, CivMapConfig config) {
    super(new TranslatableComponent("civmodern.screen.main.title"));
    this.mod = mod;
    this.config = config;
  }

  @Override
  protected void init() {
    this.cFont = mod.getCompat().provideFont(this.font);

    int col0 = this.width / 2 - 150 / 2;
    addButton(new Button(col0, this.height / 6, 150, 20, new TranslatableComponent("civmodern.screen.main.compacted"), button -> {
      Minecraft.getInstance().setScreen(new CompactedConfigScreen(mod, config, this));
    }));
    addButton(new Button(col0, this.height / 6 + 24, 150, 20, new TranslatableComponent("civmodern.screen.main.radar"), button -> {
      Minecraft.getInstance().setScreen(new RadarConfigScreen(mod, config, this));
    }));
    addButton(new Button(col0, this.height / 6 + 48, 150, 20, new TranslatableComponent("civmodern.screen.main.ice"), button -> {
      Minecraft.getInstance().setScreen(new IceRoadConfigScreen(mod, config, this));
    }));
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    super.renderBackground(matrices);
    super.render(matrices, mouseX, mouseY, delta);

    cFont.drawShadow(matrices, this.title, this.width / 2f - this.font.width(this.title) / 2f, 40, 0xffffff);
  }
}
