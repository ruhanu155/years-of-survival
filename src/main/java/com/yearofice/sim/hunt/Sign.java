package com.yearofice.sim.hunt;

import com.google.gson.JsonObject;
import java.util.Locale;

/**
 * Animal sign and how it ages (Blueprint › Tracks and sign › How sign ages). Pure rules: the numbers are
 * {@code data/yearofice/sign/ages.json}. A sign stores the game minute it was made; what it looks like follows from its
 * age, the ground it is on and the weather since (rain erases mud and sand, new snow erases snow, moss springs back).
 */
public final class Sign {
    private Sign() {}

    public enum Kind { TRACK, SCAT, BROWSE, BED }

    public enum Substrate { MUD, SAND, SNOW, MOSS }

    public enum Age { FRESH, HOURS, DAY, OLD }

    public enum Precip { NONE, RAIN, SNOW }

    /** The age bands and lifetimes, in game minutes. */
    public record Rules(double freshMaxMin, double hoursMaxMin, double dayMaxMin, double mudLifeMin, double sandLifeMin,
                        double snowLifeMin, double mossLifeMin, double scatLifeMin, double browseLifeMin, double bedLifeMin) {
        public static Rules parse(JsonObject o) {
            return new Rules(o.get("fresh_below_min").getAsDouble(), o.get("soft_below_min").getAsDouble(), o.get("crusted_below_min").getAsDouble(),
                    o.get("mud_life_min").getAsDouble(), o.get("sand_life_min").getAsDouble(), o.get("snow_life_min").getAsDouble(),
                    o.get("moss_life_min").getAsDouble(), o.get("scat_life_min").getAsDouble(), o.get("browse_life_min").getAsDouble(),
                    o.get("bed_life_min").getAsDouble());
        }

        public static Rules standard() { return parse(HuntData.read("sign", "ages")); }

        /** Minutes this sign lasts with no weather. Scat, browse and beds follow their own lives; tracks the ground's. */
        public double lifeMin(Kind k, Substrate s) {
            return switch (k) {
                case SCAT -> scatLifeMin;
                case BROWSE -> browseLifeMin;
                case BED -> bedLifeMin;
                case TRACK -> switch (s) {
                    case MUD -> mudLifeMin;
                    case SAND -> sandLifeMin;
                    case SNOW -> snowLifeMin;
                    case MOSS -> mossLifeMin;
                };
            };
        }
    }

    public static Age age(Rules r, double ageMin) {
        return ageMin < r.freshMaxMin ? Age.FRESH : ageMin < r.hoursMaxMin ? Age.HOURS : ageMin < r.dayMaxMin ? Age.DAY : Age.OLD;
    }

    /** True once the sign is gone: past its life, or the precipitation since erased the ground it is printed in. */
    public static boolean erased(Rules r, Kind k, Substrate s, double ageMin, Precip precip) {
        if (ageMin >= r.lifeMin(k, s)) return true;
        if (k != Kind.TRACK && k != Kind.BED) return false;
        return (precip == Precip.SNOW && s == Substrate.SNOW) || (precip == Precip.RAIN && (s == Substrate.MUD || s == Substrate.SAND));
    }

    /** How well the reader sees it: 0.5 for a stranger to tracking, 1 once Practiced. */
    public static double confidence(double reps, int n) { return 0.5 + 0.5 * Math.min(1.0, n <= 0 ? 1.0 : reps / n); }

    public static String compass(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) deg += 360;
        String[] n = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        return n[(int) Math.round(deg / 45.0) % 8];
    }

    /** What a reader sees: species, how old, which way. Low confidence blurs the species and the hour. */
    public record Read(String species, Kind kind, double ageMin, Age age, double dx, double dz, int gait, double confidence, String text) {}

    public static Read read(Rules r, String species, Kind k, double madeMin, double nowMin, double dx, double dz, int gait, double confidence) {
        double ageMin = Math.max(0, nowMin - madeMin);
        Age a = age(r, ageMin);
        String name = confidence < 0.6 ? "An animal" : titled(species);
        String when;
        if (a == Age.FRESH) when = "just now";
        else if (a == Age.HOURS) when = confidence < 0.6 ? "some hours ago" : dayPart((long) madeMin, ageMin);
        else when = a == Age.DAY ? "a day or two ago" : "days ago";
        String what = switch (k) {
            case TRACK -> "tracks, " + when + (dx == 0 && dz == 0 ? "" : ", heading " + compass(dx, dz));
            case SCAT -> "droppings, " + when;
            case BROWSE -> "clipped browse, " + when;
            case BED -> "a pressed bed, " + when;
        };
        String text = k == Kind.TRACK ? name + ", " + when + (dx == 0 && dz == 0 ? "" : ", heading " + compass(dx, dz)) : name + ": " + what + ".";
        return new Read(species, k, ageMin, a, dx, dz, gait, confidence, text);
    }

    private static String dayPart(long madeMin, double ageMin) {
        int h = (int) (Math.floorMod(madeMin, 1440L) / 60);
        String part = h < 5 ? "in the night" : h < 12 ? "this morning" : h < 17 ? "this afternoon" : h < 21 ? "this evening" : "in the night";
        return ageMin > 720 ? "yesterday" : part;
    }

    private static String titled(String species) {
        String s = species.substring(species.indexOf(':') + 1).replace('_', ' ');
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
