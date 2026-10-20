package com.yearofice.body;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.InjurySpec;
import com.yearofice.body.injury.InjuryData;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.body.part.Problems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * {@code body.DamageRouter}: vanilla health is never used. Every damage to a player is cancelled and routed to the
 * body model (Minecraft development › Vanilla needs replaced › Damage routing; Body tab › Falls).
 *
 * <p>The slice's minimal routing, with the Body tab's sources inline until {@code injury_source/} data lands:
 * falls by height (over 3, 6, 12 m; collapse over 20 m), fire as {@code fire_step}, lightning, {@code /kill} and
 * the void as collapse, drowning ignored (the water clock decides; M4). Anything else is logged once by type id
 * and ignored.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class DamageRouter {
    private DamageRouter() {}

    private static Map<String, Double> w(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    private static InjurySource.ProblemRoll roll(String id, int stage, double chance) {
        return new InjurySource.ProblemRoll(id, stage, chance);
    }

    public static final InjurySource FALL_OVER_3M = new InjurySource("fall_over_3m", 4, 1,
            w("feet", 45, "legs", 25, "hands", 20, "arms", 10), Map.of(), List.of(roll(Problems.SPRAIN, 1, 1.0)), 0,
            false);
    public static final InjurySource FALL_OVER_6M = new InjurySource("fall_over_6m", 10, 2,
            w("feet", 35, "legs", 35, "hands", 10, "arms", 10, "torso", 10), Map.of(),
            List.of(roll(Problems.BROKEN_BONE, 1, 1.0)), 0, false);
    public static final InjurySource FALL_OVER_12M = new InjurySource("fall_over_12m", 60, 3,
            w("legs", 30, "feet", 25, "torso", 20, "arms", 15, "head", 10), Map.of(),
            List.of(roll(Problems.BROKEN_BONE, 1, 1.0)), 0, false);
    /** Standing in fire: −5 a second. Vanilla hits every half second, so −2.5 a hit. */
    public static final InjurySource FIRE_STEP = InjurySource.simple("fire_step", 2.5, w("feet", 60, "legs", 40),
            List.of(roll(Problems.BURN, 1, 0.5)));
    public static final InjurySource LIGHTNING = InjurySource.simple("lightning", 30,
            w("torso", 40, "head", 20, "arms", 20, "legs", 20), List.of(roll(Problems.BURN, 2, 1.0)));

    /** Effective fall height from vanilla's fall damage: damage = ceil(height − 3), so height ≈ damage + 3. */
    public static double fallHeight(float damage) { return damage + 3.0; }

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        e.setCanceled(true);
        route(p, e.getSource(), e.getAmount());
    }

    /** The data source {@code id} ({@code injury_source/<id>.json}), or the built-in fallback when data is not loaded. */
    private static InjurySource src(String id, InjurySource fallback) { return InjuryData.source(id, fallback); }

    /**
     * Routes one vanilla or mod damage event into the body. Public for tests. Falls go by height; everything else
     * goes by {@code body/damage_routes.json}: a source id, {@code #systemic} (collapse), or {@code #none}.
     */
    public static void route(ServerPlayer p, DamageSource src, float amount) {
        String key = key(src);
        String target = InjuryData.route(key);
        if (src.is(DamageTypes.FALL)) {
            double h = fallHeight(amount);
            if (h > 20) SosFlow.trigger(p, SosFlow.Cause.FELL);
            else if (h > 12) injure(p, src("fall_over_12m", FALL_OVER_12M), "fall");
            else if (h > 6) injure(p, src("fall_over_6m", FALL_OVER_6M), "fall");
            else if (h > 3) injure(p, src("fall_over_3m", FALL_OVER_3M), "fall");
        } else if (src.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            SosFlow.trigger(p, SosFlow.Cause.FELL);
        } else if (src.is(DamageTypes.GENERIC_KILL)) {
            SosFlow.trigger(p, SosFlow.Cause.COMMAND);
        } else if (src.is(DamageTypes.DROWN)) {
            // Out of air: the body collapses (the Endless SOS, cause drowning); the left pack goes to the bank.
            if (p.getAirSupply() <= -20) SosFlow.trigger(p, SosFlow.Cause.DROWNING);
        } else if ("#none".equals(target)) {
            // The water clock in the body model decides.
        } else if ("#systemic".equals(target)) {
            SosFlow.trigger(p, SosFlow.Cause.CONDITION);
        } else if (src.is(DamageTypes.LIGHTNING_BOLT)) {
            injure(p, src("lightning", LIGHTNING), "lightning_bolt");
        } else if (src.is(DamageTypes.IN_FIRE) || src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.HOT_FLOOR)) {
            if (src.is(DamageTypes.ON_FIRE)) p.clearFire();
            injure(p, src("fire_step", FIRE_STEP), key);
        } else if (target != null && !target.startsWith("#")) {
            InjurySource s = InjuryData.source(target);
            if (s != null) injure(p, s, key);
            else if (WARNED.add(key)) YearOfIce.LOG.warn("DamageRouter: {} routes to missing source {}", key, target);
        } else if (WARNED.add(key)) {
            YearOfIce.LOG.warn("DamageRouter: unrouted damage type {} ignored", key);
        }
    }

    private static void injure(ServerPlayer p, InjurySource s, String cause) {
        BodyService.get().applyInjury(p, InjurySpec.of(s), cause);
    }

    private static String key(DamageSource src) {
        return src.typeHolder().unwrapKey().map(ResourceKey::location).map(Object::toString).orElse("?");
    }

    /** Health never moves: nothing heals a player through vanilla. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (e.getEntity() instanceof ServerPlayer) e.setCanceled(true);
    }

    /** No death screen: any death SosFlow did not start becomes a collapse and the SOS. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        SosFlow.trigger(p, e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) ? SosFlow.Cause.FELL : SosFlow.Cause.CONDITION);
    }
}
