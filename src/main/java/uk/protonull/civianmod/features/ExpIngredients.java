package uk.protonull.civianmod.features;

import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.CivianModHelpers;

public final class ExpIngredients {
    private static final Set<Item> EXP_MATERIALS = Set.of(
        Items.NETHER_WART,
        Items.MELON,
        Items.POTATO,
        Items.BONE,
        Items.COCOA_BEANS,
        Items.TWISTING_VINES,
        Items.KELP,
        Items.RED_MUSHROOM,
        Items.OAK_SAPLING,
        Items.COAL,
        Items.GLASS_BOTTLE,
        Items.CARROT,
        Items.VINE,
        Items.BAMBOO,
        Items.SPRUCE_SAPLING,
        Items.BROWN_MUSHROOM,
        Items.WEEPING_VINES,
        Items.SPIDER_EYE,
        Items.CACTUS,
        Items.PUMPKIN,
        Items.WHEAT,
        Items.REDSTONE,
        Items.RAW_COPPER,
        Items.GUNPOWDER,
        Items.CRIMSON_STEM,
        Items.BIRCH_SAPLING,
        Items.SWEET_BERRIES,
        Items.BEETROOT,
        Items.JUNGLE_SAPLING,
        Items.LAPIS_LAZULI,
        Items.WARPED_STEM,
        Items.ROTTEN_FLESH,
        Items.QUARTZ,
        Items.BLAZE_ROD,
        Items.SUGAR_CANE
    );

    public static boolean isExpIngredient(
        final @NotNull ItemStack item
    ) {
        return EXP_MATERIALS.contains(item.getItem())
            || isPlayerEssence(item);
    }

    private static boolean isPlayerEssence(
        final @NotNull ItemStack item
    ) {
        return item.getItem() == Items.ENDER_EYE
            && CivianModHelpers.hasPlainDisplayName(item, "Player Essence")
            && CivianModHelpers.hasPlainLoreLine(item, "Activity reward used to fuel pearls");
    }
}
