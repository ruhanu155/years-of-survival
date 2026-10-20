package com.yearofice.sim.fishing;

/**
 * Every rod-fishing tuning number the slice adapter reads, as the {@code [fishing]} section of
 * {@code yearofice-common.toml} holds them ({@code com.yearofice.hunt.fishing.FishingConfig} fills it; tests use
 * {@link #standard()}). The fight's own rates stay in {@link FightModel}, {@link Tackle} and {@link FishSpecies},
 * which are the Blueprint's numbers. Sources: Blueprint › Fishing in depth unless a field says otherwise. Water
 * temperature comes from the Environment field and the fight's clock from the Close rate ({@code clock/rates.json});
 * the M2.5 keys {@code close_clock_rate} and {@code water_temp_c} are retired.
 *
 * @param spotQ               q of the slice's spot. The slice valley's one spot is a creek mouth on its creek:
 *                            (0.55 base + 0.10 deeper than 2 m + 0.05 within 300 m below a lake) × r 1.2, the top
 *                            of the ±20% spread, so 0.84 (Wildlife and fish › Fishing spot quality). Chosen so the
 *                            M2.5 test's "first bite within 30 game minutes in 90% of runs" holds on a May morning.
 * @param spotMinDepthM       the slice spot rule: every column at least this deep (1.5 m)
 * @param spotMinWidth        the slice spot rule: at least this many columns across in both directions (2)
 * @param spotRadiusM         a creek mouth's radius (5 m, Reading the water › Major spots get a radius too)
 * @param graylingCapacity    grayling a creek mouth holds at q = 1 (30, Fish stocks)
 * @param spawnCapacityX      the creek mouth's capacity during the grayling spawn (×5)
 * @param stockX              stock capacity × this: Realism presets › Animal numbers (×1.5, ×1.0, ×0.7)
 * @param baitDepthM          a bobber rig hangs the bait this far down (1 m; under 2 m keeps the size draw at u²)
 * @param wormBites           a worm lasts this many bites (2)
 * @param wormMinutes         or this many game minutes in the water (30)
 * @param castRangeM          the willow rod's cast range at full charge (15 m, Rods and retrieve)
 * @param castChargeS         real seconds of right mouse for a full cast (1.5, The fight, as built: Cast)
 * @param castScatterPct      the cast lands scattered ±this % in range (10)
 * @param castScatterDeg      and ±this many degrees (5)
 * @param strikeGraceS        added to the hook's window on the server for the round trip (slice rule, 0.25 s)
 * @param missBaitLossPct     a strike outside the window leaves the bait gone this % of the time (60)
 * @param spookMin            game minutes a strike at a nibble spooks the spot (10)
 * @param nibbleShare         chance of a false nibble in a minute with no bite, as a share of P_bite (slice rule)
 * @param leadMinS            the rod tip twitches this long, at least, before a real bite's dip (slice rule)
 * @param leadMaxS            and at most this long
 * @param nibbleS             a nibble shakes the bobber this long (slice rule)
 * @param lineWearPct         plant-fiber line wear per fight, ×3 if the arc reached red (3, Lines)
 * @param snapWearPct         line condition a snap costs in the slice, where the line is re-tied from the winder
 *                            instead of replaced (slice rule)
 * @param lineWetMin          a line wets after this many game minutes in use (10, Lines)
 * @param inputTimeoutTicks   the server steps an idle input for any tick the client is this late with (slice rule)
 * @param assist              1 turns on the fishing assist (on by default in Gentle, The fight, as built › Assist)
 * @param wormDigMax          worms one broken riparian soil block may give, 0 to this (stand-in for the dig job)
 * @param wormDigRange        soil within this many blocks of water counts as riparian
 * @param wormForestMax      worms one broken soil block under trees may give away from water, 0 to this (forest floor)
 * @param turbidity           turbidity until {@code env.water} lands (0 clear to 1 muddy)
 * @param spotMigrationPerDay the share of a fished-down spot's gap that swims in from neighbouring water each game day
 *                            (M6 lane C, RECONCILE 237; 0.08 refills half the gap in 8 days)
 */
public record FishingTuning(double spotQ, double spotMinDepthM, int spotMinWidth, double spotRadiusM,
                            double graylingCapacity, double spawnCapacityX, double stockX, double baitDepthM,
                            int wormBites, int wormMinutes, double castRangeM, double castChargeS,
                            double castScatterPct, double castScatterDeg, double strikeGraceS,
                            double missBaitLossPct, double spookMin, double nibbleShare, double leadMinS,
                            double leadMaxS, double nibbleS, double lineWearPct, double snapWearPct,
                            double lineWetMin, int inputTimeoutTicks, int assist,
                            int wormDigMax, int wormDigRange, double turbidity, int wormForestMax, double spotMigrationPerDay) {

    public static FishingTuning standard() { return FishingPreset.STANDARD.tuning; }

    public boolean assistOn() { return assist != 0; }
}
