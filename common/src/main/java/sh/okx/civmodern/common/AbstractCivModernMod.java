package sh.okx.civmodern.common;

import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.events.ScrollEvent;
import sh.okx.civmodern.common.gui.screen.MainConfigScreen;
import sh.okx.civmodern.common.macro.AttackMacro;
import sh.okx.civmodern.common.macro.HoldKeyMacro;
import sh.okx.civmodern.common.macro.IceRoadMacro;
import sh.okx.civmodern.common.radar.Radar;

public abstract class AbstractCivModernMod {

    private static AbstractCivModernMod INSTANCE;
    private static final Logger LOGGER = LogManager.getLogger();

    private final KeyMapping configBinding;
    private final KeyMapping holdLeftBinding;
    private final KeyMapping holdRightBinding;
    private final KeyMapping iceRoadBinding;
    private final KeyMapping attackBinding;
    private CivMapConfig config;
    private ColourProvider colourProvider;
    private Radar radar;

    private HoldKeyMacro leftMacro;
    private HoldKeyMacro rightMacro;
    private IceRoadMacro iceRoadMacro;
    private AttackMacro attackMacro;

    private EventBus eventBus;

    public AbstractCivModernMod() {
        this.configBinding = new KeyMapping(
            "key.civmodern.config",
            Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.civmodern"
        );
        this.holdLeftBinding = new KeyMapping(
            "key.civmodern.left",
            Type.KEYSYM,
            GLFW.GLFW_KEY_MINUS,
            "category.civmodern"
        );
        this.holdRightBinding = new KeyMapping(
            "key.civmodern.right",
            Type.KEYSYM,
            GLFW.GLFW_KEY_EQUAL,
            "category.civmodern"
        );
        this.iceRoadBinding = new KeyMapping(
            "key.civmodern.ice",
            Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            "category.civmodern"
        );
        this.attackBinding = new KeyMapping(
            "key.civmodern.attack",
            Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            "category.civmodern"
        );

        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("AbstractCivModernMod initialised twice");
        }
    }

    public final void init() {
        this.eventBus = provideEventBus();

        registerKeyBinding(this.configBinding);
        registerKeyBinding(this.holdLeftBinding);
        registerKeyBinding(this.holdRightBinding);
        registerKeyBinding(attackBinding);
        registerKeyBinding(this.iceRoadBinding);
    }

    public final void enable() {
        loadConfig();
        loadRadar();
        replaceItemRenderer();

        this.eventBus.listen(ClientTickEvent.class, e -> this.tick());
        this.eventBus.listen(ScrollEvent.class, e -> this.onScroll());

        Options options = Minecraft.getInstance().options;
        this.leftMacro = new HoldKeyMacro(this, this.holdLeftBinding, options.keyAttack);
        this.rightMacro = new HoldKeyMacro(this, this.holdRightBinding, options.keyUse);
        this.iceRoadMacro = new IceRoadMacro(this, config, this.iceRoadBinding);
        this.attackMacro = new AttackMacro(this, this.attackBinding, options.keyAttack);
    }

    public abstract EventBus provideEventBus();
    public abstract void registerKeyBinding(KeyMapping mapping);

    public void onScroll() {
        if (this.leftMacro != null) this.leftMacro.onScroll();
        if (this.rightMacro != null) this.rightMacro.onScroll();
        if (this.attackMacro != null) this.attackMacro.onScroll();
    }

    private void tick() {
        while (configBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(new MainConfigScreen(this, config));
        }
    }

    private void replaceItemRenderer() {
        Minecraft minecraft = Minecraft.getInstance();
        for (Field field : Minecraft.class.getDeclaredFields()) {
            if (field.getType() == ItemRenderer.class) {
                field.setAccessible(true);
                try {
                    field.set(minecraft, new CustomItemRenderer(minecraft.getItemRenderer(), colourProvider));
                    replaceGuiItemRenderer();
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.warn("Unable to replace item renderer");
    }

    private void replaceGuiItemRenderer() {
        Minecraft minecraft = Minecraft.getInstance();
        Gui gui = minecraft.gui;
        for (Field field : Gui.class.getDeclaredFields()) {
            if (field.getType() == ItemRenderer.class) {
                field.setAccessible(true);
                try {
                    field.set(gui, minecraft.getItemRenderer());
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.warn("Unable to replace hotbar item renderer");
    }

    private void loadConfig() {
        Properties properties = new Properties();
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        File configFile = gameDir.resolve("config").resolve("civmodern.properties").toFile();
        try {
            if (!configFile.exists()) {
                InputStream resource = AbstractCivModernMod.class
                    .getResourceAsStream("/civmodern.properties");
                byte[] buffer = new byte[resource.available()];
                resource.read(buffer);
                FileOutputStream fos = new FileOutputStream(configFile);
                fos.write(buffer);
            }

            FileInputStream input = new FileInputStream(configFile);
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.config = new CivMapConfig(configFile, properties);

    }

    private void loadRadar() {
        this.colourProvider = new ColourProvider(config);
        this.radar = new Radar(config, eventBus, colourProvider);
        this.radar.init();
    }

    public ColourProvider getColourProvider() {
        return colourProvider;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public static AbstractCivModernMod getInstance() {
        return INSTANCE;
    }
}
