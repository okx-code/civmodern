package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;

public class MapScreen extends Screen {

  private final AbstractCivModernMod mod;
  private final MapCache mapCache;

  private double x;
  private double y;

  public MapScreen(AbstractCivModernMod mod, MapCache mapCache) {
    super(new TranslatableComponent("civmodern.screen.map.title"));
    this.mod = mod;
    this.mapCache = mapCache;
    Window window = Minecraft.getInstance().getWindow();

    x = Minecraft.getInstance().player.getX();
    y = Minecraft.getInstance().player.getZ();

    // TODO rendering algorithm
/*
    for (int screenX = 0; screenX < window.getGuiScaledWidth() + 512; screenX += 512) {
      for (int screenY = 0; screenY < window.getGuiScaledHeight() + 512; screenY += 512) {
        int realX = (int) this.x + screenX;
        int realY = (int) this.z + screenY;

        int renderX = realX - Math.floorMod(realX, 512);
        int renderY = realY - Math.floorMod(realY, 512);

        RegionKey key = new RegionKey(Math.floorDiv(renderX, 512), Math.floorDiv(renderY, 512));
        RegionData data = mapCache.getData(key);
        RegionTexture texture = mapCache.getTexture(key);
        if (data != null && texture != null) {
          data.render(texture);
        }
      }
    }
*/
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    Window window = Minecraft.getInstance().getWindow();

    for (int screenX = 0; screenX < window.getWidth() + 512; screenX += 512) {
      for (int screenY = 0; screenY < window.getHeight() + 512; screenY += 512) {
        int realX = (int) this.x + screenX;
        int realY = (int) this.y + screenY;

        int renderX = realX - Math.floorMod(realX, 512);
        int renderY = realY - Math.floorMod(realY, 512);

        RegionKey key = new RegionKey(Math.floorDiv(renderX, 512), Math.floorDiv(renderY, 512));
        RegionTexture texture = mapCache.getTexture(key);
        if (texture != null) {
          float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
          texture.draw(matrices, (float) ((renderX - this.x)) / scale, (float) ((renderY - this.y)) / scale);
        }
      }
    }
  }

  @Override
  public void onClose() {
    super.onClose();
    mapCache.save();
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double changeX, double changeY) {
    if (button == 0) {
      double scale =  Minecraft.getInstance().getWindow().getGuiScale();
      this.x -= changeX / scale;
      this.y -= changeY / scale;
      return true;
    } else {
      return false;
    }
    // 0 = left
    // 1 = right
    // 2 = middle
  }
}
