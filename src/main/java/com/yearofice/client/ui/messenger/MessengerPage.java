package com.yearofice.client.ui.messenger;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.env.Precip;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * One page of the Trail Messenger (Minecraft development › Messenger screens). The slice ships Vitals, Nutrition
 * and Weather; Map, Prompts, Notebook, Stores, Fires, Ambitions, Journal, Radio and SOS follow with their systems.
 */
public interface MessengerPage {

    String id();

    default Component title() { return Component.translatable("messenger.yearofice.page." + id()); }

    void render(Lcd lcd, BodySyncPayload p);

    static Component tr(String key, Object... args) {
        return Component.translatable("messenger.yearofice.vitals." + key, args);
    }

    /** Vitals: weight, the BMI bar toward 17, today's kcal, energy bolts, warmth and feels-like, wetness. */
    MessengerPage VITALS = new MessengerPage() {
        @Override public String id() { return "vitals"; }

        @Override
        public void render(Lcd l, BodySyncPayload p) {
            l.line(tr("weight", Fmt.kg(p.kg())), Pal.INK, "", Pal.INK);
            l.gap(-Lcd.ROW);
            l.right(tr("bmi", Fmt.one(p.bmi()), tr("band." + band(p.bmi()))), p.bmi() < 18.5f ? Pal.RED : Pal.INK);
            l.row();
            l.bmiBar(p.bmi());
            l.line(tr("today", Fmt.whole(p.kcalInToday()), Fmt.whole(p.kcalOutToday())), Pal.INK, "", Pal.INK);
            l.line(tr("balance"), Pal.DIM, Fmt.signed(p.balance24h()) + " kcal",
                    ClientBody.caloriesWarn(p) ? Pal.RED : Pal.INK);
            l.text(tr("energy", Fmt.one(p.energy())), ClientBody.energyWarn(p) ? Pal.RED : Pal.INK);
            l.row();
            l.bolts(0, p.energy(), p.energyCap());
            l.gap(Lcd.ROW);
            if (p.energyCap() < 10) {
                l.text(tr("capped", Math.round(p.energyCap())), Pal.DIM);
                l.row();
            }
            l.text(tr("warmth", Math.round(p.warmth())), ClientBody.warmthWarn(p) ? Pal.RED : Pal.INK);
            l.bar(64, p.warmth() / 100.0, p.warmth() < 30 ? Pal.RED : (p.warmth() < 70 ? Pal.ORANGE : Pal.GREEN));
            l.row();
            l.line(tr("feels"), Pal.DIM, Fmt.c(p.feelsLikeC()), p.feelsLikeC() < 10 ? Pal.BLUE : Pal.INK);
            l.text(tr("wet", Fmt.pct(p.wetness())), ClientBody.wetWarn(p) ? Pal.BLUE : Pal.INK);
            l.bar(64, p.wetness(), Pal.BLUE);
            l.row();
            l.line(tr("stomach", Fmt.pct(p.stomachPct())), Pal.INK, "", Pal.INK);
            l.gap(-Lcd.ROW);
            l.right(tr("condition", Math.round(p.condition())), ClientBody.conditionWarn(p) ? Pal.RED : Pal.INK);
            l.row();
        }
    };

