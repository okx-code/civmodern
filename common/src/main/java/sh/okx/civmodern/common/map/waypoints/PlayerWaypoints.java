package sh.okx.civmodern.common.map.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerWaypoints {

    private final Map<UUID, PlayerWaypoint> waypoints = new HashMap<>();
    private final Map<String, PlayerWaypoint> unfilledWaypoints = new HashMap<>();

    private static final Pattern SNITCH_PATTERN = Pattern.compile("^(Enter|Login|Logout) +(\\w+) +(.+) +\\[(-?\\d+) (-?\\d+) (-?\\d+)] +(\\[.+])?$");

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

        if (playerName.equals(Minecraft.getInstance().player.getGameProfile().getName())) {
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

        PlayerInfo player = null;
        for (PlayerInfo info : Minecraft.getInstance().player.connection.getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (!name.equals(playerName)) {
                continue;
            }
            player = info;
        }
        if (player == null) {
            unfilledWaypoints.put(playerName, new PlayerWaypoint(playerName, null, x, y, z, null, Instant.now()));
        } else {
            waypoints.put(player.getProfile().getId(), new PlayerWaypoint(player.getProfile().getName(), player.getProfile().getId(), x, y, z, player.getSkin().texture(), Instant.now()));
        }
    }

    public void tick() {
        Iterator<Map.Entry<String, PlayerWaypoint>> it = unfilledWaypoints.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PlayerWaypoint> waypoint = it.next();
            if (waypoint.getValue().timestamp().until(Instant.now(), ChronoUnit.SECONDS) > 30) {
                it.remove();
                continue;
            }

            for (PlayerInfo info : Minecraft.getInstance().player.connection.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name.equals(waypoint.getKey())) {
                    PlayerWaypoint value = waypoint.getValue();
                    waypoints.put(info.getProfile().getId(), new PlayerWaypoint(info.getProfile().getName(), info.getProfile().getId(), value.x(), value.y(), value.z(), info.getSkin().texture(), value.timestamp()));
                    it.remove();
                }
            }
        }
    }
}
