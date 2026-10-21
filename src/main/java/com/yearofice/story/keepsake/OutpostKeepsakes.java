package com.yearofice.story.keepsake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;

/** Places an outpost's {@code keepsakes} list (outpost json, append-only key) as a barrel holding the note, after ageing. */
public final class OutpostKeepsakes {
    private OutpostKeepsakes() {}

    /** Places each listed keepsake at its offset from the south-west floor corner; returns how many went in. */
    public static int place(ServerLevel level, BlockPos origin, String outpostId) {
        String path = outpostId.contains(":") ? outpostId.substring(outpostId.indexOf(':') + 1) : outpostId;
        int n = 0;
        try (var in = OutpostKeepsakes.class.getResourceAsStream("/data/yearofice/outpost/" + path + ".json")) {
            if (in == null) return 0;
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!o.has("keepsakes")) return 0;
            for (var e : o.getAsJsonArray("keepsakes")) {
                JsonObject k = e.getAsJsonObject();
                var at = k.getAsJsonArray("at");
                BlockPos p = origin.offset(at.get(0).getAsInt(), at.get(1).getAsInt(), at.get(2).getAsInt());
                level.setBlock(p, Blocks.BARREL.defaultBlockState(), 2);
                if (level.getBlockEntity(p) instanceof Container c) {
                    c.setItem(0, KeepsakeRegistry.stack(k.get("id").getAsString()));
                    n++;
                }
            }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        return n;
    }
}
