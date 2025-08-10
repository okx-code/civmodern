package sh.okx.civmodern.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.gui.Alignment;

public class CivMapConfig {
    private static final Logger LOGGER = LogManager.getLogger(CivMapConfig.class);

    public static final int DEFAULT_RADAR_FG_COLOUR = 0x0D0202;
    public static final int DEFAULT_RADAR_BG_COLOUR = 0xE8E3E3;
    public static final int DEFAULT_CHEVRON_COLOUR = 0xFF0000;
    private final File file;
    private int compactedColour;
    private int radarCircles;
    private int radarSize;
    private float iconSize;
    private float textSize;
    private Alignment alignment;
    private double range;
    private float transparency;
    private float bgTransparency;
    private int radarColour;
    private int radarBgColour;
    private boolean radarEnabled;
    private boolean pingEnabled;
    private boolean pingSoundEnabled;
    private int x;
    private int y;
    private boolean iceRoadPitchCardinalEnabled;
    private boolean iceRoadYawCardinalEnabled;
    private boolean iceRoadAutoEat;
    private boolean iceRoadStop;
    private boolean showItems;
    private boolean northUp;
    private int chevronColour;
    private boolean minimapEnabled;
    private boolean mappingEnabled;
    private int minimapX;
    private int minimapY;
    private Alignment minimapAlignment;
    private int minimapSize;
    private boolean playerWaypointsEnabled;
    private float minimapZoom;
    private boolean cratesAreCompacted;
    private boolean showRepairCost;
    private boolean radarLogarithm;
    private boolean showMinimapCoords;

    public CivMapConfig(File file, Properties properties) {
        this.file = file;
        this.compactedColour = Integer.parseInt(properties.getProperty("compacted_colour", "16777048"));
        this.radarCircles = Integer.parseInt(properties.getProperty("radar_circles", "4"));
        this.radarSize = Integer.parseInt(properties.getProperty("radar_size", "80"));
        this.alignment = Alignment.valueOf(properties.getProperty("alignment", "top_left").toUpperCase());
        this.iconSize = Float.parseFloat(properties.getProperty("icon_size", "1"));
        this.textSize = Float.parseFloat(properties.getProperty("text_size", String.valueOf(this.iconSize)));
        this.range = Double.parseDouble(properties.getProperty("range", "64"));
        this.transparency = Float.parseFloat(properties.getProperty("transparency", "0.5"));
        this.radarColour = Integer.parseInt(properties.getProperty("radar_colour", Integer.toString(DEFAULT_RADAR_FG_COLOUR)));
        this.radarBgColour = Integer.parseInt(properties.getProperty("radar_background_colour", Integer.toString(DEFAULT_RADAR_BG_COLOUR)));
        this.x = Integer.parseInt(properties.getProperty("x", "5"));
        this.y = Integer.parseInt(properties.getProperty("y", "5"));
        this.radarEnabled = Boolean.parseBoolean(properties.getProperty("radar_enabled", "true"));
        this.pingEnabled = Boolean.parseBoolean(properties.getProperty("ping_enabled", "true"));
        this.pingSoundEnabled = Boolean.parseBoolean(properties.getProperty("ping_sound_enabled", "true"));
        this.iceRoadPitchCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_pitch", "true"));
        this.iceRoadYawCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal_yaw", "true"));
        this.iceRoadAutoEat = Boolean.parseBoolean(properties.getProperty("ice_road_auto_eat", "false"));
        this.iceRoadStop = Boolean.parseBoolean(properties.getProperty("ice_road_stop", "true"));
        this.bgTransparency = Float.parseFloat(properties.getProperty("bg_transparency", String.valueOf(this.transparency)));
        this.showItems = Boolean.parseBoolean(properties.getProperty("show_items", "true"));
        this.northUp = Boolean.parseBoolean(properties.getProperty("north_up", "false"));
        this.chevronColour = Integer.parseInt(properties.getProperty("chevron_colour", Integer.toString(DEFAULT_CHEVRON_COLOUR)));
        this.minimapEnabled = Boolean.parseBoolean(properties.getProperty("minimap_enabled", "true"));
        this.mappingEnabled = Boolean.parseBoolean(properties.getProperty("mapping_enabled", "true"));
        this.minimapX = Integer.parseInt(properties.getProperty("minimap_x", "5"));
        this.minimapY = Integer.parseInt(properties.getProperty("minimap_y", "5"));
        this.minimapAlignment = Alignment.valueOf(properties.getProperty("minimap_alignment", "top_right").toUpperCase());
        this.minimapSize = Integer.parseInt(properties.getProperty("minimap_size", "100"));
        this.playerWaypointsEnabled = Boolean.parseBoolean(properties.getProperty("player_waypoints_enabled", "true"));
        this.minimapZoom = Float.parseFloat(properties.getProperty("minimap_zoom", "4"));
        this.cratesAreCompacted = Boolean.parseBoolean(properties.getProperty("crates_are_compacted", "true"));
        this.showRepairCost = Boolean.parseBoolean(properties.getProperty("show_repair_cost", "true"));
        this.radarLogarithm = Boolean.parseBoolean(properties.getProperty("radar_logarithm", "false"));
        this.showMinimapCoords = Boolean.parseBoolean(properties.getProperty("show_minimap_coords", "true"));

    }

