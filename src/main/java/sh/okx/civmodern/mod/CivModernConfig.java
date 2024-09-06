package sh.okx.civmodern.mod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CivModernConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivModernConfig.class);
    public static @NotNull File CONFIG_DIR = null, CONFIG_FILE = null; // Will be set during mod init

    public static int compactedItemColour;
    public static boolean showItemRepairLevel;

    public static boolean iceRoadPitchCardinalEnabled;
    public static boolean iceRoadYawCardinalEnabled;
    public static boolean iceRoadAutoEat;
    public static boolean iceRoadStop;

    @ApiStatus.Internal
    public static @NotNull Properties load() {
        final var properties = new Properties();
        InputStream configReadStream;
        try {
            configReadStream = new FileInputStream(CONFIG_FILE);
        }
        catch (final FileNotFoundException ignored) {
            final byte[] raw;
            try (final InputStream defaultConfigResource = CivModernMod.class.getResourceAsStream("/civmodern.properties")) {
                raw = defaultConfigResource.readAllBytes(); // Ignore highlighter
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not read CivModern's default config resource!", e);
            }
            CONFIG_FILE.getParentFile().mkdirs(); // Just in case
            try {
                FileUtils.writeByteArrayToFile(CONFIG_FILE, raw);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not save CivModern's default config resource!", e);
            }
            configReadStream = new ByteArrayInputStream(raw);
        }
        try {
            properties.load(configReadStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Could not parse CivModern's default config resource!", e);
        }
        return properties;
    }

    @ApiStatus.Internal
    public static void parse(
        final @NotNull Properties properties
    ) {
        compactedItemColour = Integer.parseInt(properties.getProperty("compacted_colour", "16777048"));
        showItemRepairLevel = Boolean.parseBoolean(properties.getProperty("show_repair_level", "true"));

        iceRoadPitchCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_pitch", "true"));
        iceRoadYawCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_yaw", "true"));
        iceRoadAutoEat = Boolean.parseBoolean(properties.getProperty("ice_road_auto_eat", "false"));
        iceRoadStop = Boolean.parseBoolean(properties.getProperty("ice_road_stop", "true"));
    }

    @ApiStatus.Internal
    public static void save() {
        final var properties = new Properties();
        properties.setProperty("compacted_colour", Integer.toString(compactedItemColour));
        properties.setProperty("show_repair_level", Boolean.toString(showItemRepairLevel));

        properties.setProperty("ice_road_cardinal_pitch", Boolean.toString(iceRoadPitchCardinalEnabled));
        properties.setProperty("ice_road_cardinal_yaw", Boolean.toString(iceRoadYawCardinalEnabled));
        properties.setProperty("ice_road_auto_eat", Boolean.toString(iceRoadAutoEat));
        properties.setProperty("ice_road_stop", Boolean.toString(iceRoadStop));

        CONFIG_DIR.mkdirs();
        try (final FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, null);
            LOGGER.info("Saved config to {}", CONFIG_FILE.getAbsolutePath());
        }
        catch (final IOException e) {
            LOGGER.warn("Could not save config to {}", CONFIG_FILE.getAbsolutePath(), e);
        }
    }
}
