package com.yearofice.body.injury;

import com.yearofice.body.BodyAttachments;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import com.yearofice.sim.body.part.Triage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Hold-to-treat on a part (Body tab › Treat flow). The client only asks (a part, or all, or cancel); the server picks
 * the treatment, runs the hold, checks and consumes the remedy item, then applies the effect through
 * {@link InjuryService#treat}. Treat all runs every treatment the pack allows in Triage order, each with its own
 * hold, and stops with "Out of bandages. Right shin still bleeding." when an item runs out.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class Treat {
    private Treat() {}

    /** Treatments tried for each problem, best first. */
    private static final Map<String, List<String>> ACTIONS = Map.ofEntries(
            Map.entry(Problems.CUT, List.of("bandage")),
            Map.entry(Problems.INFECTION, List.of("clean_redress")),
            Map.entry(Problems.BURN, List.of("cool_burn", "bandage")),
            Map.entry(Problems.SPRAIN, List.of("sprain_wrap")),
            Map.entry(Problems.BROKEN_BONE, List.of("rib_wrap", "splint", "sling")),
            Map.entry(Problems.BLISTERS, List.of("bandage")),
            Map.entry("quills", List.of("pull_quills")),
            Map.entry("leeches", List.of("remove_leech")),
            Map.entry("frostbite", List.of("rewarm_hands", "rewarm_feet", "rewarm_face")));

    public enum Outcome { TREATED, NOTHING, OUT_OF_ITEM }

    public record Result(Outcome outcome, @Nullable String action, @Nullable Problem problem) {}

    private record Plan(String action, Problem problem) {}

    // --- Items ------------------------------------------------------------------------------------------------

    private static boolean matches(ItemStack st, String spec) {
        if (st.isEmpty()) return false;
        if (spec.startsWith("#")) {
            return st.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(spec.substring(1))));
        }
        ResourceLocation id = ResourceLocation.parse(spec);
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.containsKey(id) && st.is(item);
    }

    private static @Nullable ItemStack find(ServerPlayer p, String spec) {
        Inventory inv = p.getInventory();
        for (ItemStack st : inv.items) if (matches(st, spec)) return st;
        for (ItemStack st : inv.offhand) if (matches(st, spec)) return st;
        return null;
    }

    /** The first required item the player lacks, as a plural word ("bandages"), or null when all are carried. */
    static @Nullable String missing(ServerPlayer p, String action) {
        var a = InjuryData.action(action);
        if (a == null || !a.has("items")) return null;
        for (var el : a.getAsJsonArray("items")) {
            String spec = el.getAsString();
            if (find(p, spec) == null) return itemWord(spec);
        }
        return null;
    }

    static String itemWord(String spec) {
        String n = spec.substring(spec.indexOf(':') + 1).replace('_', ' ');
        return n.endsWith("s") ? n : n + "s";
    }

    private static void consume(ServerPlayer p, String action) {
        var a = InjuryData.action(action);
        if (a == null || !a.has("items")) return;
        for (var el : a.getAsJsonArray("items")) {
            ItemStack st = find(p, el.getAsString());
            if (st != null) st.shrink(1);
        }
    }

    /** Whether the item for {@code action} is in the pack right now (tests and the client hint). */
    public static boolean hasItems(ServerPlayer p, String action) { return missing(p, action) == null; }

    // --- Choosing ---------------------------------------------------------------------------------------------

    /** The next treatment for {@code part} (any part when null) in Triage order, skipping problems in {@code done}. */
    private static @Nullable Plan next(BodyState b, @Nullable Part part, List<Problem> done) {
        List<Problem> sorted = Triage.sort(b.parts.problems());
        for (Problem q : sorted) {
            if (part != null && q.part() != part) continue;
            if (done.contains(q)) continue;
            for (String act : ACTIONS.getOrDefault(q.id(), List.of())) {
                int i = InjuryService.findProblem(List.of(q), act, q.part());
                if (i >= 0) return new Plan(act, q);
            }
        }
        return null;
    }

    /** Seconds the hold for the next treatment on {@code part} takes, or −1 when nothing fits. */
    public static double holdSeconds(ServerPlayer p, @Nullable Part part) {
        Plan pl = next(BodyAttachments.of(p), part, List.of());
        if (pl == null) return -1;
        var a = InjuryData.action(pl.action());
        return a != null && a.has("hold_s") ? a.get("hold_s").getAsDouble() : 1;
    }

    // --- Treating ---------------------------------------------------------------------------------------------

    /** One treatment on {@code part} now (the hold is the caller's business): consumes the item and applies it. */
    public static Result treatPart(ServerPlayer p, @Nullable Part part) {
        return treatOne(p, part, new ArrayList<>());
    }

    private static Result treatOne(ServerPlayer p, @Nullable Part part, List<Problem> done) {
        BodyState b = BodyAttachments.of(p);
        Plan pl = next(b, part, done);
        if (pl == null) {
            InjuryMessages.line(p, "nothing_to_treat", part == null ? "that" : InjuryMessages.partWord(part));
            return new Result(Outcome.NOTHING, null, null);
        }
        String lack = missing(p, pl.action());
        if (lack != null) {
            String still = pl.problem().bleedPerMinute() > 0 ? " still bleeding" : " still needs it";
            InjuryMessages.line(p, "out_of", lack, cap(InjuryMessages.where(pl.problem())) + still);
            return new Result(Outcome.OUT_OF_ITEM, pl.action(), pl.problem());
        }
        if (!InjuryService.treat(p, pl.action(), pl.problem().part())) return new Result(Outcome.NOTHING, pl.action(), pl.problem());
        consume(p, pl.action());
        done.add(pl.problem());
        return new Result(Outcome.TREATED, pl.action(), pl.problem());
    }

    /** Treat all: every treatment the pack allows, most urgent first; the out-of-item line fires once and it stops. */
    public static int treatAll(ServerPlayer p) {
        List<Problem> done = new ArrayList<>();
        int n = 0;
        for (int guard = 0; guard < 16; guard++) {
            Result r = treatOne(p, null, done);
            if (r.outcome() != Outcome.TREATED) break;
            n++;
        }
        return n;
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    // --- The hold ---------------------------------------------------------------------------------------------

    private record Hold(@Nullable Part part, boolean all, long endTick) {}

    private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();

    /** The client asked: part ordinal, or −1 for Treat all. Starts the hold of the first treatment. */
    public static boolean start(ServerPlayer p, int partIndex) {
        Part part = partIndex >= 0 && partIndex < Part.COUNT ? Part.values()[partIndex] : null;
        double s = holdSeconds(p, part);
        if (s < 0) {
            InjuryMessages.line(p, "nothing_to_treat", part == null ? "that" : InjuryMessages.partWord(part));
            return false;
        }
        HOLDS.put(p.getUUID(), new Hold(part, partIndex < 0, p.server.getTickCount() + Math.round(s * 20)));
        return true;
    }

    public static void cancel(ServerPlayer p) { HOLDS.remove(p.getUUID()); }

    public static boolean holding(ServerPlayer p) { return HOLDS.containsKey(p.getUUID()); }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (HOLDS.isEmpty()) return;
        long tick = e.getServer().getTickCount();
        for (Map.Entry<UUID, Hold> en : List.copyOf(HOLDS.entrySet())) {
            Hold h = en.getValue();
            if (tick < h.endTick()) continue;
            HOLDS.remove(en.getKey());
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) continue;
            Result r = treatPart(p, h.part());
            if (h.all() && r.outcome() == Outcome.TREATED) start(p, -1);
        }
    }
}
