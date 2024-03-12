package sh.okx.civmodern.common;

import java.util.Objects;

public class ColourProvider {

    private final CivMapConfig config;
    private Integer radarFg;
    private Integer radarBg;
    private Integer compacted;

    public ColourProvider(CivMapConfig config) {
        this.config = config;
    }

    public int getCompactedColour() {
        return Objects.requireNonNullElseGet(compacted, config::getColour);
    }

    public int getForegroundColour() {
        return Objects.requireNonNullElseGet(radarFg, config::getRadarColour);
    }

    public int getBackgroundColour() {
        return Objects.requireNonNullElseGet(radarBg, config::getRadarBgColour);
    }

    public void setTemporaryRadarForegroundColour(Integer colour) {
        radarFg = colour;
    }

    public void setTemporaryRadarBackgroundColour(Integer colour) {
        radarBg = colour;
    }

    public void setTemporaryCompactedColour(Integer colour) {
        compacted = colour;
    }
}
