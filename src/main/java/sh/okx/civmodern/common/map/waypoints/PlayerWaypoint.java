package sh.okx.civmodern.common.map.waypoints;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.resources.Identifier;

import java.time.Instant;
import java.util.UUID;

public record PlayerWaypoint(String playerName, UUID playerId, int x, int y, int z, Identifier playerSkin, Instant timestamp) {

	public void render(GuiGraphicsExtractor graphics, int colour) {
		PlayerFaceExtractor.extractRenderState(graphics, playerSkin, -5, -5, 10, true, false, colour);
	}
}
