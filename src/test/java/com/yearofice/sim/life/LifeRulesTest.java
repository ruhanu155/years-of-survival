package com.yearofice.sim.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.sim.fishing.SalmonRun;
import com.yearofice.sim.time.Calendar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** M7 lane C pure rules: the lemming index, bug pressure, the chorus, winterkill and the migration calendar. */
class LifeRulesTest {
    static final LifeTuning T = LifeTuning.standard();

    @Test
    void lemmingIndexRunsTheFourYearTable() {
        assertEquals(0.2, LemmingCycle.index(0, 0), 1e-12);
        assertEquals(2.5, LemmingCycle.index(0, 2), 1e-12);
        assertEquals(0.3, LemmingCycle.index(3, 0), 1e-12, "phase 3 starts in the crash");
        assertEquals(LemmingCycle.index(1, 7), LemmingCycle.index(1, 3), "the cycle repeats every four years");
        assertTrue(LemmingCycle.bold(2.5));
        assertFalse(LemmingCycle.bold(0.7));
        assertEquals(1.3, LemmingCycle.pikeBiteMult(2.5, 5), 1e-12);
        assertEquals(1.0, LemmingCycle.pikeBiteMult(2.5, 6), 1e-12);
        assertEquals(LemmingCycle.phase(42), LemmingCycle.phase(42), "the seed fixes the phase");
    }

    @Test
    void collaredLemmingTurnsWhiteOctoberTwentyFifthToMayFifteenth() {
        assertEquals(2, LemmingCycle.collaredCoat(Calendar.dayOfYear(10, 25)));
        assertEquals(2, LemmingCycle.collaredCoat(Calendar.dayOfYear(1, 10)));
        assertEquals(2, LemmingCycle.collaredCoat(Calendar.dayOfYear(5, 15)));
        assertEquals(1, LemmingCycle.collaredCoat(Calendar.dayOfYear(10, 20)));
        assertEquals(1, LemmingCycle.collaredCoat(Calendar.dayOfYear(5, 20)));
        assertEquals(0, LemmingCycle.collaredCoat(Calendar.dayOfYear(7, 1)));
    }

    @Test
    void mosquitoPressureFollowsTheBlueprintTable() {
        assertEquals(0, Bugs.date(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(6, 9)), 1e-12);
        assertEquals(1, Bugs.date(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(6, 20)), 1e-12);
        assertEquals(1, Bugs.date(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(7, 15)), 1e-12);
        assertEquals(0, Bugs.date(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(7, 31)), 1e-12);
        assertTrue(Bugs.date(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(7, 23)) > 0);
        assertTrue(Bugs.date(Bugs.Kind.BLACK_FLY, Calendar.dayOfYear(6, 3)) > 0, "black flies start before the mosquitoes");
        assertEquals(0, Bugs.date(Bugs.Kind.NO_SEE_UM, Calendar.dayOfYear(6, 30)), 1e-12);
        assertEquals(0, Bugs.wind(4), 1e-12);
        assertEquals(0, Bugs.temp(8), 1e-12);
        assertEquals(1, Bugs.temp(12), 1e-12);
        double dusk = Bugs.pressure(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(6, 25), "bog", 20 * 60, 0, 15);
        double noon = Bugs.pressure(Bugs.Kind.MOSQUITO, Calendar.dayOfYear(6, 25), "bog", 12 * 60, 0, 15);
        assertEquals(1.5, dusk, 1e-12);
        assertEquals(0.6, noon, 1e-12);
        assertTrue(Bugs.exposed(dusk, false));
        assertFalse(Bugs.exposed(dusk, true), "smoke or a head net protects");
        assertFalse(Bugs.exposed(0.29, false));
        assertEquals(Double.POSITIVE_INFINITY, Bugs.biteIntervalS(0.1, 20));
    }

