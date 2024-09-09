package sh.okx.civmodern.mod.features.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.CivModernMod;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public @NotNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CivModernMod::newConfigGui;
    }
}
