package com.yearofice.build.craft;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.build.GateService;
import com.yearofice.api.build.StationCraftEvent;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.build.work.WorkService;
import com.yearofice.net.YoiNet;
import com.yearofice.sim.build.CraftMath;
import com.yearofice.sim.build.Practice;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.minigame.knapping.KnappingJudge;
import com.yearofice.sim.time.Rate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

/**
 * Station crafting, server side (Crafting and building in depth › Stations, Recipes, Practice). A player picks a
 * recipe from the station screen; {@link #start} checks it and begins a {@link Job}, which runs at the Work rate
 * (5 game minutes a real second) while the player stays near. When it finishes the inputs go, the tool wears, the
 * result is given, the skill earns its rep and a {@link StationCraftEvent} is posted. Inputs are taken when the job
 * finishes, not reserved, so a job that stops loses nothing. One job at a time (queues are M8).
 */
public final class StationCraft {
    private StationCraft() {}

    /** Why a recipe cannot be started now. */
    public enum Problem { NONE, GATE, TAUGHT, STATION, INPUTS, TOOL }

    /**
     * A recipe as this player sees it now.
     *
     * @param minutes game minutes after practice, tool, light and body
     * @param kcal    kilocalories, which practice never changes
     */
    public record Plan(RecipeHolder<StationRecipe> holder, Problem problem, double minutes, double kcal, Practice.Word word) {
        public StationRecipe recipe() { return holder.value(); }
    }

    // ---- reading the world ------------------------------------------------------------------------------------

