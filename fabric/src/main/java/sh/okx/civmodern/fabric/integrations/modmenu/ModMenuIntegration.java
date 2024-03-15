package sh.okx.civmodern.fabric.integrations.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import sh.okx.civmodern.common.AbstractCivModernMod;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (previousScreen) -> AbstractCivModernMod.getInstance().newConfigGui(previousScreen);
    }
}
