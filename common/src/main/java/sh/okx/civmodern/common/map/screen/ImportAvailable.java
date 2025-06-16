package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.AbstractCivModernMod;

import java.util.function.Consumer;

public class ImportAvailable extends BaseOwoScreen<FlowLayout> {
    private String[] mods;
    private final Consumer<String> callback;

    public ImportAvailable(String[] mods, Consumer<String> callback) {
        this.mods = mods;
        this.callback = callback;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        root.child(Components.label(Component.literal("Import Available")).shadow(true).margins(Insets.bottom(5)));
        root.child(Components.label(Component.literal("CivMap can import map data from the following mods, please select one:")));

        var buttons = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        buttons.configure(layout -> {
            layout.margins(Insets.of(5));
        });
        buttons.gap(6);
        buttons.surface(Surface.PANEL);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        for (var mod : mods) {
            buttons.child(Components.button(Component.literal(mod), button -> {
                callback.accept(mod);
                Minecraft.getInstance().setScreen(null);
            }));
        }
        root.child(buttons);

        // TODO: add button to say I don't want to ever import, and remember that setting

        root.child(Components.button(Component.literal("Close"), button -> {
            callback.accept("close");
            Minecraft.getInstance().setScreen(null);
        }).margins(Insets.of(5)));
    }

    @Override
    public void removed() {
        super.removed();
        this.callback.accept("close");
    }
}
