package com.yearofice.decay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.decay.ColdCurve;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * {@code data/yearofice/decay/<curve>.json} (Minecraft development › Layer 5: "curves live in
 * data/yearofice/decay/&lt;curve&gt;.json"). The slice ships one, {@code decay/meat.json}: the living-rot curve that
 * raw and cooked meat and fish run on, with its frozen line. Its temperature factor is {@link ColdCurve}'s whole-degree
 * table (3× per 10 °C, 0.2 half-frozen), so the file sets the frozen line and names the shape and stage ladder it
 * uses; the safe lives by state stay in {@code data_maps/item/food.json} as {@code spoil_days_at_5c}.
 *
 * <p>The client never loads server data, so it reads {@link ColdCurve#STANDARD}; at the slice's 5 °C stand-in both
 * give f_T = 1.
 */
public final class DecayCurves {
    private DecayCurves() {}

    private static volatile ColdCurve meat = ColdCurve.STANDARD;

    /** The living-rot curve for meat and fish. */
    public static ColdCurve meat() { return meat; }

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new Loader()); }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() { super(new com.google.gson.Gson(), "decay"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            ColdCurve next = ColdCurve.STANDARD;
            JsonElement el = files.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "meat"));
            if (el != null) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    if (!"linear".equals(o.get("shape").getAsString()) || !"food".equals(o.get("ladder").getAsString())) {
                        throw new IllegalArgumentException("the slice reads only shape linear on the food ladder");
                    }
                    double frozen = o.get("frozen_below_c").getAsDouble();
                    next = frozen == ColdCurve.DEFAULT_FROZEN_BELOW_C ? ColdCurve.STANDARD : new ColdCurve(frozen);
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("decay/meat.json: {}; using the Standard curve", ex.toString());
                }
            } else {
                YearOfIce.LOG.warn("decay/meat.json is missing; using the Standard curve");
            }
            meat = next;
        }
    }
}
