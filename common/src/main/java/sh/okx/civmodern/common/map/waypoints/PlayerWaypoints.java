package sh.okx.civmodern.common.map.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerWaypoints {

    private final Map<UUID, PlayerWaypoint> waypoints = new HashMap<>();

    private static final Pattern SNITCH_PATTERN = Pattern.compile("^(Enter|Login|Logout) +(\\w+) +(\\w+) +\\[(-?\\d+) (-?\\d+) (-?\\d+)] +(\\[.+])?$");

    public Collection<PlayerWaypoint> getWaypoints() {
        return waypoints.values();
    }

    public void acceptSnitchHit(Component message) {
        StringBuilder acc = new StringBuilder();
        for (Component component : message.toFlatList()) {
            if (!(component.getContents() instanceof PlainTextContents text)) {
                continue;
            }

            char[] charArray = text.text().toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                char c = charArray[i];
                if (c == '§') {
                    i++;
                    continue;
                }
                acc.append(c);
            }
        }

        Matcher snitchMatcher = SNITCH_PATTERN.matcher(acc);
        if (!snitchMatcher.matches()) {
            return;
        }
        String playerName = snitchMatcher.group(2);
        String snitchName = snitchMatcher.group(3);
        boolean hasDistance = snitchMatcher.group(7) != null; // is in same world

        if (playerName == null || snitchName == null || !hasDistance) {
            return;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(snitchMatcher.group(4));
            y = Integer.parseInt(snitchMatcher.group(5));
            z = Integer.parseInt(snitchMatcher.group(6));
        } catch (NumberFormatException ex) {
            return;
        }

        AbstractClientPlayer player = null;
        for (AbstractClientPlayer iter : Minecraft.getInstance().level.players()) {
            if (!iter.getGameProfile().getName().equals(playerName)) {
                continue;
            }
            player = iter;
        }
        if (player == null) {
            return;
        }

        waypoints.put(player.getUUID(), new PlayerWaypoint(player.getGameProfile().getName(), player.getGameProfile().getId(), x, y, z, player.getSkin().texture(), Instant.now()));
    }
}
