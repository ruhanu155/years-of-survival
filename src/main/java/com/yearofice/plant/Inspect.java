package com.yearofice.plant;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.sim.plant.PatchModel;
import com.yearofice.sim.plant.PlantNotes;
import com.yearofice.sim.plant.PlantPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** The close-up card (Look, Smell, Cut) and the taste option, opened with the pocket guide. */
public final class Inspect {
    private Inspect() {}

    public static void say(ServerPlayer p, String text) {
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.LOOK, "plant_card", "plant", HudArg.text(text)));
    }

    /** The card text for the plant at {@code pos}; also counts the phase as a stage seen. */
    public static String card(ServerPlayer p, ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof PlantBlock pb)) return "Nothing here to look at.";
        PlantSpec spec = PlantData.get(pb.species());
        if (spec == null) return "A plant with no notes.";
        PlantPhase phase = st.getValue(PlantBlock.PHASE).sim();
        boolean now = Knowledge.seen(p, spec, phase);
        PlantNotes.Level lv = Knowledge.level(p, spec);
        StringBuilder sb = new StringBuilder(Knowledge.shownName(p, spec));
        sb.append(" (").append(phase == PlantPhase.BURIED ? "under snow" : phase.key()).append(", taken ").append(st.getValue(PlantBlock.TAKEN)).append("/3)");
        sb.append(". Look: ").append(spec.look.replace('_', ' '));
        sb.append(". Smell: ").append(spec.smell.replace('_', ' '));
        sb.append(". Cut: ").append(spec.cut.replace('_', ' ')).append('.');
        if (lv == PlantNotes.Level.PARTLY) sb.append(" Stages seen: ").append(Integer.bitCount(Knowledge.notes(p).stagesSeen(spec.id))).append(" of 3.");
        if (now) sb.append(" You know this one now: ").append(spec.name).append('.');
        if (lv == PlantNotes.Level.IDENTIFIED) {
            PatchData.Patch patch = PatchLedger.data(level.getChunkAt(pos)).patches.get(spec.id);
            if (patch != null) sb.append(" Patch: ").append(Math.round(PatchLedger.pickedShare(patch) * 100)).append("% picked. Health: ").append(PatchModel.word(patch.h)).append('.');
        }
        return sb.toString();
    }

    public static String taste(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof PlantBlock pb)) return "Nothing here to taste.";
        PlantSpec spec = PlantData.get(pb.species());
        if (spec == null) return "A plant with no notes.";
        if (Knowledge.identified(p, spec)) return "You know " + spec.name + " already.";
        Knowledge.Taste t = Knowledge.taste(p, spec);
        if (!t.allowed()) return "Never taste a fungus.";
        if (t.toxic()) PlantPoison.tasted(p, spec, t.conditionLoss());
        return t.toxic() ? "Bitter and wrong. It was " + spec.name + "; you will feel it in an hour."
                : "A small bite, and it is safe. It was " + spec.name + ".";
    }
}
