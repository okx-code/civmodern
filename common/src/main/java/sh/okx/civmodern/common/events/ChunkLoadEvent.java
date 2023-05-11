package sh.okx.civmodern.common.events;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public record ChunkLoadEvent(ClientLevel level, LevelChunk chunk) implements Event {
}
