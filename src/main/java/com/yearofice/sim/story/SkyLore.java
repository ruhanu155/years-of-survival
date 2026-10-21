package com.yearofice.sim.story;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Sky signs and the Sky lore skill (Land and seasons in play › Sky signs). Each evening a sign shows with the chance the
 * table gives for tomorrow's real state; the skill decides how many shown signs the player notices (strongest first).
 */
public final class SkyLore {
    private SkyLore() {}

    public record Sign(String id, String truth, double pTrue, double pFalse, int strength, String lore) {}

    public enum Level { CLUMSY, HANDY, PRACTICED }

    /** Tomorrow's facts a sign can be true of. */
    public record Tomorrow(double meanC, double todayMeanC, double precipMm, double windMs) {
        public boolean holds(String truth) {
            return switch (truth) {
                case "cold_holds" -> meanC <= todayMeanC;
                case "colder_tonight" -> meanC < todayMeanC - 2;
                case "clear_tomorrow" -> precipMm < 0.1 && windMs < 3;
                case "front" -> precipMm >= 1 || windMs > 6;
                default -> false;
            };
        }
    }

    public static Map<String, Sign> signs(JsonObject root) {
        Map<String, Sign> out = new TreeMap<>();
        for (var e : root.getAsJsonObject("signs").entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            out.put(e.getKey(), new Sign(e.getKey(), o.get("truth").getAsString(), o.get("p_true").getAsDouble(),
                    o.get("p_false").getAsDouble(), o.get("strength").getAsInt(), o.get("lore").getAsString()));
        }
        return out;
    }

    /** The signs that show this evening, strongest first. */
    public static List<Sign> shown(Map<String, Sign> table, Tomorrow t, Random rng) {
        List<Sign> out = new ArrayList<>();
        for (Sign s : table.values()) if (rng.nextDouble() < (t.holds(s.truth()) ? s.pTrue() : s.pFalse())) out.add(s);
        out.sort(Comparator.comparingInt(Sign::strength).reversed().thenComparing(Sign::id));
        return out;
    }

    public static int notices(Level l) { return switch (l) { case CLUMSY -> 1; case HANDY -> 3; case PRACTICED -> Integer.MAX_VALUE; }; }

    public static double right(Level l) { return switch (l) { case CLUMSY -> 0.55; case HANDY -> 0.70; case PRACTICED -> 0.80; }; }

    public static List<Sign> noticed(List<Sign> shown, Level l) { return shown.subList(0, Math.min(shown.size(), notices(l))); }
}
