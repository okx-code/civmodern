package sh.okx.civmodern.mod.hooks.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.CivModernMod;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public @NotNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (previousScreen) -> CivModernMod.getInstance().newConfigGui(previousScreen);
    }
}
