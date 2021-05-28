package sh.okx.civmodern.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricCivModernBootstrap implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final FabricCivModernMod mod;

    public FabricCivModernBootstrap() {
        this.mod = new FabricCivModernMod();
    }

    @Override
    public void onInitialize() {
        ClientLifecycleEvents.CLIENT_STARTED.register(e -> mod.enable());
    }

    /*@SubscribeEvent
    public void loadWorld(WorldEvent.Load event) {
        LOGGER.info("World Load " + event.getPhase() + " " + event.getResult() + " " + event
            .getWorld().getClass().getTypeName());
        if (event.getWorld() instanceof ClientWorld) {
            BlockModelShapes shaper = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes();

            Field cacheField = shaper.getClass().getDeclaredFields()[0];
            Map<BlockState, IBakedModel> map;
            try {
                cacheField.setAccessible(true);
                map = new IdentityHashMap<>((Map<BlockState, IBakedModel>) cacheField.get(shaper));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }

            Iterator<Entry<BlockState, IBakedModel>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<BlockState, IBakedModel> entry = iterator.next();
                IBakedModel model = entry.getValue();
                List<BakedQuad> quads = model
                    .getQuads(null, Direction.UP, ThreadLocalRandom.current(),
                        EmptyModelData.INSTANCE);
                if (quads.isEmpty()) {
                    iterator.remove();
                }
            }
            System.out.println(map.size() + " toal size");
        }
    }*/
}
