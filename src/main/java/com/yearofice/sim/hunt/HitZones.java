package com.yearofice.sim.hunt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A species' hit zones (data {@code hitzone/<id>.json}, Blueprint › The hit-zone model). Zones are ellipsoids in the
 * animal's body frame (x across, y up, z forward, origin under the body) or, for {@code legs}, a half-space below a
 * height. The ray trace picks the decisive zone: the first one entered, except that a gut hit is replaced by a heart,
 * lung or liver zone the same arrow also crosses (the gut ellipsoid is the body's envelope).
 */
public final class HitZones {
    public record Zone(String name, double[] center, double[] radii, double shoulderFront, double belowY) {
        public boolean halfSpace() { return center == null; }
    }

    public record Trace(Zone zone, double t, double[] entry) {}

    private final String species;
    private final double killThresholdJ;
    private final Map<String, List<Zone>> poses;

    HitZones(String species, double killThresholdJ, Map<String, List<Zone>> poses) {
        this.species = species;
        this.killThresholdJ = killThresholdJ;
        this.poses = poses;
    }

    public String species() { return species; }

    public double killThresholdJ() { return killThresholdJ; }

    public List<Zone> zones(String pose) { return poses.getOrDefault(pose, poses.getOrDefault("standing", List.of())); }

    public static HitZones parse(JsonObject o) {
        Map<String, List<Zone>> poses = new LinkedHashMap<>();
        JsonObject p = o.getAsJsonObject("poses");
        List<Zone> standing = zones(p.getAsJsonArray("standing"), 0);
        poses.put("standing", standing);
        for (Map.Entry<String, JsonElement> e : p.entrySet()) {
            if (e.getKey().equals("standing")) continue;
            JsonObject d = e.getValue().getAsJsonObject();
            poses.put(e.getKey(), zones(null, d.get("lower_m").getAsDouble(), standing));
        }
        return new HitZones(o.get("species").getAsString(), o.get("kill_threshold_j").getAsDouble(), poses);
    }

    private static List<Zone> zones(JsonArray a, double lower) {
        List<Zone> out = new ArrayList<>();
        for (JsonElement el : a) {
            JsonObject z = el.getAsJsonObject();
            String name = z.get("zone").getAsString();
            double sf = z.has("shoulder_front") ? z.get("shoulder_front").getAsDouble() : 0;
            if (z.has("below_y_m")) out.add(new Zone(name, null, null, 0, z.get("below_y_m").getAsDouble()));
            else out.add(new Zone(name, vec(z.getAsJsonArray("center_m")), vec(z.getAsJsonArray("radii_m")), sf, Double.NaN));
        }
        return out;
    }

    private static List<Zone> zones(JsonArray unused, double lower, List<Zone> base) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : base) {
            if (z.halfSpace()) out.add(new Zone(z.name(), null, null, 0, z.belowY() - lower));
            else out.add(new Zone(z.name(), new double[] {z.center()[0], z.center()[1] - lower, z.center()[2]}, z.radii(), z.shoulderFront(), Double.NaN));
        }
        return out;
    }

    private static double[] vec(JsonArray a) { return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()}; }

    /** Entry parameter of the ray {@code o + t d} (d unit) into the zone within [0, maxT], or NaN. */
    static double enter(Zone z, double[] o, double[] d, double maxT) {
        if (z.halfSpace()) {
            if (o[1] < z.belowY()) return 0;
            if (d[1] >= 0) return Double.NaN;
            double t = (z.belowY() - o[1]) / d[1];
            return t <= maxT ? t : Double.NaN;
        }
        double a = 0, b = 0, c = -1;
        for (int i = 0; i < 3; i++) {
            double oc = (o[i] - z.center()[i]) / z.radii()[i];
            double dc = d[i] / z.radii()[i];
            a += dc * dc;
            b += 2 * oc * dc;
            c += oc * oc;
        }
        double disc = b * b - 4 * a * c;
        if (disc < 0 || a == 0) return Double.NaN;
        double sq = Math.sqrt(disc);
        double t0 = (-b - sq) / (2 * a), t1 = (-b + sq) / (2 * a);
        if (t1 < 0) return Double.NaN;
        double t = Math.max(0, t0);
        return t <= maxT ? t : Double.NaN;
    }

    /** Traces a ray in the body frame; null is a clean miss. */
    public Trace trace(String pose, double[] origin, double[] dir, double maxT) {
        double n = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        double[] d = {dir[0] / n, dir[1] / n, dir[2] / n};
        Trace first = null, vital = null;
        for (Zone z : zones(pose)) {
            double t = enter(z, origin, d, maxT);
            if (Double.isNaN(t)) continue;
            Trace tr = new Trace(z, t, new double[] {origin[0] + d[0] * t, origin[1] + d[1] * t, origin[2] + d[2] * t});
            if (first == null || t < first.t()) first = tr;
            if ((z.name().equals("heart_lungs") || z.name().equals("liver")) && (vital == null || t < vital.t())) vital = tr;
        }
        if (first != null && first.zone().name().equals("gut") && vital != null) return vital;
        return first;
    }

    /** True when the entry point lies in the front {@code shoulder_front} share of a heart-and-lung zone (the shoulder blade). */
    public static boolean shoulder(Trace t) {
        Zone z = t.zone();
        if (z.shoulderFront() <= 0 || z.center() == null) return false;
        double frontEdge = z.center()[2] + z.radii()[2];
        return (frontEdge - t.entry()[2]) / (2 * z.radii()[2]) < z.shoulderFront();
    }
}
