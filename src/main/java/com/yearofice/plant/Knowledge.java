package com.yearofice.plant;

import com.yearofice.hud.fieldnote.FieldNoteRecord;
import com.yearofice.hud.fieldnote.FieldNotes;
import com.yearofice.item.inventory.KitItems;
import com.yearofice.sim.plant.PlantNotes;
import com.yearofice.sim.plant.PlantPhase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Plant knowledge (Blueprint › Foraging and plant knowledge): a plant is Unknown, Partly known or Identified. Ways in:
 * the crate's pocket guide (12 plants, while carried), a field-note or logbook page that names the plant, seeing the
 * shoot, flower and fruit stages, eating its part, and a taste test (never a fungus). Identified plants show their
 * name; an unknown one shows "a plant".
 */
public final class Knowledge {
    private Knowledge() {}

    public static PlantNotes notes(ServerPlayer p) { return p.getData(PlantAttachments.PLANT_NOTES.get()); }

    private static void set(ServerPlayer p, PlantNotes n) { p.setData(PlantAttachments.PLANT_NOTES.get(), n); }

    public static boolean carriesGuide(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(KitItems.PLANT_GUIDE.get())) return true;
        }
        return false;
    }

    public static boolean identified(ServerPlayer p, PlantSpec spec) {
        if (notes(p).identified(spec.id)) return true;
        if (spec.pocketGuide && carriesGuide(p)) return true;
        if (!spec.notes.isEmpty()) {
            FieldNoteRecord r = FieldNotes.record(p);
            for (String n : spec.notes) if (r.filed.containsKey(n)) return true;
        }
        return false;
    }

    public static PlantNotes.Level level(ServerPlayer p, PlantSpec spec) {
        return identified(p, spec) ? PlantNotes.Level.IDENTIFIED : notes(p).level(spec.id);
    }

    /** What the crosshair shows for this species: its name, or "a plant". */
    public static String shownName(ServerPlayer p, PlantSpec spec) { return identified(p, spec) ? spec.name : "a plant"; }

    public static int stageBit(PlantPhase phase) {
        return switch (phase) {
            case FLOWER -> PlantNotes.FLOWER;
            case FRUIT, SEED -> PlantNotes.FRUIT;
            default -> PlantNotes.SHOOT;
        };
    }

    /** Looking closely at a plant in a growth stage; the third stage identifies it. Returns true when it just became identified. */
    public static boolean seen(ServerPlayer p, PlantSpec spec, PlantPhase phase) {
        boolean was = identified(p, spec);
        set(p, notes(p).seen(spec.id, stageBit(phase)));
        return !was && identified(p, spec);
    }

    public static void identify(ServerPlayer p, String species) { set(p, notes(p).identify(species)); }

    public static void forget(ServerPlayer p, String species) { set(p, notes(p).forget(species)); }

    /** A taste test result. */
    public record Taste(boolean allowed, boolean toxic, double conditionLoss) {}

    /** "Taste a small bite": refused for fungi; a safe plant is identified; a toxic one is too, at a Condition cost (applied by the body lane). */
    public static Taste taste(ServerPlayer p, PlantSpec spec) {
        if (!spec.tasteable()) return new Taste(false, false, 0);
        identify(p, spec.id);
        if (spec.toxic == null) return new Taste(true, false, 0);
        double loss = Math.min(spec.toxic.conditionLoss(), spec.toxic.grade().equals("deadly") ? 25 : 10) * ForageConfig.tuning().poisonScale();
        return new Taste(true, true, loss);
    }
}
