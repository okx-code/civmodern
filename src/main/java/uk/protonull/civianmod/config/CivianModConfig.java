package uk.protonull.civianmod.config;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.awt.Color;
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
import uk.protonull.civianmod.config.gui.IntegrationConfigGui;
import uk.protonull.civianmod.config.gui.ItemConfigGui;
import uk.protonull.civianmod.config.gui.MacroConfigGui;
import uk.protonull.civianmod.config.old.LegacyIceRoadSettings;
import uk.protonull.civianmod.config.old.LegacyItemSettings;
import uk.protonull.civianmod.config.old.LegacyPolyfillsSettings;
import uk.protonull.civianmod.features.ClickRailDest;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.features.ItemDurability;
import uk.protonull.civianmod.features.SafeMining;
import uk.protonull.civianmod.features.macros.IceRoadMacro;

public final class CivianModConfig {
    @SerialEntry
    public @NotNull Color itemColourCrate = CompactedItem.CRATE.defaultAwtColor;
    @SerialEntry
    public @NotNull Color itemColourCompacted = CompactedItem.COMPACTED.defaultAwtColor;

    @SerialEntry
    public @NotNull TooltipLineOption showRepairLevel = ItemDurability.DEFAULT_SHOW_REPAIR_LEVEL;
    @SerialEntry
    public @NotNull TooltipLineOption showDamageLevel = ItemDurability.DEFAULT_SHOW_DAMAGE_LEVEL;

    @SerialEntry
    public boolean showExpTooltip = ExpIngredients.DEFAULT_ENABLED;

    @SerialEntry
    public boolean safeMiningEnabled = SafeMining.DEFAULT_ENABLED;
    @SerialEntry
    public int safeMiningThreshold = SafeMining.DEFAULT_THRESHOLD;

    @SerialEntry
    public boolean clickDestEnabled = ClickRailDest.DEFAULT_ENABLED;
    @SerialEntry
    public long clickDestCooldown = ClickRailDest.DEFAULT_COOLDOWN;

    @SerialEntry
    public boolean iceRoadSnapPitch = IceRoadMacro.DEFAULT_SNAP_PITCH;
    @SerialEntry
    public boolean iceRoadSnapYaw = IceRoadMacro.DEFAULT_SNAP_YAW;
    @SerialEntry
    public boolean iceRoadAutoEat = IceRoadMacro.DEFAULT_AUTO_EAT;
    @SerialEntry
    public boolean iceRoadStopWhenStarving = IceRoadMacro.DEFAULT_STOP_WHEN_STARVING;

    public void apply() {
        // Items tab
        CompactedItem.CRATE.colour = this.itemColourCrate.getRGB();
        CompactedItem.COMPACTED.colour = this.itemColourCompacted.getRGB();
        ItemDurability.showDamageLevel = this.showDamageLevel;
        ItemDurability.showRepairLevel = this.showRepairLevel;
        ExpIngredients.enabled = this.showExpTooltip;
        SafeMining.enabled = this.safeMiningEnabled;
        SafeMining.threshold = this.safeMiningThreshold;
        // Integrations tab
        ClickRailDest.enabled = this.clickDestEnabled;
        ClickRailDest.cooldown = this.clickDestCooldown;
        // Macros tab
        IceRoadMacro.snapPitch = this.iceRoadSnapPitch;
        IceRoadMacro.snapYaw = this.iceRoadSnapYaw;
        IceRoadMacro.autoEat = this.iceRoadAutoEat;
        IceRoadMacro.stopWhenStarving = this.iceRoadStopWhenStarving;
    }

    // ============================================================
    // Legacy structure
    // ============================================================

    @SerialEntry(value = "items", required = false, nullable = true)
    public LegacyItemSettings legacyItemSettings = null;
    @SerialEntry(value = "polyfills", required = false, nullable = true)
    public LegacyPolyfillsSettings legacyPolyfillsSettings = null;
    @SerialEntry(value = "iceRoad", required = false, nullable = true)
    public LegacyIceRoadSettings legacyIceRoadSettings = null;

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

    public static void migrateFromCivModernConfig() {
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

    public static boolean migrateToFlattenedConfig(
        final @NotNull CivianModConfig config
    ) {
        boolean hadLegacySettings = false;
        if (config.legacyItemSettings instanceof final LegacyItemSettings legacy) {
            config.itemColourCrate = legacy.crateItemColour;
            config.itemColourCompacted = legacy.compactedItemColour;
            config.showRepairLevel = legacy.showRepairLevel;
            config.showDamageLevel = legacy.showDamageLevel;
            config.showExpTooltip = legacy.showIsExpIngredient;
            config.safeMiningEnabled = legacy.safeMiningEnabled;
            config.safeMiningThreshold = legacy.safeMiningThreshold;
            config.legacyItemSettings = null; // Remove legacy
            hadLegacySettings = true;
        }
        if (config.legacyPolyfillsSettings instanceof final LegacyPolyfillsSettings legacy) {
            config.clickDestEnabled = legacy.clickDestEnabled;
            config.clickDestCooldown = legacy.clickDestCooldown;
            config.legacyPolyfillsSettings = null; // Remove legacy
            hadLegacySettings = true;
        }
        if (config.legacyIceRoadSettings instanceof final LegacyIceRoadSettings legacy) {
            config.iceRoadSnapPitch = legacy.snapPitch;
            config.iceRoadSnapYaw = legacy.snapYaw;
            config.iceRoadAutoEat = legacy.autoEat;
            config.iceRoadStopWhenStarving = legacy.stopWhenHungry;
            config.legacyIceRoadSettings = null; // Remove legacy
            hadLegacySettings = true;
        }
        return hadLegacySettings;
    }

    // ============================================================
    // Screen generation
    // ============================================================

    public static @NotNull YetAnotherConfigLib generateScreenGenerator(
        final @NotNull CivianModConfig config
    ) {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("category.civianmod"))
            .category(ItemConfigGui.generateCategory(config))
            .category(IntegrationConfigGui.generateCategory(config))
            .category(MacroConfigGui.generateCategory(config))
            .save(() -> {
                HANDLER.save();
                config.apply();
            })
            .build();
    }
}
