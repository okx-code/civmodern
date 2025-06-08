package sh.okx.civmodern.common.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record CommandRegistration(CommandDispatcher<ClientSuggestionProvider> dispatcher, CommandBuildContext registryAccess) {
}
