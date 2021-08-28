package sh.okx.civmodern.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricCivModernBootstrap implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static FabricCivModernMod mod;

    public FabricCivModernBootstrap() {
        FabricCivModernBootstrap.mod = new FabricCivModernMod();
    }

    @Override
    public void onInitializeClient() {
        FabricCivModernBootstrap.mod.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(e -> mod.enable());
    }

    public static FabricCivModernMod getMod() {
        return FabricCivModernBootstrap.mod;
    }
}
