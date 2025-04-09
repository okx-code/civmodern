package uk.protonull.civianmod.config.old;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import uk.protonull.civianmod.config.CivianModConfig;

public final class LegacyIceRoadSettings {
    @SerialEntry
    public boolean snapPitch = CivianModConfig.DEFAULT_SNAP_PITCH;
    @SerialEntry
    public boolean snapYaw = CivianModConfig.DEFAULT_SNAP_YAW;
    @SerialEntry
    public boolean autoEat = CivianModConfig.DEFAULT_AUTO_EAT;
    @SerialEntry
    public boolean stopWhenHungry = CivianModConfig.DEFAULT_STOP_WHEN_HUNGRY;
}
