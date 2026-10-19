package com.yearofice.sim.story;

/**
 * The numbers of the {@code [onboarding]} section of {@code yearofice-common.toml}. The switches {@code enabled}
 * and {@code beacon} live beside them in the section; these are the ones with a number.
 *
 * @param nearFireRadiusM  "sleep by the fire": the most a sleeping player may be from a warm fire's centre, metres.
 *                         Standard 3 m is the fire's full-heat radius (Minecraft engine › Fire › Heat: the full bonus
 *                         within 3 m) and the woodpile's "feeds itself within 3 m" (Blueprint › Start site, step 13)
 * @param duskLeadMin      the dusk warning opens this many game minutes before sunset ({@code ClockService}'s sun):
 *                         the Blueprint's "20:30 with no fire" against its 21:45 May sunset is 75
 * @param reachM           how near a ring or a lit fire must be to count as "yours" for the ring and light steps, metres
 * @param searchRadius     how far the server looks for a beacon target (a birch, the creek), blocks
 * @param beaconFadeNearM  the beacon is invisible within this distance of its target, metres
 * @param beaconFadeFullM  and at full (soft) strength from this distance, metres
 * @param beaconAlphaPct   the beacon's strongest opacity, percent
 */
public record OnboardingTuning(double nearFireRadiusM, int duskLeadMin, double reachM,
                               int searchRadius, double beaconFadeNearM, double beaconFadeFullM, int beaconAlphaPct) {

    public OnboardingTuning {
        if (!(nearFireRadiusM > 0) || !(reachM > 0)) throw new IllegalArgumentException("radii must be positive");
        if (duskLeadMin < 0 || duskLeadMin > 720) throw new IllegalArgumentException("lead is 0-720 game minutes");
        if (beaconAlphaPct < 0 || beaconAlphaPct > 100) throw new IllegalArgumentException("alpha is a percent");
    }

    public static OnboardingTuning standard() { return Preset.STANDARD.tuning; }

    /**
     * Gentle widens "by the fire" to 4 m and warns 15 minutes sooner. Brutal keeps Standard's numbers; it drops
     * the beacon instead (the {@code beacon} switch's Brutal default), keeping the prompts.
     */
    public enum Preset {
        GENTLE(new OnboardingTuning(4, 90, 4, 32, 4, 12, 30)),
        STANDARD(new OnboardingTuning(3, 75, 4, 32, 4, 12, 30)),
        BRUTAL(new OnboardingTuning(3, 75, 4, 32, 4, 12, 30));

        public final OnboardingTuning tuning;

        Preset(OnboardingTuning t) { this.tuning = t; }
    }
}
