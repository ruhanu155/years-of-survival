package com.yearofice.body.illness;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.IllnessDef;
import com.yearofice.sim.body.IllnessDefs;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * {@code data/yearofice/illness/<id>.json}: the register, reloaded with datapacks. The jar's twenty files already
 * load from the classpath for the pure model; a reload replaces them with whatever the packs hold, so a pack can
 * change an onset or an effect without code.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class IllnessData {
    private IllnessData() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new Loader()); }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() { super(new com.google.gson.Gson(), "illness"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<String, IllnessDef> next = new HashMap<>(IllnessDefs.all());
            for (Map.Entry<ResourceLocation, JsonElement> f : files.entrySet()) {
                if (!f.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
                try {
                    JsonObject o = f.getValue().getAsJsonObject();
                    if (!o.has("id")) o.addProperty("id", f.getKey().toString());
                    IllnessDef d = IllnessDef.parse(o);
                    next.put(d.shortId(), d);
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("illness/{}: {}", f.getKey().getPath(), ex.toString());
                }
            }
            IllnessDefs.replace(next);
        }
    }
}