    public void save() {
        try {
            Properties properties = new Properties();
            properties.setProperty("compacted_colour", Integer.toString(compactedColour));
            properties.setProperty("radar_circles", Integer.toString(radarCircles));
            properties.setProperty("radar_size", Integer.toString(radarSize));
            properties.setProperty("alignment", alignment.name().toLowerCase());
            properties.setProperty("icon_size", Float.toString(iconSize));
            properties.setProperty("text_size", Float.toString(textSize));
            properties.setProperty("range", Double.toString(range));
            properties.setProperty("transparency", Float.toString(transparency));
            properties.setProperty("bg_transparency", Float.toString(bgTransparency));
            properties.setProperty("radar_colour", Integer.toString(radarColour));
            properties.setProperty("radar_background_colour", Integer.toString(radarBgColour));
            properties.setProperty("x", Integer.toString(x));
            properties.setProperty("y", Integer.toString(y));
            properties.setProperty("radar_enabled", Boolean.toString(radarEnabled));
            properties.setProperty("ping_enabled", Boolean.toString(pingEnabled));
            properties.setProperty("ping_sound_enabled", Boolean.toString(pingSoundEnabled));
            properties.setProperty("ice_road_cardinal_pitch", Boolean.toString(iceRoadPitchCardinalEnabled));
            properties.setProperty("ice_road_cardinal_yaw", Boolean.toString(iceRoadYawCardinalEnabled));
            properties.setProperty("ice_road_auto_eat", Boolean.toString(iceRoadAutoEat));
            properties.setProperty("ice_road_stop", Boolean.toString(iceRoadStop));
            properties.setProperty("show_items", Boolean.toString(showItems));
            properties.setProperty("north_up", Boolean.toString(northUp));
            properties.setProperty("chevron_colour", Integer.toString(chevronColour));
            properties.setProperty("minimap_enabled", Boolean.toString(minimapEnabled));
            properties.setProperty("minimap_x", Integer.toString(minimapX));
            properties.setProperty("minimap_y", Integer.toString(minimapY));
            properties.setProperty("minimap_alignment", minimapAlignment.name().toLowerCase());
            properties.setProperty("minimap_size", Integer.toString(minimapSize));
            properties.setProperty("player_waypoints_enabled", Boolean.toString(playerWaypointsEnabled));
            properties.setProperty("minimap_zoom", Float.toString(minimapZoom));
            properties.setProperty("crates_are_compacted", Boolean.toString(cratesAreCompacted));
            properties.setProperty("show_repair_cost", Boolean.toString(showRepairCost));
            properties.setProperty("radar_logarithm", Boolean.toString(radarLogarithm));
            properties.setProperty("show_minimap_coords", Boolean.toString(showMinimapCoords));

            try (FileOutputStream output = new FileOutputStream(file)) {
                properties.store(output, null);
                LOGGER.info("Saved config to " + file.getAbsolutePath());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isShowItems() {
        return showItems;
    }

    public void setShowItems(boolean showItems) {
        this.showItems = showItems;
    }

    public int getColour() {
        return compactedColour;
    }

    public void setColour(int compactedColour) {
        this.compactedColour = compactedColour;
    }

    public void setRadarCircles(int radarCircles) {
        this.radarCircles = radarCircles;
    }

    public int getRadarCircles() {
        return radarCircles;
    }

    public int getRadarSize() {
        return radarSize;
    }

    public void setRadarSize(int radarSize) {
        this.radarSize = radarSize;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public float getIconSize() {
        return iconSize;
    }

    public void setIconSize(float iconSize) {
        this.iconSize = iconSize;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

    public float getBackgroundTransparency() {
        return bgTransparency;
    }

    public void setBackgroundTransparency(float bgTransparency) {
        this.bgTransparency = bgTransparency;
    }

    public int getRadarColour() {
        return radarColour;
    }

    public void setRadarColour(int radarColour) {
        this.radarColour = radarColour;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isRadarEnabled() {
        return radarEnabled;
    }

    public void setRadarEnabled(boolean radarEnabled) {
        this.radarEnabled = radarEnabled;
    }

    public boolean isPingEnabled() {
        return pingEnabled;
    }

    public void setPingEnabled(boolean pingEnabled) {
        this.pingEnabled = pingEnabled;
    }

    public void setIceRoadPitchCardinalEnabled(boolean iceRoadPitchCardinalEnabled) {
        this.iceRoadPitchCardinalEnabled = iceRoadPitchCardinalEnabled;
    }

    public void setIceRoadYawCardinalEnabled(boolean iceRoadYawCardinalEnabled) {
        this.iceRoadYawCardinalEnabled = iceRoadYawCardinalEnabled;
    }

    public boolean iceRoadPitchCardinalEnabled() {
        return iceRoadPitchCardinalEnabled;
    }

    public boolean iceRoadYawCardinalEnabled() {
        return iceRoadYawCardinalEnabled;
    }

    public void setIceRoadAutoEat(boolean iceRoadAutoEat) {
        this.iceRoadAutoEat = iceRoadAutoEat;
    }

    public boolean isIceRoadAutoEat() {
        return iceRoadAutoEat;
    }

    public void setIceRoadStop(boolean iceRoadStop) {
        this.iceRoadStop = iceRoadStop;
    }

    public boolean isIceRoadStop() {
        return iceRoadStop;
    }

    public boolean isPingSoundEnabled() {
        return pingSoundEnabled;
    }

    public void setPingSoundEnabled(boolean pingSoundEnabled) {
        this.pingSoundEnabled = pingSoundEnabled;
    }

    public int getRadarBgColour() {
        return radarBgColour;
    }

    public void setRadarBgColour(int radarBgColour) {
        this.radarBgColour = radarBgColour;
    }

    public boolean isNorthUp() {
        return northUp;
    }

    public void setNorthUp(boolean northUp) {
        this.northUp = northUp;
    }

    public void setChevronColour(int chevronColour) {
        this.chevronColour = chevronColour;
    }

    public int getChevronColour() {
        return chevronColour;
    }

    public boolean isMinimapEnabled() {
        return minimapEnabled;
    }

    public void setMinimapEnabled(boolean minimapEnabled) {
        this.minimapEnabled = minimapEnabled;
    }

    public boolean isMappingEnabled() {
        return mappingEnabled;
    }

    public void setMappingEnabled(boolean mappingEnabled) {
        this.mappingEnabled = mappingEnabled;
    }

    public Alignment getMinimapAlignment() {
        return minimapAlignment;
    }

    public void setMinimapAlignment(Alignment minimapAlignment) {
        this.minimapAlignment = minimapAlignment;
    }

    public int getMinimapSize() {
        return minimapSize;
    }

    public void setMinimapSize(int minimapSize) {
        this.minimapSize = minimapSize;
    }

    public int getMinimapX() {
        return minimapX;
    }

    public void setMinimapX(int minimapX) {
        this.minimapX = minimapX;
    }

    public int getMinimapY() {
        return minimapY;
    }

    public void setMinimapY(int minimapY) {
        this.minimapY = minimapY;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public boolean isPlayerWaypointsEnabled() {
        return playerWaypointsEnabled;
    }

    public void setPlayerWaypointsEnabled(boolean playerWaypointsEnabled) {
        this.playerWaypointsEnabled = playerWaypointsEnabled;
    }

    public float getMinimapZoom() {
        return minimapZoom;
    }

    public void setMinimapZoom(float minimapZoom) {
        this.minimapZoom = minimapZoom;
    }

    public boolean isCratesAreCompacted() {
        return cratesAreCompacted;
    }

    public void setCratesAreCompacted(boolean cratesAreCompacted) {
        this.cratesAreCompacted = cratesAreCompacted;
    }

    public boolean isShowRepairCost() {
        return showRepairCost;
    }

    public void setShowRepairCost(boolean showRepairCost) {
        this.showRepairCost = showRepairCost;
    }

    public boolean isRadarLogarithm() {
        return radarLogarithm;
    }

    public void setRadarLogarithm(boolean radarLogarithm) {
        this.radarLogarithm = radarLogarithm;
    }

    public boolean isShowMinimapCoords() {
        return showMinimapCoords;
    }

    public void setShowMinimapCoords(boolean showMinimapCoords) {
        this.showMinimapCoords = showMinimapCoords;
    }
}
