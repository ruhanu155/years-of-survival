package com.yearofice.sim.bird;

/**
 * Every number of the M6 birds, as the {@code [birds]} section of {@code yearofice-common.toml} holds them
 * (Birds and the air › Flocks, Birds that steal from camp; Bestiary › Scavengers).
 *
 * @param flockCohesionM     a flock member farther than this from its flock's centre steers back (m)
 * @param flockSeparationM   and one nearer than this to a neighbour steers apart (m)
 * @param dabblerMin         ducks in a ground flock, least (Birds and the air › Flocks: 4 to 40)
 * @param dabblerMax         and most for the dabblers the lane places
 * @param gullMin            gulls in a flock, least
 * @param gullMax            and most
 * @param scavengeRangeM     a corvid or an eagle notices a carcass within this many metres
 * @param corvidArriveMin    corvids arrive 0 to this many game minutes after a death (Bestiary: 0 to 2 game hours)
 * @param ravenKgPerDay      what one raven eats from a carcass (1 kg a day)
 * @param eagleKgPerDay      what an eagle eats (0.5 kg a day)
 * @param jayGramsPerVisit   what a gray jay carries off a visit (50 g)
 * @param eagleFishPerDay    fish an eagle takes a game day from a spot at a full stock
 * @param eagleSpotRangeM    an eagle fishes spots within this many metres of itself
 * @param thiefRangeM        a jay or raven sees the camp's stores within this many metres
 * @param activeCap          single birds alive near the players (Performance caps: 14)
 * @param flockCap           ground flocks alive near the players (6)
 * @param spawnRadiusM       birds are placed within this many metres of a player
 */
public record BirdTuning(double flockCohesionM, double flockSeparationM, int dabblerMin, int dabblerMax, int gullMin, int gullMax,
                         double scavengeRangeM, double corvidArriveMin, double ravenKgPerDay, double eagleKgPerDay,
                         double jayGramsPerVisit, double eagleFishPerDay, double eagleSpotRangeM, double thiefRangeM,
                         int activeCap, int flockCap, double spawnRadiusM) {

    public static BirdTuning standard() { return Preset.STANDARD.tuning; }

    public enum Preset {
        GENTLE(make(0.7, 0.7)),
        STANDARD(make(1.0, 1.0)),
        BRUTAL(make(1.4, 1.3));

        public final BirdTuning tuning;

        Preset(BirdTuning t) { this.tuning = t; }

        private static BirdTuning make(double eat, double fish) {
            return new BirdTuning(10, 1.5, 4, 12, 3, 8, 48, 120, 1.0 * eat, 0.5 * eat, 50 * eat, 1.5 * fish, 40, 24, 14, 6, 80);
        }
    }
}
