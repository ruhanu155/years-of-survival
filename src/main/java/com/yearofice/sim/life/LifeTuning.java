package com.yearofice.sim.life;

/**
 * Every number of the M7 life of the year, as the {@code [life]} section of {@code yearofice-common.toml} holds them
 * (Land animals in depth › Lemmings; Bestiary › Bug pressure; Waters and fish › Winterkill; Birds and the air ›
 * The dawn chorus).
 *
 * @param lemmingBubbleM       lemmings and voles exist as bodies within this many metres of a player (32)
 * @param insectRangeM         the insect swarm entities exist within this many metres of a player (8)
 * @param insectCap            swarm entities alive near the players at once
 * @param biteIntervalS        game seconds between bites at a bug pressure of 1
 * @param biteScale            multiplier on the Condition one bite takes
 * @param chorusVoices         the dawn chorus mixes at most this many voices (12)
 * @param oxygenStartMgL       dissolved oxygen at freeze-over (12 mg/L)
 * @param oxygenDeathBelowMgL  fish start to die below this (3 mg/L)
 * @param oxygenDeathPerMg     share of a stock that dies a day per mg/L under the line (5%)
 * @param oxygenDemandMult     scales every water type's oxygen demand c
 * @param salmonDieoffDays     a run's fish die this many days after the run's peak (10)
 * @param salmonCarcassMax     spent salmon bodies shown at a spot at once
 * @param waveJitterDays       a species draws its arrival within this many days of its mean (5)
 * @param winterkillCarcassMax winterkill bodies shown at a spot at breakup
 */
public record LifeTuning(double lemmingBubbleM, double insectRangeM, int insectCap, double biteIntervalS, double biteScale,
                         int chorusVoices, double oxygenStartMgL, double oxygenDeathBelowMgL, double oxygenDeathPerMg,
                         double oxygenDemandMult, double salmonDieoffDays, int salmonCarcassMax, double waveJitterDays,
                         int winterkillCarcassMax) {

    public static LifeTuning standard() { return Preset.STANDARD.tuning; }

    public enum Preset {
        GENTLE(make(0.5, 0.7)),
        STANDARD(make(1.0, 1.0)),
        BRUTAL(make(1.5, 1.4));

        public final LifeTuning tuning;

        Preset(LifeTuning t) { this.tuning = t; }

        private static LifeTuning make(double bite, double demand) {
            return new LifeTuning(32, 8, 6, 20, bite, 12, 12, 3, 0.05, demand, 10, 8, 5, 8);
        }
    }
}
