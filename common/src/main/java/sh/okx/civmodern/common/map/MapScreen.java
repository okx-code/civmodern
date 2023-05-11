package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;

public class MapScreen extends Screen {

  private final AbstractCivModernMod mod;
  private final MapCache mapCache;

  public MapScreen(AbstractCivModernMod mod, MapCache mapCache) {
    super(new TranslatableComponent("civmodern.screen.map.title"));
    this.mod = mod;
    this.mapCache = mapCache;
    mapCache.lol2().render(mapCache.lol());
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    RegionTexture lol = mapCache.lol();
    if (lol != null) {
      lol.draw(matrices);
    }
  }

}
