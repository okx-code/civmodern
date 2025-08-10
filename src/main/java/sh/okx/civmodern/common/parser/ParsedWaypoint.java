package sh.okx.civmodern.common.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ParsedWaypoint(String name, int x, int y, int z, int textPosStart, int textPosEnd) {
    public static List<ParsedWaypoint> parseWaypoints(String text) {
        List<ParsedWaypoint> waypoints = new ArrayList<>();

        char[] chars = text.toCharArray();

        WaypointParseState state = WaypointParseState.START;

        int startPos = 0;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();

        Map<String, String> kvPairs = new HashMap<>();

        for (int i = 0; i < chars.length; i++) {
            if (state == WaypointParseState.START) {
                if (chars[i] == '[') {
                    startPos = i;
                    state = WaypointParseState.WAYPOINT_KEY;
                }
            } else if (state == WaypointParseState.WAYPOINT_KEY) {
                if (chars[i] == ':') {
                    state = WaypointParseState.WAYPOINT_VALUE;
                } else if (chars[i] == ']') {
                    // error
                    state = WaypointParseState.START;
                    key = new StringBuilder();
                    value = new StringBuilder();
                    kvPairs.clear();
                } else {
                    key.append(chars[i]);
                }
            } else if (state == WaypointParseState.WAYPOINT_VALUE) {
                if (chars[i] == ',') {
                    state = WaypointParseState.WAYPOINT_KEY;
                    kvPairs.put(key.toString(), value.toString());
                    key = new StringBuilder();
                    value = new StringBuilder();
                } else if (chars[i] == ']') {
                    // waypoint
                    state = WaypointParseState.START;

                    kvPairs.put(key.toString(), value.toString());
                    key = new StringBuilder();
                    value = new StringBuilder();

                    String name = "";
                    Integer x = null;
                    Integer y = null;
                    Integer z = null;

                    try {
                        for (Map.Entry<String, String> kvPair : kvPairs.entrySet()) {
                            String parsedKey = kvPair.getKey();
                            String parsedValue = kvPair.getValue();
                            if (parsedKey.equalsIgnoreCase("name")) {
                                name = parsedValue;
                            } else if (parsedKey.equalsIgnoreCase("x")) {
                                x = Integer.parseInt(parsedValue);
                            } else if (parsedKey.equalsIgnoreCase("y")) {
                                y = Integer.parseInt(parsedValue);
                            } else if (parsedKey.equalsIgnoreCase("z")) {
                                z = Integer.parseInt(parsedValue);
                            }
                        }
                    } catch (NumberFormatException ex) {
                        continue;
                    } finally {
                        kvPairs.clear();
                    }

                    if (x != null && y != null && z != null) {
                        waypoints.add(new ParsedWaypoint(name, x, y, z, startPos, i + 1));
                    }
                } else {
                    value.append(chars[i]);
                }
            }
        }
        return waypoints;
    }
}