    /** Nutrition: stomach, the 72 h macro meters, today's intake, water and the food lines. */
    MessengerPage NUTRITION = new MessengerPage() {
        @Override public String id() { return "nutrition"; }

        @Override
        public void render(Lcd l, BodySyncPayload p) {
            l.text(tr("stomach", Fmt.pct(p.stomachPct())), p.stomachPct() > 0.7f ? Pal.ORANGE : Pal.INK);
            l.bar(80, p.stomachPct(), Pal.BROWN);
            l.row();
            l.gap(2);
            l.text(tr("macros"), Pal.DIM);
            l.row();
            macro(l, "carbs", p.macroCarbs());
            macro(l, "fat", p.macroFat());
            macro(l, "protein", p.macroProtein());
            l.gap(2);
            l.line(tr("today_in", Fmt.whole(p.kcalInToday())), Pal.INK, "", Pal.INK);
            l.text(tr("water", Math.round(p.water())), ClientBody.waterWarn(p) ? Pal.BLUE : Pal.INK);
            l.bar(80, p.water() / 100.0, Pal.BLUE);
            l.row();
            l.line(tr("drunk"), Pal.DIM, Fmt.litres(p.drunkMlToday()), Pal.INK);
            l.gap(2);
            if (ClientBody.needFood(p)) {
                l.text(tr("need_food"), Pal.RED);
                l.row();
            }
            if (p.gutIll()) {
                l.text(tr("gut_ill"), Pal.RED);
                l.row();
            }
            if (p.macroFat() < 0.3f && p.macroProtein() > 0.45f) {
                l.text(tr("need_fat"), Pal.ORANGE);
                l.row();
            }
        }

        private void macro(Lcd l, String key, float share) {
            l.text(tr(key), Pal.INK);
            l.bar(52, share, share < 0.3f ? Pal.ORANGE : Pal.GREEN);
            l.textAt(52 + (l.w - 52) / 2 - 8, Fmt.pct(share), Pal.INK);
            l.row();
        }
    };

    /** Weather (basic): air, feels-like term by term, sky, wind and the daylight line. */
    MessengerPage WEATHER = new MessengerPage() {
        @Override public String id() { return "weather"; }

        @Override
        public void render(Lcd l, BodySyncPayload p) {
            l.line(tr("air", Fmt.c(p.airC())), Pal.INK, "", Pal.INK);
            l.gap(-Lcd.ROW);
            l.right(tr("feels_short", Fmt.c(p.feelsLikeC())), p.feelsLikeC() < 10 ? Pal.BLUE : Pal.INK);
            l.row();
            l.gap(2);
            l.text(tr("terms"), Pal.DIM);
            l.row();
            String[] keys = {"t_air", "t_wind", "t_wet", "t_clothing", "t_shelter", "t_fire", "t_activity", "t_body"};
            float[] f = p.feel();
            float[] sign = {1, -1, -1, 1, 1, 1, 1, 1};
            int half = l.w / 2;
            for (int i = 0; i < keys.length; i += 2) {
                l.text(tr(keys[i]), Pal.INK);
                String a = i == 0 ? Fmt.c(f[i]) : Fmt.dc(sign[i] * f[i]);
                l.textAt(half - 6 - net.minecraft.client.Minecraft.getInstance().font.width(a), a, Pal.INK);
                l.textAt(half + 4, tr(keys[i + 1]), Pal.INK);
                l.right(Fmt.dc(sign[i + 1] * f[i + 1]), Pal.INK);
                l.row();
            }
            l.gap(2);
            Precip pr = Precip.values()[Math.max(0, Math.min(Precip.values().length - 1, p.precip()))];
            Component sky = pr == Precip.NONE ? tr("sky_dry")
                    : tr("sky_precip", tr("precip." + pr.name().toLowerCase(Locale.ROOT)), Fmt.one(p.precipMmH()));
            l.text(sky, pr == Precip.NONE ? Pal.INK : Pal.BLUE);
            l.row();
            l.line(tr("wind", Fmt.one(p.windMs())), Pal.INK, "", Pal.INK);
            if (p.roofed()) {
                l.text(tr("roofed"), Pal.DIM);
                l.row();
            }
            int mod = (int) Math.floorMod(p.minute(), 1440);
            int sunset = 18 * 60, sunrise = 6 * 60;
            Component light = mod >= sunrise && mod < sunset
                    ? tr("daylight_left", (sunset - mod) / 60, (sunset - mod) % 60)
                    : tr("dark_until", Fmt.hhmm(sunrise));
            l.text(light, Pal.INK);
            l.row();
            Activity a = Activity.values()[Math.max(0, Math.min(Activity.values().length - 1, p.activity()))];
            l.text(tr("doing", tr("activity." + a.name().toLowerCase(Locale.ROOT))), Pal.DIM);
            l.row();
        }
    };

    MessengerPage[] ALL = {VITALS, NUTRITION, WEATHER};

    static String band(float bmi) {
        if (bmi >= 25) return "heavy";
        if (bmi >= 20) return "healthy";
        if (bmi >= 18.5f) return "thin";
        return "underweight";
    }
}
