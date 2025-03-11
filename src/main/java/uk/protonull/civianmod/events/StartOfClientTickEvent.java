package uk.protonull.civianmod.events;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public record StartOfClientTickEvent(
    @NotNull Minecraft minecraft
) {
    public StartOfClientTickEvent {
        Objects.requireNonNull(minecraft);
    }
}