    @Test
    void chorusPeaksAtThreeThirtyInTheWhiteNightsAndAtSunriseInApril() {
        int jun = Calendar.dayOfYear(6, 10), apr = Calendar.dayOfYear(4, 20), dec = Calendar.dayOfYear(12, 10);
        assertEquals(1.0, Chorus.timing(jun, 210, 180), 1e-12);
        assertEquals(0.0, Chorus.timing(jun, 100, 180), 1e-12);
        assertEquals(0.0, Chorus.timing(jun, 360, 180), 1e-12);
        assertEquals(1.0, Chorus.timing(apr, 330, 330), 1e-12);
        assertEquals(0.0, Chorus.timing(apr, 330 - 45, 330), 1e-12, "it starts 45 minutes before sunrise");
        double june = Chorus.volume(jun, 210, 180, 1);
        double decv = Chorus.volume(dec, 8 * 60, 8 * 60, 1);
        double july = Chorus.volume(Calendar.dayOfYear(7, 10), 210, 180, 1);
        assertEquals(1.0, june, 1e-12);
        assertEquals(0.5, july, 1e-12, "July halves it");
        assertTrue(decv < 0.1, "winter is silent");
        assertEquals(0.3, Chorus.weather(true, 0, 10, 6, false), 1e-12);
        assertEquals(0.12, Chorus.weather(true, 9, 10, 6, false), 1e-12, "rain and wind multiply");
        assertEquals(0.2, Chorus.weather(false, 0, -6, 5, false), 1e-12);
        assertEquals(1.3, Chorus.weather(false, 0, 10, 6, true), 1e-12);
        assertEquals(12, Chorus.voices(1.3, T.chorusVoices()), "the mix is capped at 12 voices");
        assertEquals(0, Chorus.voices(0.01, 12));
    }

    @Test
    void aNormalWinterSparesABeaverPondAndAHardOneKillsMostOfIt() {
        Winter.Result normal = Winter.simulate("beaver_pond", 1.2, false, 1.0, 1, T);
        assertTrue(normal.finalDo() > 3.5 && normal.finalDo() < 6.5, "normal winter leaves about 4-6 mg/L, got " + normal.finalDo());
        assertEquals(0, normal.lostShare(), 1e-12, "no loss in a normal winter");
        Winter.Result hard = Winter.simulate("beaver_pond", 1.2, false, 1.4, 1, T);
        assertTrue(hard.minDo() < 1.0, "a hard winter drives it near zero, got " + hard.minDo());
        assertTrue(hard.lostShare() > 0.7, "and kills most of the fish, got " + hard.lostShare());
        assertEquals(0, Winter.simulate("beaver_pond", 1.2, false, 1.4, Winter.speciesFactor("alaska_blackfish"), T).lostShare(), 1e-12, "blackfish never die");
        double st = Winter.simulate("beaver_pond", 1.2, false, 1.4, Winter.speciesFactor("ninespine_stickleback"), T).lostShare();
        assertTrue(st < hard.lostShare() && st > 0, "stickleback die at a third of the rate");
        assertTrue(Winter.simulate("big_lake", 6, false, 1.4, 1, T).lostShare() == 0, "a big lake keeps its oxygen");
        assertTrue(Winter.simulate("beaver_pond", 1.2, true, 1.4, 1, T).finalDo() > hard.finalDo(), "an inflow helps");
        assertTrue(Winter.simulate("bog_pool", 1.0, false, 1.0, 1, T).lostShare() > normal.lostShare(), "bogs are worst");
    }

    @Test
    void winterDeathStartsBelowThreeMilligrams() {
        assertEquals(0, Winter.dailyDeath(3.0, 1, 3, 0.05), 1e-12);
        assertEquals(0.05, Winter.dailyDeath(2.0, 1, 3, 0.05), 1e-12);
        assertEquals(0.15, Winter.dailyDeath(0, 1, 3, 0.05), 1e-12);
    }

