package sh.okx.civmodern.mod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.features.CompactedItem;

public final class CivModernConfig {
    @SerialEntry(value = "items")
    public final ItemSettings itemSettings = new ItemSettings();

    @SerialEntry(value = "iceRoad")
    public final IceRoadSettings iceRoadSettings = new IceRoadSettings();

    // ============================================================
    // Serialisation
    // ============================================================

    public static ConfigClassHandler<CivModernConfig> HANDLER = ConfigClassHandler.createBuilder(CivModernConfig.class)
        .id(ResourceLocation.tryBuild("civmodern", "global_config"))
        .serializer((config) -> {
            return GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().getConfigDir().resolve("civmodern.json"))
                .setJson5(false)
                .build();
        })
        .build();

    public static void apply(
        final @NotNull CivModernConfig config
    ) {
        CompactedItem.CRATE_COLOUR = config.itemSettings.crateItemColour.getRGB();
        CompactedItem.COMPACTED_COLOUR = config.itemSettings.compactedItemColour.getRGB();
    }

    // ============================================================
    // Screen generation
    // ============================================================

    public static @NotNull YetAnotherConfigLib generateScreenGenerator(
        final @NotNull CivModernConfig config
    ) {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("category.civmodern"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("civmodern.config.category"))
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
