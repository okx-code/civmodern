package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

// Makes sure waypoint textures are anti aliases, they are not in the default minecraft texture
public class WaypointTexture extends AbstractTexture {

  private final ResourceLocation location;

  public WaypointTexture(ResourceLocation location) {
    this.location = location;
  }

  @Override
  public void load(ResourceManager resourceManager) throws IOException {
    Resource resource = resourceManager.getResource(this.location);
    NativeImage nativeImage = NativeImage.read(resource.getInputStream());
    TextureUtil.prepareImage(this.getId(), 0, nativeImage.getWidth(), nativeImage.getHeight());
    nativeImage.upload(0, 0, 0, 0, 0, nativeImage.getWidth(), nativeImage.getHeight(), true, false, false, true);
  }

  public void register() {
    Minecraft.getInstance().getTextureManager().register(this.location, this);
  }
}