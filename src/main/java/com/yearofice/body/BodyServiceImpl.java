package com.yearofice.body;

import com.yearofice.api.body.BodyReadout;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.InjurySpec;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.Metabolism;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.body.part.InjuryContext;
import com.yearofice.sim.body.part.Injuries;
import com.yearofice.sim.food.FoodValues;
import net.minecraft.network.chat.Component;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The Body layer's service. Every write to {@code yearofice:body} outside the tick comes through here. */
public final class BodyServiceImpl implements BodyService {

    private static BodyState s(ServerPlayer p) { return BodyAttachments.of(p); }

    private static long now(ServerPlayer p) { return BodyTicker.clock.minute(p.server); }

    @Override
    public void effort(ServerPlayer p, String activity, double kcal, double gameMinutes) {
        if (p.isCreative() || p.isSpectator() || kcal <= 0) return;
        long minute = now(p);
        com.yearofice.sim.body.BodyModel.charge(s(p), minute, kcal);
        BodyTicker.sync(p, minute, false);
    }

    @Override public double warmth(ServerPlayer p) { return s(p).warmth; }

    @Override public double condition(ServerPlayer p) { return s(p).condition(); }

    @Override public double strength(ServerPlayer p) { return Metabolism.strength(s(p).condition(), s(p).bmi()); }

    @Override public double bmi(ServerPlayer p) { return s(p).bmi(); }

    @Override public double energy(ServerPlayer p) { return s(p).energy; }

    @Override public double water(ServerPlayer p) { return s(p).water; }

    @Override
    public BodyReadout readout(ServerPlayer p) {
        BodyState b = s(p);
        long m = now(p);
        double bmi = b.bmi();
        return new BodyReadout(b.kg, bmi, b.kcalInToday(m), b.kcalOutToday(m), b.balance24h(), b.energy,
                b.lastEnergyCap, b.warmth, b.lastFeel == null ? b.lastAirC : b.lastFeel.totalC(), b.wetness(), b.water,
                b.stomachPct(BodyConfig.tuning().stomachKg()), b.condition(), Metabolism.strength(b.condition(), bmi),
                b.macroMeter(Metabolism.CARBS), b.macroMeter(Metabolism.FAT), b.macroMeter(Metabolism.PROTEIN),
                b.drunkMlToday(m), b.gutIll(m), b.sos);
    }

    @Override
    public boolean beginIllness(ServerPlayer p, String illnessId) { return com.yearofice.body.illness.Illnesses.begin(p, illnessId); }

    @Override
    public boolean isSafeToStage(ServerPlayer p) {
        BodyState b = s(p);
        return b.condition() > 30 && b.warmth > 30 && !p.isInWater();
    }

    @Override
    public void applyInjury(ServerPlayer p, InjurySpec injury, String cause) {
        BodyState b = s(p);
        long m = now(p);
        InjuryContext ctx = new InjuryContext(p.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT
                ? com.yearofice.sim.body.part.Side.LEFT : com.yearofice.sim.body.part.Side.RIGHT, injury.posture(),
                injury.forcedPart(), injury.lossOverride(), false, false, m);
        Mulberry32 rng = Injuries.rngFor(p.server.overworld().getSeed(), p.getStringUUID(), b.parts.injurySeq() + 1);
        Injuries.Result r = Injuries.apply(injury.source(), b.parts, rng, ctx);
        b.parts = r.record();
        com.yearofice.body.injury.InjuryMessages.injured(p, injury.source().id(), cause, r);
        if (r.tearZone() != null && r.tear() > 0 && !r.hitPack()) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.yearofice.api.event.GarmentTearEvent(p,
                    r.tearZone().name().toLowerCase(java.util.Locale.ROOT), r.tear(), injury.source().id()));
        }
        com.yearofice.core.YearOfIce.LOG.debug("Injury {} ({}) on {}: parts {}, Condition {}", injury.source().id(),
                cause, p.getGameProfile().getName(), r.partsHit(), b.condition());
        if (b.condition() <= 0) {
            SosFlow.trigger(p, SosFlow.Cause.CONDITION);
            return;
        }
        BodyTicker.sync(p, m, true);
    }

    @Override
    public boolean eat(ServerPlayer p, ItemStack stack, double grams) {
        FoodMap.Row row = FoodMap.row(stack);
        if (row == null) return false;
        FoodValues v = row.values(stack.getItem().toString());
        BodyState b = s(p);
        BodyTuning t = BodyConfig.tuning();
        long m = now(p);
        double f = grams / 100.0;
        boolean ok = BodyModel.eat(b, t, m, grams, v.kcal(grams), v.carbsGPer100g() * f, v.fatGPer100g() * f,
                v.proteinGPer100g() * f, Metabolism.waterPoints(row.waterMlPer100g() * f / 1000.0));
        if (!ok) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "stomach_full", "body"));
            return false;
        }
        HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, "ate", "body", HudArg.of(Math.round(v.kcal(grams))),
                HudArg.of(Math.round(b.stomachPct(t.stomachKg()) * 100))));
        BodyTicker.sync(p, m, true);
        return true;
    }

    @Override
    public boolean eatNutrients(ServerPlayer p, double grams, double kcal, double proteinG, double fatG, double waterMl) {
        BodyState b = s(p);
        BodyTuning t = BodyConfig.tuning();
        long m = now(p);
        double carbs = Math.max(0, (kcal - 4 * proteinG - 9 * fatG) / 4.0);
        if (!BodyModel.eat(b, t, m, grams, kcal, carbs, fatG, proteinG, Metabolism.waterPoints(waterMl / 1000.0))) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "stomach_full", "body"));
            return false;
        }
        HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, "ate", "body", HudArg.of(Math.round(kcal)),
                HudArg.of(Math.round(b.stomachPct(t.stomachKg()) * 100))));
        BodyTicker.sync(p, m, true);
        return true;
    }

    @Override
    public void addVitaminC(ServerPlayer p, double cDays) { com.yearofice.body.illness.Illnesses.addVitaminC(p, cDays); }

    @Override
    public WaterPurity.Source waterSourceAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return com.yearofice.body.intake.Drinking.sourceAt(level, pos);
    }

    @Override
    public void drink(ServerPlayer p, double litres, WaterPurity.Source source, boolean boiled) {
        BodyState b = s(p);
        long m = now(p);
        Mulberry32 rng = new Mulberry32(StageHash.of(p.server.overworld().getSeed() + ":drink:" + p.getStringUUID()
                + ":" + m));
        WaterPurity.Drink d = WaterPurity.drink(source.purity, boiled, BodyConfig.tuning().unboiledIllness(), rng);
        BodyModel.drink(b, m, litres, d.ill() ? d.onsetMinutes() : -1);
        HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, "drank", "body", HudArg.of(Math.round(litres * 1000)),
                HudArg.of(Math.round(b.water))));
        BodyTicker.sync(p, m, true);
    }

    @Override
    public void wake(ServerPlayer p, String cause) {
        if (p.isSleeping()) p.stopSleepInBed(true, true);
        HudBus.get().post(p, HudLine.of(HudLineType.WARN, "woke", "body", HudArg.text(cause)));
    }
}
