package com.yearofice.client.hand;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure sampler for the GeckoLib-format hand animations ({@code animations/hands.animation.json}): linear keyframes
 * of position (px) and rotation (degrees) per bone. No Minecraft imports.
 */
public final class HandClips {
    private HandClips() {}

    public record Pose(float x, float y, float rotZ) {
        static final Pose REST = new Pose(0, 40, 0);
    }

    record Key(float t, float[] v) {}

    public record Clip(String name, float length, boolean hold, Map<String, List<Key>> pos, Map<String, List<Key>> rot) {
        public Pose sample(String bone, float time) {
            float t = hold ? Math.min(time, length) : time;
            float[] p = lerp(pos.get(bone), t, new float[] {Pose.REST.x(), Pose.REST.y(), 0});
            float[] r = lerp(rot.get(bone), t, new float[3]);
            return new Pose(p[0], p[1], r[2]);
        }
    }

    static float[] lerp(List<Key> ks, float t, float[] dflt) {
        if (ks == null || ks.isEmpty()) return dflt;
        if (t <= ks.get(0).t()) return ks.get(0).v();
        for (int i = 1; i < ks.size(); i++) {
            Key a = ks.get(i - 1), b = ks.get(i);
            if (t <= b.t()) {
                float k = (t - a.t()) / Math.max(1e-6f, b.t() - a.t());
                float[] o = new float[3];
                for (int j = 0; j < 3; j++) o[j] = a.v()[j] + (b.v()[j] - a.v()[j]) * k;
                return o;
            }
        }
        return ks.get(ks.size() - 1).v();
    }

    public static Map<String, Clip> parse(JsonObject root) {
        Map<String, Clip> out = new HashMap<>();
        for (var e : root.getAsJsonObject("animations").entrySet()) {
            JsonObject a = e.getValue().getAsJsonObject();
            Map<String, List<Key>> pos = new HashMap<>(), rot = new HashMap<>();
            for (var b : a.getAsJsonObject("bones").entrySet()) {
                JsonObject bo = b.getValue().getAsJsonObject();
                if (bo.has("position")) pos.put(b.getKey(), keys(bo.getAsJsonObject("position")));
                if (bo.has("rotation")) rot.put(b.getKey(), keys(bo.getAsJsonObject("rotation")));
            }
            boolean hold = a.has("loop") && "hold_on_last_frame".equals(a.get("loop").getAsString());
            out.put(e.getKey(), new Clip(e.getKey(), a.get("animation_length").getAsFloat(), hold, pos, rot));
        }
        return out;
    }

    private static List<Key> keys(JsonObject o) {
        List<Key> ks = new ArrayList<>();
        for (var k : o.entrySet()) {
            JsonElement arr = k.getValue();
            var a = arr.getAsJsonArray();
            ks.add(new Key(Float.parseFloat(k.getKey()), new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()}));
        }
        ks.sort((p, q) -> Float.compare(p.t(), q.t()));
        return ks;
    }
}