    @Test
    void salmonDieOffBeginsTenDaysAfterThePeak() {
        int peak = Calendar.dayOfYear(8, 5), freeze = Calendar.dayOfYear(10, 25);
        assertFalse(SalmonDieoff.dying(peak + 9, peak, freeze, 10));
        assertTrue(SalmonDieoff.dying(peak + 10, peak, freeze, 10));
        assertTrue(SalmonDieoff.dying(freeze, peak, freeze, 10));
        assertFalse(SalmonDieoff.dying(freeze + 1, peak, freeze, 10));
        assertEquals(8, SalmonDieoff.bodies(1500, 0.5, 8));
    }

    static List<MigrationDef> shipped() throws IOException {
        Path dir = Path.of("src/main/resources/data/yearofice/migration");
        List<MigrationDef> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.sorted().toList()) out.add(MigrationDef.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject()));
        }
        return out;
    }

    @Test
    void migrationDataParsesAndSalmonWindowsMatchTheRunTable() throws IOException {
        List<MigrationDef> defs = shipped();
        assertTrue(defs.size() >= 16);
        long waves = defs.stream().filter(d -> d.kind().equals("bird_wave")).count();
        assertEquals(7, waves, "seven bird waves");
        for (MigrationDef d : defs) {
            assertTrue(d.from() <= d.peak() && d.peak() <= d.to(), d.id() + " window order");
            if (d.kind().equals("salmon_run") || d.kind().equals("fish_run")) {
                SalmonRun.Run r = SalmonRun.RUNS.get(d.run());
                assertEquals(r.from(), d.from() + 1, d.id() + " from (FishSeason counts May 1 as day 1)");
                assertEquals(r.peak(), d.peak() + 1, d.id() + " peak");
                assertEquals(r.to(), d.to() + 1, d.id() + " to");
            }
        }
    }

    @Test
    void earlyBreakupPullsWavesTwoAndThreeForwardFiveDays() throws IOException {
        MigrationCalendar cal = new MigrationCalendar(shipped());
        MigrationDef w2 = cal.all().stream().filter(d -> d.id().endsWith("wave_2_open_water")).findFirst().orElseThrow();
        assertEquals(w2.from() - 5, w2.shifted(7).from());
        assertEquals(w2.from() + 5, w2.shifted(-7).from());
        MigrationDef w6 = cal.all().stream().filter(d -> d.id().endsWith("wave_6_big_pass")).findFirst().orElseThrow();
        assertEquals(w6.from(), w6.shifted(7).from(), "autumn waves do not move with spring warmth");
        assertTrue(cal.skyTraffic(Calendar.dayOfYear(9, 15), 0) > 0.9);
        assertEquals(0, cal.skyTraffic(Calendar.dayOfYear(1, 15), 0), 1e-12);
        assertTrue(cal.flocksToday(Calendar.dayOfYear(9, 15), 0) >= 4 && cal.flocksToday(Calendar.dayOfYear(9, 15), 0) <= 12);
        assertEquals(0, cal.flocksToday(Calendar.dayOfYear(1, 15), 0));
    }

    @Test
    void aSpeciesIsPresentOnlyInItsWindowAndDrawsItsOwnJitter() throws IOException {
        MigrationCalendar cal = new MigrationCalendar(shipped());
        int july = Calendar.dayOfYear(7, 15), feb = Calendar.dayOfYear(2, 1);
        assertTrue(cal.present("mallard", july, 7, 1, 0, 5));
        assertFalse(cal.present("mallard", feb, 7, 1, 0, 5));
        assertTrue(cal.present("raven", feb, 7, 1, 0, 5), "a species with no row is always present");
        for (int y = 1; y < 40; y++) {
            int j = MigrationCalendar.jitter(99, "mallard", y, 5);
            assertTrue(j >= -5 && j <= 5);
        }
        assertEquals(MigrationCalendar.jitter(99, "mallard", 3, 5), MigrationCalendar.jitter(99, "mallard", 3, 5));
        boolean varies = false;
        for (int y = 2; y < 40; y++) varies |= MigrationCalendar.jitter(99, "mallard", y, 5) != MigrationCalendar.jitter(99, "mallard", 1, 5);
        assertTrue(varies, "arrivals differ from year to year");
    }
}
