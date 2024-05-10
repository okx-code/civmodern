package sh.okx.civmodern.common.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

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

    private HsbColourPicker picker;

    public CompactedConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
        super(Component.translatable("civmodern.screen.compacted.title"));
        this.mod = mod;
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        itemX = this.width / 2 - 16 / 2;
        itemY = this.height / 6 - 24;

        int leftWidth = width / 2 - (60 + 8 + 20 + 8 + 20) / 2;

        EditBox widget = new EditBox(font, leftWidth, height / 6, 60, 20, Component.empty());
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
        addRenderableWidget(widget);

        ColourProvider colourProvider = mod.getColourProvider();
        HsbColourPicker hsb = new HsbColourPicker(leftWidth + 60 + 8, height / 6, 20, 20, config.getColour(),
            colour -> {
                widget.setValue("#" + String.format("%06X", colour));
                config.setColour(colour);
            }, colourProvider::setTemporaryCompactedColour, () -> {});

        addRenderableWidget(new ImageButton(leftWidth + 60 + 8 + 20 + 8, height / 6, 20, 20, new ResourceLocation("civmodern", "gui/rollback.png"), imbg -> {
            int colour = 0xffff58;
            widget.setValue("#FFFF58");
            config.setColour(colour);
            hsb.close();
        }));

        addRenderableWidget(picker = hsb);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            config.save();
            minecraft.setScreen(parent);
        }).pos(this.width / 2 - 49, this.height / 6 + 169).size(98, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        drawCentredText(guiGraphics, this.title, 0, 15, 0xffffff);

        drawItem(guiGraphics);

        if (isCursorOverItem(mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, ITEM, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (isCursorOverItem((int) mouseX, (int) mouseY) && button == 0 && player.isCreative()) {
            player.addItem(ITEM.copy());
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
        if (picker != null) {
            picker.mouseMoved(d, e);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        mod.getColourProvider().setTemporaryCompactedColour(null);
        config.save();
    }

    private boolean isCursorOverItem(int mouseX, int mouseY) {
        return mouseX >= itemX - 1  && mouseX < itemX + 17 && mouseY > itemY - 1 && mouseY < itemY + 17;
    }

    private void drawItem(GuiGraphics guiGraphics) {
        guiGraphics.renderItem(ITEM, itemX, itemY);
        guiGraphics.renderItemDecorations(font, ITEM, itemX, itemY);
    }

    private void drawCentredText(GuiGraphics guiGraphics, Component text, int xOffsetCentre, int yOffsetTop, int colour) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int centre = width / 2 - font.width(text) / 2;
        guiGraphics.drawString(this.font, text, centre + xOffsetCentre, yOffsetTop, colour);
    }
}
