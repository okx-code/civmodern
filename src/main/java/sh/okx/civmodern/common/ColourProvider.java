package sh.okx.civmodern.common;

import java.util.Objects;
import java.util.OptionalInt;

public class ColourProvider {

    private final CivMapConfig config;
    private Integer radarFg;
    private Integer radarBg;
    private Integer compacted;
    private Integer chevron;
    private Integer border;

    public ColourProvider(CivMapConfig config) {
        this.config = config;
    }

    public int getCompactedColour() {
        return Objects.requireNonNullElseGet(compacted, config::getColour);
    }

    /// Convenience function
    public OptionalInt getCrateColour() {
        return this.config.isCratesAreCompacted()
            ? OptionalInt.of(getCompactedColour())
            : OptionalInt.empty();
    }

    public int getForegroundColour() {
        return Objects.requireNonNullElseGet(radarFg, config::getRadarColour);
    }

    public int getBackgroundColour() {
        return Objects.requireNonNullElseGet(radarBg, config::getRadarBgColour);
    }

    public int getChevronColour() {
        return Objects.requireNonNullElseGet(chevron, config::getChevronColour);
    }

    public int getBorderColour() {
        return Objects.requireNonNullElseGet(border, config::getBorderColour);
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

    public void setTemporaryChevronColour(Integer colour) {
        chevron = colour;
    }

    public void setTemporaryBorderColour(Integer colour) {
        border = colour;
    }
}
