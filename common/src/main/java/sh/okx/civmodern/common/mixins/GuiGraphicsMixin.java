package sh.okx.civmodern.common.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.features.ExtendedItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

  @Shadow
  private PoseStack pose;

  @Shadow
  abstract int drawString(Font font, @Nullable String string, int i, int j, int k, boolean bl);

  @SuppressWarnings("DataFlowIssue")
  @Overwrite
  private void renderItemCount(Font font, ItemStack itemStack, int i, int j, @Nullable String string) {
    boolean compacted = ((ExtendedItemStack) (Object) itemStack).isMarkedAsCompacted();
    if (itemStack.getCount() != 1 || string != null || compacted) {
      String string2 = string == null ? String.valueOf(itemStack.getCount()) : string;
      this.pose.pushPose();
      this.pose.translate(0.0F, 0.0F, 200.0F);
      int colour = compacted ? AbstractCivModernMod.getInstance().getColourProvider().getCompactedColour() : -1;
      this.drawString(font, string2, i + 19 - 2 - font.width(string2), j + 6 + 3, colour, true);
      this.pose.popPose();
    }
  }
}
