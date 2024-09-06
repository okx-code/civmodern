package sh.okx.civmodern.mod.mixins;

import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sh.okx.civmodern.mod.CivModernConfig;
import sh.okx.civmodern.mod.features.ExtendedItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ExtendedItemStack {
    @Shadow
    public abstract DataComponentMap getComponents();

    // ============================================================
    // Compacted item detection
    // ============================================================

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

    // ============================================================
    // Showing an item's repair level
    //
    // @ModifyVariable seems to have some difficulty finding the List<Component> local variable, likewise with local
    // capture and @Local. Anything that works shows red lines in the IDE, and anything that the IDE thinks should work
    // throws while testing. This may just be an issue with the Minecraft Development plugin. Nonetheless, I've done it
    // in a roundabout way to ensure stability.
    // ============================================================

    @Unique
    private List<Component> civmodern$tooltipLines = null;

    @ModifyVariable(
        method = "getTooltipLines",
        at = @At("STORE")
    )
    private @NotNull List<Component> civmodern$captureTooltipLines(
        final @NotNull List<Component> tooltipLines
    ) {
        return this.civmodern$tooltipLines = tooltipLines;
    }

    @Inject(
        method = "getTooltipLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isDamaged()Z",
            shift = At.Shift.BEFORE
        )
    )
    private void civmodern$showRepairLevel(
        final @NotNull CallbackInfoReturnable<List<Component>> cir
    ) {
        if (CivModernConfig.showItemRepairLevel) {
            final int repairCost = getComponents().getOrDefault(DataComponents.REPAIR_COST, 0);
            if (repairCost > 0) {
                this.civmodern$tooltipLines.add(Component.translatable(
                    "civmodern.repair.level",
                    Integer.toString(repairCost)
                ));
            }
        }
    }

    @Inject(
        method = "getTooltipLines",
        at = @At("RETURN")
    )
    private void civmodern$releaseTooltipLines(
        final @NotNull CallbackInfoReturnable<List<Component>> cir
    ) {
        this.civmodern$tooltipLines = null;
    }
}
