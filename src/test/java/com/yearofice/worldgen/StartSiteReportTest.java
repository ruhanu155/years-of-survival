package com.yearofice.worldgen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Opt-in (-Pstartsite.report): runs seeds 1-50 and writes build/start-sites.tsv, one row per seed, for docs/qa/start-sites-50.md. */
class StartSiteReportTest {
    @Test
    void writeFiftySeedTable() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("startsite.report"), "runs with -Pstartsite.report");
        StartSiteParams sp = StartSiteParams.load();
        List<String> rows = new ArrayList<>();
        for (long seed = 1; seed <= 50; seed++) {
            String row;
            try {
                StartSite.Outcome o = StartSite.search(Fixture.PIPE, seed, sp);
                List<String> m = o.site().measures();
                row = seed + "\tPASS\t" + o.rerolls() + "\t" + m.get(0) + "\t" + m.get(1) + "\t" + m.get(10) + "\t" + m.get(11) + "\t"
                        + String.format(java.util.Locale.ROOT, "%.3f", o.site().score());
            } catch (IllegalStateException e) {
                row = seed + "\tFAIL\t20\t" + e.getMessage().replace('\n', '|');
            }
            rows.add(row);
            Files.createDirectories(Path.of("build"));
            Files.write(Path.of("build/start-sites.tsv"), rows);
        }
    }
}
