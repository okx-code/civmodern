package sh.okx.civmodern.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import sh.okx.civmodern.common.gui.Alignment;

public class CivMapConfig {
  private final File file;
  private int compactedColour;
  private int radarCircles;
  private int radarSize;
  private float iconSize;
  private Alignment alignment;
  private double range;
  private float transparency;
  private int radarColour;
  private int radarBgColour;
  private boolean radarEnabled;
  private boolean pingEnabled;
  private boolean pingSoundEnabled;
  private int x;
  private int y;
  private boolean iceRoadCardinalEnabled;
  private boolean iceRoadAutoEat;
  private boolean iceRoadStop;

  public CivMapConfig(File file, Properties properties) {
    this.file = file;
    this.compactedColour = Integer.parseInt(properties.getProperty("compacted_colour", "16777048"));
    this.radarCircles = Integer.parseInt(properties.getProperty("radar_circles", "4"));
    this.radarSize = Integer.parseInt(properties.getProperty("radar_size", "50"));
    this.alignment = Alignment.valueOf(properties.getProperty("alignment", "top_left").toUpperCase());
    this.iconSize = Float.parseFloat(properties.getProperty("icon_size", "1"));
    this.range = Double.parseDouble(properties.getProperty("range", "64"));
    this.transparency = Float.parseFloat(properties.getProperty("transparency", "0.8"));
    this.radarColour = Integer.parseInt(properties.getProperty("radar_colour", "8947848"));
    this.radarBgColour = Integer.parseInt(properties.getProperty("radar_background_colour", Integer.toString(radarColour)));
    this.x = Integer.parseInt(properties.getProperty("x", "100"));
    this.y = Integer.parseInt(properties.getProperty("y", "100"));
    this.radarEnabled = Boolean.parseBoolean(properties.getProperty("radar_enabled", "true"));
    this.pingEnabled = Boolean.parseBoolean(properties.getProperty("ping_enabled", "true"));
    this.pingSoundEnabled = Boolean.parseBoolean(properties.getProperty("ping_sound_enabled", "true"));
    this.iceRoadCardinalEnabled = Boolean.parseBoolean(properties.getProperty("ice_road_cardinal", "true"));
    this.iceRoadAutoEat = Boolean.parseBoolean(properties.getProperty("ice_road_auto_eat", "false"));
    this.iceRoadStop = Boolean.parseBoolean(properties.getProperty("ice_road_stop", "true"));
  }

  public void save() {
    try {
      Properties properties = new Properties();
      properties.setProperty("compacted_colour", Integer.toString(compactedColour));
      properties.setProperty("radar_circles", Integer.toString(radarCircles));
      properties.setProperty("radar_size", Integer.toString(radarSize));
      properties.setProperty("alignment", alignment.name().toLowerCase());
      properties.setProperty("icon_size", Float.toString(iconSize));
      properties.setProperty("range", Double.toString(range));
      properties.setProperty("transparency", Float.toString(transparency));
      properties.setProperty("radar_colour", Integer.toString(radarColour));
      properties.setProperty("radar_background_colour", Integer.toString(radarBgColour));
      properties.setProperty("x", Integer.toString(x));
      properties.setProperty("y", Integer.toString(y));
      properties.setProperty("radar_enabled", Boolean.toString(radarEnabled));
      properties.setProperty("ping_enabled", Boolean.toString(pingEnabled));
      properties.setProperty("ping_sound_enabled", Boolean.toString(pingEnabled));
      properties.setProperty("ice_road_cardinal", Boolean.toString(iceRoadCardinalEnabled));
      properties.setProperty("ice_road_auto_eat", Boolean.toString(iceRoadAutoEat));
      properties.setProperty("ice_road_stop", Boolean.toString(iceRoadStop));

      FileOutputStream output = new FileOutputStream(file);
      properties.store(output, null);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
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

  public void setIceRoadCardinalEnabled(boolean iceRoadCardinalEnabled) {
    this.iceRoadCardinalEnabled = iceRoadCardinalEnabled;
  }

  public boolean isIceRoadCardinalEnabled() {
    return iceRoadCardinalEnabled;
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
}
