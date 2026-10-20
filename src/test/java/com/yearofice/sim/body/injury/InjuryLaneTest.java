package com.yearofice.sim.body.injury;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Triage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class InjuryLaneTest {
    private static final Path DATA = Path.of("src/main/resources/data/yearofice");

    private static long count(String dir, String prefix) throws IOException {
        try (Stream<Path> s = Files.list(DATA.resolve(dir))) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefix)).count();
        }
    }

    @Test
    void dataCounts() throws IOException {
        assertTrue(count("injury_source", "") >= 29);
        assertTrue(count("injury", "") >= 13);
        assertEquals(25, count("damage_type", ""));
        assertEquals(17, count("action", "treat_"));
    }

    @Test
    void everySourceParsesAndWeightsAreSane() throws IOException {
        try (Stream<Path> s = Files.list(DATA.resolve("injury_source"))) {
            for (Path f : (Iterable<Path>) s::iterator) {
                String id = f.getFileName().toString().replace(".json", "");
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                InjurySource src = SourceJson.parse(id, o);
                assertEquals(id, src.id());
                double chance = src.problems().stream().mapToDouble(InjurySource.ProblemRoll::chance).sum();
                assertTrue(chance <= 1.0 + 1e-9, id + " problem chances sum over 1");
                if (!src.systemic()) assertFalse(src.parts().isEmpty(), id + " has parts");
            }
        }
    }

    @Test
    void bearBiteMatchesTheBlueprintTable() throws IOException {
        JsonObject o = JsonParser.parseString(Files.readString(DATA.resolve("injury_source/bear_bite.json"))).getAsJsonObject();
        InjurySource s = SourceJson.parse("bear_bite", o);
        assertEquals(8, s.conditionLoss());
        assertEquals(40, s.parts().get("arms"));
        assertTrue(s.posture().values().iterator().next().firstHitPack());
    }

    @Test
    void cauterizeStopsTheBleed() {
        BodyRecord r = BodyRecord.healthy().withProblems(List.of(Problem.fresh("cut", Part.LEFT_LEG, "shin", 2, 0)));
        assertEquals(3, Triage.totalBleed(r.problems()));
        BodyRecord b = Treatments.bleed(r, 10, 1, 10);
        assertEquals(70, b.condition(), 1e-9);
        Treatments.Cauterized c = Treatments.cauterize(b, 0, 10, 8, 15, 0.5, 0.9);
        assertTrue(c.applied());
        assertEquals(0, Triage.totalBleed(c.record().problems()));
        assertEquals("burn", c.record().problems().get(0).id());
        assertEquals(62, c.record().condition(), 1e-9);
        assertFalse(c.blackout());
    }

    @Test
    void blackoutBelowFifteen() {
        BodyRecord r = BodyRecord.healthy().withProblems(List.of(Problem.fresh("cut", Part.LEFT_LEG, "shin", 2, 0)));
        r = Treatments.bleed(r, 27, 1, 27); // 100 - 81 = 19
        assertTrue(Treatments.cauterize(r, 0, 27, 8, 15, 0.5, 0.1).blackout());
        assertFalse(Treatments.cauterize(r, 0, 27, 8, 15, 0.5, 0.9).blackout());
    }

    @Test
    void problemsEnd() {
        Problem bruise = Problem.fresh("bruise", Part.LEFT_FOOT, "toes", 1, 0);
        assertFalse(ProblemClock.over(bruise, 719));
        assertTrue(ProblemClock.over(bruise, 720));
        Problem bone = Problem.fresh("broken_bone", Part.LEFT_ARM, "forearm", 1, 0);
        assertFalse(ProblemClock.over(bone, 100000));
        assertTrue(ProblemClock.over(bone.splinted(0), 30 * 1440));
    }

    @Test
    void stoneAndGrime() {
        assertEquals(4, PocketStone.torsoBonus(new double[] {0, 0, 0}, 90), 1e-9);
        assertEquals(0, PocketStone.torsoBonus(new double[] {90}, 90), 1e-9);
        assertEquals(0, Grime.wash(80, true));
        assertEquals(40, Grime.wash(80, false));
    }
}
