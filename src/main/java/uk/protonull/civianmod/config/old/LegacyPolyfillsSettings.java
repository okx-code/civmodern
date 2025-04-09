package uk.protonull.civianmod.config.old;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import uk.protonull.civianmod.features.ClickRailDest;

public final class LegacyPolyfillsSettings {
    @SerialEntry
    public boolean clickDestEnabled = ClickRailDest.DEFAULT_ENABLED;
    @SerialEntry
    public long clickDestCooldown = ClickRailDest.DEFAULT_COOLDOWN;
}
