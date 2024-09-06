package sh.okx.civmodern.mod;

import java.util.Objects;

public final class ColourProvider {
    private static Integer compactedItemColour;

    public static int getCompactedItemColour() {
        return Objects.requireNonNullElse(compactedItemColour, CivModernConfig.compactedItemColour);
    }

    public static void setTemporaryCompactedItemColour(
        final Integer colour
    ) {
        compactedItemColour = colour;
    }
}
