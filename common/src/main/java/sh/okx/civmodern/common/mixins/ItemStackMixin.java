package sh.okx.civmodern.common.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import sh.okx.civmodern.common.features.ExtendedItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ExtendedItemStack {
    @Unique
    private Boolean civmodern$isCompacted = null;

    @Unique
    @Override
    public boolean isMarkedAsCompacted() {
        if (this.civmodern$isCompacted == null) {
            this.civmodern$isCompacted = civmodern$isCompacted();
        }
        return this.civmodern$isCompacted;
    }

    @Unique
    private boolean civmodern$isCompacted() {
        final CompoundTag itemTag = ((ItemStack) (Object) this).getTag();
        if (itemTag == null) {
            return false;
        }
        if (!(itemTag.get(ItemStack.TAG_DISPLAY) instanceof final CompoundTag displayTag)) {
            return false;
        }
        if (!(displayTag.get(ItemStack.TAG_LORE) instanceof final ListTag loreTag)) {
            return false;
        }
        if (loreTag.getElementType() != Tag.TAG_STRING) {
            return false;
        }
        for (final Tag element : loreTag) {
            final Component line = Component.Serializer.fromJson(element.getAsString());
            if (line == null) {
                continue;
            }
            final var content = new StringBuilder();
            for (final Component child : line.toFlatList()) {
                if (!Style.EMPTY.equals(child.getStyle())) {
                    return false;
                }
                content.append(child.getString());
            }
            if (ExtendedItemStack.COMPACTED_ITEM_LORE.contentEquals(content)) {
                return true;
            }
        }
        return false;
    }
}
