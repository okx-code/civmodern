package sh.okx.civmodern.mod;

import java.util.Objects;

public final class ColourProvider {
    private static Integer compactedItemColour;
    private static Integer radarFg;
    private static Integer radarBg;

    public static int getCompactedItemColour() {
        return Objects.requireNonNullElse(compactedItemColour, CivModernConfig.compactedItemColour);
    }

    public static int getRadarForegroundColour() {
        return Objects.requireNonNullElse(radarFg, CivModernConfig.radarFgColour);
    }

    public static int getRadarBackgroundColour() {
        return Objects.requireNonNullElse(radarBg, CivModernConfig.radarBgColour);
    }

    public static void setTemporaryCompactedItemColour(
        final Integer colour
    ) {
        compactedItemColour = colour;
    }

    public static void setTemporaryRadarForegroundColour(
        Integer colour
    ) {
        radarFg = colour;
    }

    public static void setTemporaryRadarBackgroundColour(
        Integer colour
    ) {
        radarBg = colour;
    }
}
