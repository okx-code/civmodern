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
import sh.okx.civmodern.mod.gui.Alignment;

public final class CivModernConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivModernConfig.class);
    public static @NotNull File CONFIG_DIR = null, CONFIG_FILE = null; // Will be set during mod init

    public static final int DEFAULT_RADAR_FG_COLOUR = 0x0D0202;
    public static final int DEFAULT_RADAR_BG_COLOUR = 0xE8E3E3;

    public static int compactedItemColour;

    public static boolean radarEnabled;
    public static @NotNull Alignment radarAlignment = Alignment.TOP_LEFT;
    public static int radarX;
    public static int radarY;
    public static int radarSize;
    public static double radarRange;
    public static int radarCircles;
    public static float radarIconSize;
    public static float radarTransparency;
    public static float radarBackgroundTransparency;
    public static int radarFgColour = DEFAULT_RADAR_FG_COLOUR;
    public static int radarBgColour = DEFAULT_RADAR_BG_COLOUR;
    public static boolean radarPingsEnabled;
    public static boolean radarPingSoundEnabled;
    public static boolean radarShowItems;
    public static boolean radarIsNorthUp;

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

        radarEnabled = Boolean.parseBoolean(properties.getProperty("radar_enabled", "true"));
        radarAlignment = Alignment.valueOf(properties.getProperty("alignment", "top_left").toUpperCase());
        radarX = Integer.parseInt(properties.getProperty("x", "5"));
        radarY = Integer.parseInt(properties.getProperty("y", "5"));
        radarSize = Integer.parseInt(properties.getProperty("radar_size", "80"));
        radarRange = Double.parseDouble(properties.getProperty("range", "64"));
        radarCircles = Integer.parseInt(properties.getProperty("radar_circles", "4"));
        radarIconSize = Float.parseFloat(properties.getProperty("icon_size", "1"));
        radarTransparency = Float.parseFloat(properties.getProperty("transparency", "0.5"));
        radarBackgroundTransparency = Float.parseFloat(properties.getProperty("bg_transparency", String.valueOf(radarTransparency)));
        radarFgColour = Integer.parseInt(properties.getProperty("radar_colour", Integer.toString(DEFAULT_RADAR_FG_COLOUR)));
        radarBgColour = Integer.parseInt(properties.getProperty("radar_background_colour", Integer.toString(DEFAULT_RADAR_BG_COLOUR)));
        radarPingsEnabled = Boolean.parseBoolean(properties.getProperty("ping_enabled", "true"));
        radarPingSoundEnabled = Boolean.parseBoolean(properties.getProperty("ping_sound_enabled", "true"));
        radarShowItems = Boolean.parseBoolean(properties.getProperty("show_items", "true"));
        radarIsNorthUp = Boolean.parseBoolean(properties.getProperty("north_up", "false"));

        iceRoadPitchCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_pitch", "true"));
        iceRoadYawCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_yaw", "true"));
        iceRoadAutoEat = Boolean.parseBoolean(properties.getProperty("ice_road_auto_eat", "false"));
        iceRoadStop = Boolean.parseBoolean(properties.getProperty("ice_road_stop", "true"));
    }

    @ApiStatus.Internal
    public static void save() {
        final var properties = new Properties();
        properties.setProperty("compacted_colour", Integer.toString(compactedItemColour));

        properties.setProperty("radar_enabled", Boolean.toString(radarEnabled));
        properties.setProperty("alignment", radarAlignment.name());
        properties.setProperty("x", Integer.toString(radarX));
        properties.setProperty("y", Integer.toString(radarY));
        properties.setProperty("radar_size", Integer.toString(radarSize));
        properties.setProperty("range", Double.toString(radarRange));
        properties.setProperty("radar_circles", Integer.toString(radarCircles));
        properties.setProperty("icon_size", Float.toString(radarIconSize));
        properties.setProperty("transparency", Float.toString(radarTransparency));
        properties.setProperty("bg_transparency", Float.toString(radarBackgroundTransparency));
        properties.setProperty("radar_colour", Integer.toString(radarFgColour));
        properties.setProperty("radar_background_colour", Integer.toString(radarBgColour));
        properties.setProperty("ping_enabled", Boolean.toString(radarPingsEnabled));
        properties.setProperty("ping_sound_enabled", Boolean.toString(radarPingSoundEnabled));
        properties.setProperty("show_items", Boolean.toString(radarShowItems));
        properties.setProperty("north_up", Boolean.toString(radarIsNorthUp));

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
