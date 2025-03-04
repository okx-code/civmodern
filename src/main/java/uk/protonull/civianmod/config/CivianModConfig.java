package uk.protonull.civianmod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.features.CompactedItem;

public final class CivianModConfig {
    @SerialEntry(value = "items")
    public final ItemSettings itemSettings = new ItemSettings();

    @SerialEntry(value = "iceRoad")
    public final IceRoadSettings iceRoadSettings = new IceRoadSettings();

    // ============================================================
    // Serialisation
    // ============================================================

    public static ConfigClassHandler<CivianModConfig> HANDLER = ConfigClassHandler.createBuilder(CivianModConfig.class)
        .id(ResourceLocation.tryBuild("civianmod", "global_config"))
        .serializer((config) -> {
            return GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().getConfigDir().resolve("civianmod.json"))
                .setJson5(false)
                .build();
        })
        .build();

    public static void apply(
        final @NotNull CivianModConfig config
    ) {
        CompactedItem.CRATE_COLOUR = config.itemSettings.crateItemColour.getRGB();
        CompactedItem.COMPACTED_COLOUR = config.itemSettings.compactedItemColour.getRGB();
    }

    // ============================================================
    // Screen generation
    // ============================================================

    public static @NotNull YetAnotherConfigLib generateScreenGenerator(
        final @NotNull CivianModConfig config
    ) {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("category.civianmod"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("civianmod.config.category"))
                    .group(ItemSettings.generateGroup(config.itemSettings))
                    .group(IceRoadSettings.generateGroup(config.iceRoadSettings))
                    .build()
            )
            .save(() -> {
                HANDLER.save();
                apply(config);
            })
            .build();
    }
}
