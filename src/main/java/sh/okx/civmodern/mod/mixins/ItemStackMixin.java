package sh.okx.civmodern.mod.mixins;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import sh.okx.civmodern.mod.features.ExtendedItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ExtendedItemStack {
    @Shadow
    public abstract DataComponentMap getComponents();

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
        final ItemLore lore = getComponents().get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (final Component line : lore.lines()) {
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
