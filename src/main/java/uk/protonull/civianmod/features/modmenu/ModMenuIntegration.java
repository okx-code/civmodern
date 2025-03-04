package uk.protonull.civianmod.features.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.CivianMod;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public @NotNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CivianMod::newConfigGui;
    }
}
