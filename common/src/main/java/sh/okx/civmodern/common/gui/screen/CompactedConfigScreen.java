package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DecimalFormat;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.compat.CommonFont;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;

public class CompactedConfigScreen extends Screen {
  private static final DecimalFormat FORMAT = new DecimalFormat("##%");

  private static final ItemStack ITEM;

  private int itemX;
  private int itemY;

  static {
    CompoundTag item = new CompoundTag();
    item.putString("id", "stone");
    item.putInt("Count", 64);
    CompoundTag tag = new CompoundTag();
    ListTag lore = new ListTag();
    lore.add(StringTag.valueOf("\"Compacted Item\""));
    CompoundTag display = new CompoundTag();
    display.put("Lore", lore);
    tag.put("display", display);
    item.put("tag", tag);
    ITEM = ItemStack.of(item);
  }

  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private final Screen parent;
  private CommonFont cFont;

  public CompactedConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
    super(new TranslatableComponent("civmodern.screen.compacted.title"));
    this.mod = mod;
    this.config = config;
    this.parent = parent;
  }

  @Override
  protected void init() {
    this.cFont = mod.getCompat().provideFont(this.font);

    itemX = this.width / 2 - 16 / 2;
    itemY = this.height / 6 - 24;

    int leftWidth = width / 2 - (60 + 8 + 20 + 8 + 20) / 2;

    EditBox widget = new EditBox(font, leftWidth, height / 6, 60, 20, TextComponent.EMPTY);
    widget.setValue("#" + String.format("%06X", config.getColour()));
    widget.setMaxLength(7);
    Pattern pattern = Pattern.compile("^(#[0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);
    widget.setFilter(string -> pattern.matcher(string).matches());
    widget.setResponder(val -> {
      if (val.length() == 7) {
        int rgb = Integer.parseInt(val.substring(1), 16);
        config.setColour(rgb);
      }
    });
    addButton(widget);

    HsbColourPicker hsb = new HsbColourPicker(leftWidth + 60 + 8, height / 6, 20, 20, config.getColour(),
        colour -> {
          widget.setValue("#" + String.format("%06X", colour));
          config.setColour(colour);
        });

    addButton(new ImageButton(leftWidth + 60 + 8 + 20 + 8, height / 6, 20, 20, new ResourceLocation("civmodern", "gui/rollback.png"), imbg -> {
      int colour = 0xffff58;
      widget.setValue("#FFFF58");
      config.setColour(colour);
      hsb.close();
    }));

    addButton(hsb);


    addButton(new Button(this.width / 2 - 49, this.height / 6 + 169, 98, 20, CommonComponents.GUI_DONE, button -> {
      Minecraft.getInstance().setScreen(parent);
    }));
  }

  @Override
  public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    super.renderBackground(matrices);

    drawCentredText(matrices, this.title, 0, 40, 0xffffff);

    drawItem();

    if (isCursorOverItem(mouseX, mouseY)) {
      this.renderTooltip(matrices, ITEM, mouseX, mouseY);
    }

    super.render(matrices, mouseX, mouseY, delta);
  }

  @Override
  public void onClose() {
    super.onClose();
    config.save();
  }

  private boolean isCursorOverItem(int mouseX, int mouseY) {
    return mouseX >= itemX - 1  && mouseX < itemX + 17 && mouseY > itemY - 1 && mouseY < itemY + 17;
  }

  private void drawItem() {
    itemRenderer.renderGuiItem(ITEM, itemX, itemY);
    itemRenderer.renderGuiItemDecorations(font, ITEM, itemX, itemY);
  }

  private void drawCentredText(PoseStack matrix, Component text, int xOffsetCentre, int yOffsetTop, int colour) {
    int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int centre = width / 2 - font.width(text) / 2;
    this.cFont.drawShadow(matrix, text, centre + xOffsetCentre, yOffsetTop, colour);
  }
}
