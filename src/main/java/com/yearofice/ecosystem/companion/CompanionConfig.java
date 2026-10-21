package com.yearofice.ecosystem.companion;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.companion.CompanionTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [companion]} section of {@code yearofice-common.toml}: wolf trust and leaving, the sled team, the lead fence. */
public final class CompanionConfig {
    private CompanionConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("wolfLeaves", "companion.wolf_leaves: 0 never, 1 recoverable (the wolf walks back), 2 permanent.");
        NOTES.put("leaveBase", "February leave roll: p a game day = (leave_base - trust) / leave_divisor.");
        NOTES.put("leaveDivisor", "February leave roll divisor.");
        NOTES.put("returnMinDays", "A recoverable wolf returns this many days after leaving, at the soonest.");
        NOTES.put("returnMaxDays", "A recoverable wolf returns this many days after leaving, at the latest.");
        NOTES.put("capLow", "Most trust a wolf can gain in a game day below 50 trust.");
        NOTES.put("capMid", "Most trust a wolf can gain in a game day from 50 to 79 trust.");
        NOTES.put("capHigh", "Most trust a wolf can gain in a game day from 80 trust.");
        NOTES.put("neglectGraceDays", "Days with no trust gained before the wolf's trust drains.");
        NOTES.put("neglectDecay", "Trust lost a day once neglect passes the grace days.");
        NOTES.put("sledMaxTeam", "Most wolves in one sled team.");
        NOTES.put("sledPullKg", "Kilograms one harnessed Companion wolf pulls on a packed lane.");
        NOTES.put("sledTrotMps", "A sled team's top speed, metres a second, on a lane with a full team.");
        NOTES.put("sledLaneFactor", "Sled speed and pull on a worn sled lane.");
        NOTES.put("sledSnowFactor", "Sled speed and pull on untracked snow.");
        NOTES.put("sledIceFactor", "Sled speed and pull on river or lake ice.");
        NOTES.put("sledBareFactor", "Sled speed and pull on bare ground.");
        NOTES.put("leadFenceFollowShort", "Share of caribou bands that follow an 80 m (20 segment) lead fence wing (lead_fence_follow_80m).");
        NOTES.put("leadFenceFollowLong", "Share of caribou bands that follow a 200 m (50 segment) lead fence wing (lead_fence_follow_200m).");
        NOTES.put("caribouSnareHold", "Chance the caribou snare holds one animal on a band pass (caribou_snare_hold).");
        NOTES.put("fenceLearnKills", "Caribou taken at one fence in a season that halve its route use next year (fence_learn_kills).");
    }

    public static final PresetConfig<CompanionTuning> COMPANION = new PresetConfig<>(CompanionTuning.class, p -> switch (p) {
        case GENTLE -> CompanionTuning.Preset.GENTLE.tuning;
        case STANDARD -> CompanionTuning.Preset.STANDARD.tuning;
        case BRUTAL -> CompanionTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static void define(ModConfigSpec.Builder b) {
        b.comment("The wolf companion, the sled team and the caribou lead fence (M9).").push("companion");
        COMPANION.define(b);
        b.pop();
    }

    public static CompanionTuning tuning() { return COMPANION.tuning(); }

    public static void invalidate() { COMPANION.invalidate(); }
}
