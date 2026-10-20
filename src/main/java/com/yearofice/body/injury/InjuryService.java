package com.yearofice.body.injury;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.InjurySpec;
import com.yearofice.api.event.StaggerEvent;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.injury.BackStrain;
import com.yearofice.sim.body.injury.Grime;
import com.yearofice.sim.body.injury.PocketStone;
import com.yearofice.sim.body.injury.Stumble;
import com.yearofice.sim.body.injury.Treatments;
import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Gait;
import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Side;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The injury lane's server entry points: data-driven injuries, stumbles, back strain, treatments, pocket stones and
 * grime. Everything that changes a wound posts a Messenger line. The lane's own small state (the last back strain,
 * the grime meter, the heated stones) lives in the player's persistent data under {@code yoi_injury}, so the shared
 * body codec stays lane B's.
 */
public final class InjuryService {
    private InjuryService() {}

    private static long now(ServerPlayer p) { return BodyTicker.clock.minute(p.server); }

    private static CompoundTag state(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("yoi_injury")) root.put("yoi_injury", new CompoundTag());
        return root.getCompound("yoi_injury");
    }

    /** Injures the player from a data source. Returns false for an unknown source. */
    public static boolean injure(ServerPlayer p, String sourceId, @Nullable Part forced, double lossOverride,
                                 String cause) {
        InjurySource s = InjuryData.source(sourceId);
        if (s == null) return false;
        InjurySpec spec = InjurySpec.of(s);
        if (!Double.isNaN(lossOverride)) spec = spec.withLoss(lossOverride);
        if (forced != null) spec = spec.forcing(forced);
        BodyService.get().applyInjury(p, spec, cause);
        return true;
    }

    // --- Stumbles, trips and falls ----------------------------------------------------------------------------

    /** The stumble factor of the worse leg or foot band: x1 whole to x3 badly hurt (Body tab, Falls, stumbles and slips). */
    public static double injuredFactor(BodyRecord r, long now, boolean stick) {
        double w = Math.min(Gait.side(r, Side.LEFT, now, stick), Gait.side(r, Side.RIGHT, now, stick));
        if (w >= 0.95) return 1;
        if (w >= 0.75) return 1.5;
        if (w >= 0.5) return 2;
        return 3;
    }

    /** Applies one stumble roll's outcome; {@code uLoss} draws a fall's 5 to 20 Condition. */
    public static Stumble.Outcome stumble(ServerPlayer p, double uOutcome, double uLoss) {
        Stumble.Outcome o = Stumble.outcome(uOutcome);
        NeoForge.EVENT_BUS.post(new StaggerEvent(p, o.name().toLowerCase(java.util.Locale.ROOT)));
        String src = Stumble.source(o);
        switch (o) {
            case STUMBLE -> InjuryMessages.line(p, "stumble");
            case FOOT_INJURY -> injure(p, src, null, Double.NaN, "stumble");
            case SPRAIN -> injure(p, src, null, Double.NaN, "stumble");
            case FALL -> {
                ItemStack held = p.getMainHandItem();
                if (!held.isEmpty()) {
                    p.drop(held.copy(), true);
                    p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
                injure(p, src, null, Stumble.fallLoss(uLoss), "stumble");
            }
        }
        return o;
    }

    /** One 100 m of rough ground walked: rolls the chance, and the outcome when it goes off. */
    public static @Nullable Stumble.Outcome walk100m(ServerPlayer p, Stumble.Factors f) {
        double chance = Stumble.chance(InjuryConfig.get("stumble_base_pct"), f);
        if (p.getRandom().nextDouble() >= chance) return null;
        return stumble(p, p.getRandom().nextDouble(), p.getRandom().nextDouble());
    }

    // --- Back strain (B15) ------------------------------------------------------------------------------------

    /** Chance for this lift, with the preset's dials; 0 on Gentle. */
    public static double strainChance(BackStrain.Lift lift) {
        return BackStrain.chance(lift, InjuryConfig.get("back_strain_chance"), InjuryConfig.get("back_strain_squat_mult"), 1);
    }

    /**
     * A heavy lift by {@code p}: rolls {@code u} against the chance, respects the cooldown (unless
     * {@code ignoreCooldown}), and strains the back. The carry system calls this at the take-hold.
     */
    public static boolean heavyLift(ServerPlayer p, BackStrain.Lift lift, double u, boolean ignoreCooldown) {
        if (u >= strainChance(lift)) return false;
        long now = now(p);
        CompoundTag st = state(p);
        long last = st.contains("strain_last") ? st.getLong("strain_last") : -1;
        if (!ignoreCooldown && !BackStrain.cooldownOver(last, now, InjuryConfig.get("back_strain_cooldown_days"))) {
            return false;
        }
        st.putLong("strain_last", now);
        injure(p, "heavy_lift", Part.TORSO, Double.NaN, "heavy_lift");
        BodyState b = BodyAttachments.of(p);
        int i = Treatments.find(b.parts.problems(), "back_strain", Part.TORSO);
        if (i >= 0) {
            Problem q = b.parts.problems().get(i);
            long end = now + BackStrain.durationMinutes(p.getRandom().nextDouble());
            List<Problem> l = new java.util.ArrayList<>(b.parts.problems());
            l.set(i, new Problem(q.id(), q.part(), "back", q.stage(), q.timer(), q.dressed(), q.infected(), q.pad(),
                    q.binding(), q.cleanUntil(), q.splint(), q.sling(), q.wrap(), q.cooled(), q.started(), end));
            b.parts = b.parts.withProblems(l);
        }
        p.serverLevel().playSound(null, p.blockPosition(), com.yearofice.sound.YoiSounds.BACK_TWINGE.get(), SoundSource.PLAYERS, 1f, 1f);
        InjuryMessages.line(p, "back_strained");
        return true;
    }

    // --- Treatment --------------------------------------------------------------------------------------------

    /**
     * Runs treatment action {@code id} ({@code action/treat_<id>.json}) on the most urgent fitting problem of
     * {@code part} (any part when null). Items and the hold belong to the use handler, which is not built yet;
     * this is the effect. Returns false when nothing fits.
     */
    public static boolean treat(ServerPlayer p, String id, @Nullable Part part) {
        BodyState b = BodyAttachments.of(p);
        var action = InjuryData.action(id);
        if (action == null) return false;
        long now = now(p);
        List<Problem> ps = b.parts.problems();
        int i = -1;
        if (action.has("problems")) {
            for (var el : action.getAsJsonArray("problems")) {
                for (int k = 0; k < ps.size(); k++) {
                    Problem q = ps.get(k);
                    if (!q.id().equals(el.getAsString()) || (part != null && q.part() != part)) continue;
                    if (id.equals("cauterize") && (q.stage() < 2 || q.dressed())) continue;
                    if (id.equals("bandage") && q.dressed()) continue;
                    if (id.equals("splint") && q.isSplinted()) continue;
                    if (id.equals("rib_wrap") && q.part() != Part.TORSO) continue;
                    if (id.equals("sling") && (!q.isSplinted() || q.sling())) continue;
                    if (i < 0) i = k;
                }
                if (i >= 0) break;
            }
        }
        if (i < 0) {
            InjuryMessages.line(p, "nothing_to_treat", id.replace('_', ' '));
            return false;
        }
        Problem q = ps.get(i);
        String where = InjuryMessages.partWord(q.part());
        switch (id) {
            case "bandage", "clean_redress", "salve" -> b.parts = Treatments.bandage(b.parts, i, now);
            case "splint", "rib_wrap" -> b.parts = Treatments.splint(b.parts, i, now);
            case "sling" -> b.parts = Treatments.sling(b.parts, i);
            case "sprain_wrap" -> b.parts = Treatments.sprainWrap(b.parts, i, now);
            case "cool_burn" -> b.parts = Treatments.cool(b.parts, i, now, (int) InjuryConfig.get("bleed_window_min"));
            case "pull_quills", "remove_leech", "rewarm_hands", "rewarm_feet", "rewarm_face" ->
                    b.parts = Treatments.remove(b.parts, i);
            case "cauterize" -> {
                var c = Treatments.cauterize(b.parts, i, now, InjuryConfig.get("cauterize_loss"),
                        InjuryConfig.get("cauterize_blackout_below"), InjuryConfig.get("cauterize_blackout_chance"),
                        p.getRandom().nextDouble());
                b.parts = c.record();
                if (c.blackout()) InjuryMessages.line(p, "cauterize_blackout");
                else InjuryMessages.line(p, "cauterized", where);
                if (b.condition() <= 0) {
                    com.yearofice.body.SosFlow.trigger(p, com.yearofice.body.SosFlow.Cause.CONDITION);
                    return true;
                }
                BodyTicker.sync(p, now, true);
                return true;
            }
            default -> {
                return false;
            }
        }
        InjuryMessages.line(p, "treated", where, InjuryMessages.causeWord(id));
        BodyTicker.sync(p, now, true);
        return true;
    }

    // --- Pocket stones ----------------------------------------------------------------------------------------

    /** A stone heated beside the fire and wrapped goes into the inner pocket now. At most two count. */
    public static void heatStone(ServerPlayer p, long minute) {
        CompoundTag st = state(p);
        long[] old = st.getLongArray("stones");
        long[] next = new long[Math.min(2, old.length + 1)];
        next[0] = minute;
        for (int i = 1; i < next.length; i++) next[i] = old[i - 1];
        st.putLongArray("stones", next);
    }

    private static double[] stoneAges(ServerPlayer p, long minute) {
        long[] t = state(p).getLongArray("stones");
        double[] ages = new double[t.length];
        for (int i = 0; i < t.length; i++) ages[i] = minute - t[i];
        return ages;
    }

    /** Torso feels-like bonus from the pocket stones, °C. */
    public static double stoneTorsoC(ServerPlayer p, long minute) {
        return PocketStone.torsoBonus(stoneAges(p, minute), InjuryConfig.get("pocket_stone_fade_min"));
    }

    public static double stoneHandsC(ServerPlayer p, long minute, boolean handsEmpty) {
        return PocketStone.handsBonus(stoneAges(p, minute), InjuryConfig.get("pocket_stone_fade_min"), handsEmpty);
    }

    // --- Grime ------------------------------------------------------------------------------------------------

    public static double grime(ServerPlayer p) { return state(p).getDouble("grime"); }

    public static void addWork(ServerPlayer p, double workMinutes) {
        CompoundTag st = state(p);
        st.putDouble("grime", Grime.work(st.getDouble("grime"), workMinutes));
    }

    public static void wash(ServerPlayer p, boolean soap) {
        CompoundTag st = state(p);
        st.putDouble("grime", Grime.wash(st.getDouble("grime"), soap));
        InjuryMessages.line(p, "washed", Grime.word(st.getDouble("grime")));
    }

    static void log(String s) { YearOfIce.LOG.debug(s); }
}
