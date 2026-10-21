package com.yearofice.ecosystem.migration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.life.MigrationCalendar;
import com.yearofice.sim.life.MigrationDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Loads {@code data/yearofice/migration/<id>.json} into the year's {@link MigrationCalendar} (Birds and the air › The migration calendar). */
public final class MigrationData extends SimpleJsonResourceReloadListener {
    private static volatile MigrationCalendar calendar = new MigrationCalendar(List.of());

    private MigrationData() { super(new Gson(), "migration"); }

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new MigrationData()); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        List<MigrationDef> defs = new ArrayList<>();
        for (var e : in.entrySet()) {
            if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
            try {
                defs.add(MigrationDef.parse(e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                YearOfIce.LOG.warn("Bad migration {}: {}", e.getKey(), ex.toString());
            }
        }
        calendar = new MigrationCalendar(defs);
    }

    public static MigrationCalendar calendar() { return calendar; }
}
