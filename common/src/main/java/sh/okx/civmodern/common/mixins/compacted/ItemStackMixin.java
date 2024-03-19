package sh.okx.civmodern.common.mixins.compacted;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import sh.okx.civmodern.common.features.compacted.PotentiallyCompactedItem;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements PotentiallyCompactedItem {
    @Unique
    private Boolean cm_unique$isCompacted = null;

    @Unique
    @Override
    public boolean isMarkedAsCompacted() {
        if (this.cm_unique$isCompacted == null) {
            this.cm_unique$isCompacted = cm_unique$isCompacted();
        }
        return this.cm_unique$isCompacted;
    }

    @Unique
    private boolean cm_unique$isCompacted() {
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
            return PotentiallyCompactedItem.COMPACTED_ITEM_LORE.contentEquals(content);
        }
        return false;
    }
}
