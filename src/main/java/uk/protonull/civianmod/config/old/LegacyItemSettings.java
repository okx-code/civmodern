package uk.protonull.civianmod.config.old;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.config.TooltipLineOption;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.features.ItemDurability;
import uk.protonull.civianmod.features.SafeMining;

public final class LegacyItemSettings {
    @SerialEntry
    public @NotNull Color crateItemColour = CompactedItem.CRATE.defaultAwtColor;
    @SerialEntry
    public @NotNull Color compactedItemColour = CompactedItem.COMPACTED.defaultAwtColor;
    @SerialEntry
    public @NotNull TooltipLineOption showRepairLevel = ItemDurability.DEFAULT_SHOW_REPAIR_LEVEL;
    @SerialEntry
    public @NotNull TooltipLineOption showDamageLevel = ItemDurability.DEFAULT_SHOW_DAMAGE_LEVEL;
    @SerialEntry
    public boolean showIsExpIngredient = ExpIngredients.DEFAULT_ENABLED;
    @SerialEntry
    public boolean safeMiningEnabled = SafeMining.DEFAULT_ENABLED;
    @SerialEntry
    public int safeMiningThreshold = SafeMining.DEFAULT_THRESHOLD;
}
