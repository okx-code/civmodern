package uk.protonull.civianmod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.CivianMod;

public final class CivianModConfig {
    @SerialEntry(value = "items")
    public final ItemSettings itemSettings = new ItemSettings();

    @SerialEntry(value = "polyfills")
    public final PolyfillsSettings polyfillsSettings = new PolyfillsSettings();

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

    public static void migrate() {
        final Path configDir = FabricLoader.getInstance().getConfigDir();
        try {
            Files.copy(
                configDir.resolve("civmodern.json"),
                configDir.resolve("civianmod.json"),
                StandardCopyOption.COPY_ATTRIBUTES
            );
        }
        catch (final NoSuchFileException ignored) {
            // There's no CivModern config to migrate
        }
        catch (final FileAlreadyExistsException ignored) {
            // CivModern's config has already been migrated / a new config has already been made
        }
        catch (final IOException e) {
            CivianMod.LOGGER.warn("Could not migrate CivModern YACL config!", e);
        }
    }

    public void apply() {
        this.itemSettings.apply();
        this.polyfillsSettings.apply();
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
                    .group(PolyfillsSettings.generateGroup(config.polyfillsSettings))
                    .group(IceRoadSettings.generateGroup(config.iceRoadSettings))
                    .build()
            )
            .save(() -> {
                HANDLER.save();
                config.apply();
            })
            .build();
    }
}
