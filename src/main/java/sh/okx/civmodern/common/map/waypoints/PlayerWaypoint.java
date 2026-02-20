package sh.okx.civmodern.common.map.waypoints;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.Identifier;

import java.time.Instant;
import java.util.UUID;

public record PlayerWaypoint(String playerName, UUID playerId, int x, int y, int z, Identifier playerSkin, Instant timestamp) {

	public void render(GuiGraphics graphics, int colour) {
        PlayerFaceRenderer.draw(graphics, playerSkin, -5, -5, 10, true, false, colour);
	}
}
