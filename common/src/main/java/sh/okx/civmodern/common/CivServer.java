package sh.okx.civmodern.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CivServer {
    public static final String IS_CIV_SERVER_KEY = "civ";

    public interface ServerData {
        boolean isCivServer();

        void isCivServer(
            boolean isCivServer
        );
    }

    public static @Nullable Boolean isCivServer() {
        if (Minecraft.getInstance().getCurrentServer() instanceof final ServerData serverData) {
            return serverData.isCivServer();
        }
        return null;
    }

    public static @NotNull Checkbox createCheckbox(
        final int x,
        final int y,
        final @NotNull Font font,
        final @NotNull ServerData serverData
    ) {
        return Checkbox.builder(Component.literal("Civ?"), font)
            .pos(x, y)
            .tooltip(Tooltip.create(Component.translatable("civmodern.gui.tooltip.civ.is")))
            .onValueChange((checkbox, value) -> serverData.isCivServer(value))
            .selected(serverData.isCivServer())
            .build();
    }
}