    /** Every station recipe, by tier then id. */
    public static List<RecipeHolder<StationRecipe>> all(net.minecraft.world.level.Level level) {
        List<RecipeHolder<StationRecipe>> out = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(CraftRegistry.STATION.get()));
        out.sort(Comparator.<RecipeHolder<StationRecipe>>comparingInt(h -> h.value().tier().number).thenComparing(h -> h.id().toString()));
        return out;
    }

    public static @Nullable RecipeHolder<StationRecipe> byId(net.minecraft.world.level.Level level, ResourceLocation id) {
        for (RecipeHolder<StationRecipe> h : all(level)) if (h.id().equals(id)) return h;
        return null;
    }

    /** A workbench within its reach of {@code from}, or null. */
    public static @Nullable BlockPos benchNear(ServerLevel level, BlockPos from) {
        int r = (int) Math.ceil(CraftConfig.get("workbench_reach_m"));
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(from.offset(-r, -r, -r), from.offset(r, r, r))) {
            if (!level.getBlockState(q).is(CraftRegistry.WORKBENCH.get())) continue;
            double d = q.distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = q.immutable();
            }
        }
        return best;
    }

    /** Lanes that add stations (furnace, forge, canoe bed) append here; each adds ids to the set. Append-only. */
    public static final List<java.util.function.BiConsumer<ServerPlayer, Set<ResourceLocation>>> STATION_PROVIDERS = new ArrayList<>();

    /** The stations the player can use here: the Hands always, a workbench within reach, a warm fire close by. */
    public static Set<ResourceLocation> stations(ServerPlayer p, @Nullable BlockPos bench) {
        Set<ResourceLocation> out = new HashSet<>();
        out.add(StationRecipe.HANDS);
        ServerLevel l = p.serverLevel();
        BlockPos at = bench != null ? bench : benchNear(l, p.blockPosition());
        double reach = CraftConfig.get("workbench_reach_m") + 1.0;
        if (at != null && l.getBlockState(at).is(CraftRegistry.WORKBENCH.get()) && Vec3.atCenterOf(at).distanceTo(p.position()) <= reach + 0.5) {
            out.add(StationRecipe.WORKBENCH);
        }
        if (!FireHeatLookup.INSTANCE.sourcesNear(l, p.getX(), p.getY(), p.getZ()).isEmpty()) {
            out.add(ResourceLocation.fromNamespaceAndPath("yearofice", "fire_ring"));
        }
        for (var provider : STATION_PROVIDERS) provider.accept(p, out);
        return out;
    }

    // ---- items ------------------------------------------------------------------------------------------------

    /** The pack's stacks: the grid, the belt and the off-hand. */
    static List<ItemStack> held(Inventory inv) {
        List<ItemStack> out = new ArrayList<>(inv.items);
        out.addAll(inv.offhand);
        return out;
    }

    public static int count(Inventory inv, SizedIngredient need) {
        int n = 0;
        for (ItemStack s : held(inv)) if (!s.isEmpty() && need.ingredient().test(s)) n += s.getCount();
        return n;
    }

    public static boolean has(Inventory inv, SizedIngredient need) { return count(inv, need) >= need.count(); }

    /** The sharpest matching tool in the pack, or empty. */
    public static ItemStack findTool(Inventory inv, StationRecipe r) {
        if (r.tool().isEmpty()) return ItemStack.EMPTY;
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack s : held(inv)) {
            if (!r.tool().get().matches(s)) continue;
            if (best.isEmpty() || Wear.edge(s) > Wear.edge(best)) best = s;
        }
        return best;
    }

    static void consume(Inventory inv, SizedIngredient need) {
        int left = need.count();
        for (ItemStack s : held(inv)) {
            if (left <= 0) return;
            if (s.isEmpty() || !need.ingredient().test(s)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }

    // ---- planning ---------------------------------------------------------------------------------------------

    public static Plan plan(ServerPlayer p, RecipeHolder<StationRecipe> h, Set<ResourceLocation> stations) {
        StationRecipe r = h.value();
        Problem problem = Problem.NONE;
        if (!GateService.get().isOpen(p, r.tier())) problem = Problem.GATE;
        else if (r.taught()) problem = Problem.TAUGHT;
        else if (!stations.contains(r.station())) problem = Problem.STATION;
        else {
            for (SizedIngredient need : r.ingredients()) {
                if (!has(p.getInventory(), need)) {
                    problem = Problem.INPUTS;
                    break;
                }
            }
            if (problem == Problem.NONE && r.tool().isPresent() && findTool(p.getInventory(), r).isEmpty()) problem = Problem.TOOL;
        }
        ItemStack tool = findTool(p.getInventory(), r);
        double toolTime = tool.isEmpty() ? 1.0 : Wear.timeMult(tool);
        double toolKcal = tool.isEmpty() ? 1.0 : Wear.kcalMult(tool);
        double light = CraftMath.lightMult(dark(p));
        double body = bodyMult(p);
        double reps = r.skill().map(s -> PracticeService.get().reps(p, s)).orElse(0.0);
        int n = r.skill().map(s -> s.n).orElse(0);
        double base = r.workMin() * CraftConfig.get("work_time_scale");
        double minutes = CraftMath.minutes(base, toolTime, light, body, reps, n);
        double kcal = CraftMath.kcal(r.effort(), r.workMin(), toolKcal, light, body);
        Practice.Word word = r.skill().map(s -> Practice.word(reps, s.n)).orElse(Practice.Word.HANDY);
        return new Plan(h, problem, minutes, kcal, word);
    }

    static boolean dark(ServerPlayer p) {
        if (!EnvService.installed()) return false;
        return EnvService.get().light(p.serverLevel(), p.blockPosition()) <= (int) CraftConfig.get("dark_light_level");
    }

    static double bodyMult(ServerPlayer p) {
        double energy = 10;
        try {
            energy = BodyService.get().energy(p);
        } catch (RuntimeException ignored) {
            // no body service in a bare test world: rested
        }
        // Spirits and sleep debt arrive with the body's M4; a neutral 50 and no debt until then.
        return CraftMath.bodyMult(energy, 50, 1.0);
    }

    // ---- the job ----------------------------------------------------------------------------------------------

    /** One craft in progress. */
    public static final class Job {
        final UUID player;
        final ResourceLocation recipe;
        final @Nullable BlockPos bench;
        final Vec3 from;
        final long needDs;
        long doneDs;
        boolean finished, cancelled;
        int ticks;

        Job(UUID player, ResourceLocation recipe, @Nullable BlockPos bench, Vec3 from, long needDs) {
            this.player = player;
            this.recipe = recipe;
            this.bench = bench;
            this.from = from;
            this.needDs = Math.max(1, needDs);
        }

        public boolean finished() { return finished; }
        public boolean cancelled() { return cancelled; }
        public float share() { return Math.min(1f, (float) doneDs / needDs); }
        public ResourceLocation recipe() { return recipe; }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();
    /** Counts knapping attempts so two in one tick do not share a roll. */
    private static final java.util.concurrent.atomic.AtomicLong attempts = new java.util.concurrent.atomic.AtomicLong();

    public static synchronized @Nullable Job current(ServerPlayer p) { return JOBS.get(p.getUUID()); }

    private static final Map<UUID, String> REFUSED = new HashMap<>();

    /** Why the player's last craft was refused, in words (or null): the same text goes to the log and to {@code /yoi craft start}. */
    public static synchronized @Nullable String lastRefusal(ServerPlayer p) { return REFUSED.get(p.getUUID()); }

    /** The refusal in words: which gate, station, tool or input is the one that is missing. */
    public static String why(ServerPlayer p, Plan plan, Set<ResourceLocation> stations) {
        StationRecipe r = plan.recipe();
        String head = plan.holder().id() + " refused: " + plan.problem();
        return switch (plan.problem()) {
            case NONE -> plan.holder().id() + " ok";
            case GATE -> head + " (tier " + r.tier().id() + " is not open; gate mask " + GateService.get().mask(p) + ")";
            case TAUGHT -> head + " (taught by a page not yet read)";
            case STATION -> head + " (needs " + r.station() + "; here: " + stations + ")";
            case INPUTS -> {
                StringBuilder sb = new StringBuilder(head + " (short of");
                for (SizedIngredient need : r.ingredients()) if (!has(p.getInventory(), need)) sb.append(' ').append(need.ingredient().getItems().length == 0 ? "?" : BuiltInRegistries.ITEM.getKey(need.ingredient().getItems()[0].getItem())).append(" x").append(need.count()).append(" (have ").append(count(p.getInventory(), need)).append(')');
                yield sb.append(')').toString();
            }
            case TOOL -> head + " (needs " + r.tool().map(ToolReq::text).orElse("?") + ")";
        };
    }

    private static synchronized void refuse(ServerPlayer p, Plan plan, Set<ResourceLocation> stations) {
        String why = why(p, plan, stations);
        REFUSED.put(p.getUUID(), why);
        com.mojang.logging.LogUtils.getLogger().info("YOI craft {} -> {}", p.getGameProfile().getName(), why);
    }

    /** Starts a craft, or says why not and returns null. A job already running is replaced. */
    public static synchronized @Nullable Job start(ServerPlayer p, ResourceLocation recipeId, @Nullable BlockPos bench) {
        RecipeHolder<StationRecipe> h = byId(p.level(), recipeId);
        if (h == null) return null;
        Set<ResourceLocation> here = stations(p, bench);
        Plan plan = plan(p, h, here);
        if (plan.problem() == Problem.NONE) REFUSED.remove(p.getUUID());
        if (plan.problem() != Problem.NONE) {
            refuse(p, plan, here);
            say(p, "craft_" + plan.problem().name().toLowerCase(java.util.Locale.ROOT), HudArg.text(name(h.value())));
            return null;
        }
        cancel(p);
        long ds = Math.max(1, Math.round(plan.minutes() * 600));
        Job j = new Job(p.getUUID(), recipeId, bench, p.position(), ds);
        JOBS.put(p.getUUID(), j);
        return j;
    }

    public static synchronized void cancel(ServerPlayer p) {
        Job j = JOBS.remove(p.getUUID());
        if (j != null && !j.finished) {
            j.cancelled = true;
            YoiNet.send(p, WorkProgressPayload.idle());
        }
    }

    /** Whether the job may go on this tick: the player is near where it began and the bench is still there. */
    static boolean valid(ServerPlayer p, Job j) {
        if (!p.isAlive() || p.isSleeping()) return false;
        if (p.position().distanceTo(j.from) > CraftConfig.get("leave_m")) return false;
        RecipeHolder<StationRecipe> h = byId(p.level(), j.recipe);
        if (h == null) return false;
        return stations(p, j.bench).contains(h.value().station());
    }

    /** Advances {@code j} by {@code ds} game deciseconds; finishes it when the time is up. Returns true when it has finished. */
    public static boolean advance(ServerPlayer p, Job j, long ds) {
        if (j.finished) return true;
        j.doneDs += ds;
        j.ticks++;
        if (j.ticks % 8 == 0) p.swing(InteractionHand.MAIN_HAND, true);
        if (j.doneDs >= j.needDs) {
            j.finished = true;
            complete(p, j);
        }
        return j.finished;
    }

    /** One server tick of every craft. */
    public static synchronized void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        var it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Job j = e.getValue();
            if (p == null || p.isRemoved()) {
                it.remove();
                continue;
            }
            if (!valid(p, j)) {
                it.remove();
                j.cancelled = true;
                say(p, "craft_stopped");
                YoiNet.send(p, WorkProgressPayload.idle());
                continue;
            }
            ClockService.get().requestRate(p, Rate.WORK, "station-craft");
            boolean done = advance(p, j, WorkService.dsPerTick());
            if (done) {
                it.remove();
                YoiNet.send(p, WorkProgressPayload.idle());
            } else if (j.ticks % 5 == 1) {
                YoiNet.send(p, new WorkProgressPayload(true, j.share(), (float) ((double) WorkService.dsPerTick() / j.needDs),
                        (byte) WorkProgressPayload.Phase.CRAFT.ordinal(), (short) 0, (short) 1));
            }
        }
    }

    public static synchronized void reset() { JOBS.clear(); }

    // ---- finishing --------------------------------------------------------------------------------------------

    static void complete(ServerPlayer p, Job j) {
        RecipeHolder<StationRecipe> h = byId(p.level(), j.recipe);
        if (h == null) return;
        StationRecipe r = h.value();
        Set<ResourceLocation> st = stations(p, j.bench);
        Plan plan = plan(p, h, st);
        if (plan.problem() != Problem.NONE) {
            refuse(p, plan, st);
            say(p, "craft_" + plan.problem().name().toLowerCase(java.util.Locale.ROOT), HudArg.text(name(r)));
            return;
        }
        ItemStack tool = findTool(p.getInventory(), r);
        for (SizedIngredient need : r.ingredients()) consume(p.getInventory(), need);
        if (!tool.isEmpty()) Wear.use(p, tool, 1 + (int) (plan.minutes() / CraftConfig.get("minutes_per_tool_use")));
        PracticeService practice = PracticeService.get();
        boolean shattered = r.skill().filter(s -> s == Skill.KNAPPING).map(s -> KnappingJudge.autoShatters(
                p.serverLevel().getSeed(), p.getUUID().getLeastSignificantBits(), p.level().getGameTime() * 1009 + attempts.incrementAndGet(),
                (int) practice.reps(p, s))).orElse(false);
        ItemStack made = shattered ? ItemStack.EMPTY : r.result().copy();
        if (shattered) {
            r.skill().ifPresent(s -> practice.add(p, s, Practice.FAIL_REP));
            say(p, "craft_shattered", HudArg.text(name(r)));
        } else {
            give(p, made.copy());
            r.skill().ifPresent(s -> practice.add(p, s, Practice.JOB_REP));
            say(p, "craft_done", HudArg.text(name(r)));
        }
        NeoForge.EVENT_BUS.post(new StationCraftEvent(p, h.id(), r.station(), made));
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** The recipe's name: its result's name. */
    public static String name(StationRecipe r) { return r.result().getHoverName().getString(); }

    static void say(ServerPlayer p, String id, HudArg... args) {
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.PROMPT, id, "building", args));
    }

    /** The result item's id, for lang keys. */
    public static String resultId(StationRecipe r) { return BuiltInRegistries.ITEM.getKey(r.result().getItem()).toString(); }
}
