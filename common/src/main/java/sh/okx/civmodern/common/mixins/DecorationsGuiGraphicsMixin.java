package sh.okx.civmodern.common.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sh.okx.civmodern.common.AbstractCivModernMod;

@Mixin(GuiGraphics.class)
public abstract class DecorationsGuiGraphicsMixin {

	@Shadow
	private PoseStack pose;

	@Shadow
	private Minecraft minecraft;


	@Shadow abstract void fill(RenderType renderType, int i, int j, int k, int l, int m);

	@Shadow abstract int drawString(Font font, String formattedCharSequence, int i, int j, int k, boolean bl);

	@Overwrite
	public void renderItemDecorations(Font font, ItemStack itemStack, int i, int j, @Nullable String string) {
		// Changes to original source code:
		// - Move item damaged bar to before item count renderer
		// - Show item count if compacted
		// - Change colour if compacted

		LocalPlayer localPlayer;
		float f;
		int n;
		int m;
		if (itemStack.isEmpty()) {
			return;
		}
		this.pose.pushPose();
		if (itemStack.isBarVisible()) {
			int k = itemStack.getBarWidth();
			int l = itemStack.getBarColor();
			m = i + 2;
			n = j + 13;
			this.fill(RenderType.guiOverlay(), m, n, m + 13, n + 2, -16777216);
			this.fill(RenderType.guiOverlay(), m, n, m + k, n + 1, l | 0xFF000000);
		}
		boolean compacted = isCompacted(itemStack);
		if (itemStack.getCount() != 1 || string != null || compacted) {
			String string2 = string == null ? String.valueOf(itemStack.getCount()) : string;
			this.pose.translate(0.0f, 0.0f, 200.0f);
			int colour = compacted ? AbstractCivModernMod.getInstance().getColourProvider().getCompactedColour() : 0xffffff;
			this.drawString(font, string2, i + 19 - 2 - font.width(string2), j + 6 + 3, colour, true);
		}
		float f2 = f = (localPlayer = this.minecraft.player) == null ? 0.0f : localPlayer.getCooldowns().getCooldownPercent(itemStack.getItem(), this.minecraft.getFrameTime());
		if (f > 0.0f) {
			m = j + Mth.floor(16.0f * (1.0f - f));
			n = m + Mth.ceil(16.0f * f);
			this.fill(RenderType.guiOverlay(), i, m, i + 16, n, Integer.MAX_VALUE);
		}
		this.pose.popPose();
	}

	private boolean isCompacted(ItemStack item) {
		if (!item.hasTag()) {
			return false;
		}
		CompoundTag displayTag = item.getTagElement("display");
		if (displayTag != null && displayTag.getTagType("Lore") == 9) {
			ListTag listTag = displayTag.getList("Lore", 8);

			for (int i = 0; i < listTag.size(); i++) {
				String lore = listTag.getString(i);
				if (lore.contains("Compacted Item")) {
					return true;
				}
			}
		}
		return false;
	}
}
