package uk.protonull.civianmod.config.old;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import uk.protonull.civianmod.features.macros.IceRoadMacro;

public final class LegacyIceRoadSettings {
    @SerialEntry
    public boolean snapPitch = IceRoadMacro.DEFAULT_SNAP_PITCH;
    @SerialEntry
    public boolean snapYaw = IceRoadMacro.DEFAULT_SNAP_YAW;
    @SerialEntry
    public boolean autoEat = IceRoadMacro.DEFAULT_AUTO_EAT;
    @SerialEntry
    public boolean stopWhenHungry = IceRoadMacro.DEFAULT_STOP_WHEN_STARVING;
}
