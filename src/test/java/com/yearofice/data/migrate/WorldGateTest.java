package com.yearofice.data.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The version gate and the pre-upgrade backup (Saves across versions › The promise; Migration runs lazily). */
class WorldGateTest {
    static final SaveVersion V020 = SaveVersion.parse("0.2.0"), V021 = SaveVersion.parse("0.2.1"), V070 = SaveVersion.parse("0.7.0");

    static void file(Path root, String rel, String text) throws IOException {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text, StandardCharsets.UTF_8);
    }

    static void populate(Path world) throws IOException {
        file(world, "level.dat", "LEVEL");
        file(world, "playerdata/abc.dat", "PLAYER");
        file(world, "yearofice/yearofice_clock.dat", "CLOCK");
        file(world, "region/r.0.0.mca", "BIG REGION");
    }

    @Test
    void versionsParseAndCompare() {
        assertEquals(new SaveVersion(0, 5, 0), SaveVersion.parse("v0.5.0-beta.1"));
        assertEquals(new SaveVersion(1, 0, 0), SaveVersion.parse("1.0+build7"));
        assertTrue(V021.compareTo(V020) > 0 && V070.compareTo(V021) > 0);
        assertEquals("0.7", V070.shortName());
        assertThrows(IllegalArgumentException.class, () -> SaveVersion.parse("soon"));
    }

    @Test
    void aNewWorldIsStampedAndNeedsNoBackup(@TempDir Path world) throws IOException {
        WorldGate.Outcome o = WorldGate.open(world, V020);
        assertEquals(WorldGate.Kind.NEW, o.kind());
        assertEquals(V020, WorldGate.read(world));
        assertEquals(List.of(), Backup.list(world));
    }

    @Test
    void upgradingBacksUpProgressAndNotRegionFiles(@TempDir Path world) throws IOException {
        populate(world);
        WorldGate.open(world, V020);
        file(world, "yearofice/later.dat", "LATER");
        WorldGate.Outcome o = WorldGate.open(world, V021);
        assertEquals(WorldGate.Kind.UPGRADE, o.kind());
        Path b = world.resolve("yearofice/backups/pre-0.2.0");
        assertEquals(b, o.backup());
        assertEquals("LEVEL", Files.readString(b.resolve("level.dat")));
        assertEquals("PLAYER", Files.readString(b.resolve("playerdata/abc.dat")));
        assertEquals("CLOCK", Files.readString(b.resolve("yearofice/yearofice_clock.dat")));
        assertEquals(V020, SaveVersion.parse(new com.google.gson.Gson().fromJson(Files.readString(b.resolve("yearofice/save_version.json")),
                com.google.gson.JsonObject.class).get("save_version").getAsString()), "the backup keeps the old stamp");
        assertFalse(Files.exists(b.resolve("region")), "region files are not copied");
        assertFalse(Files.exists(b.resolve("yearofice/backups")), "a backup never contains backups");
        assertEquals(V021, WorldGate.read(world));
        assertTrue(o.message().startsWith("This world will be upgraded from 0.2 to 0.2."), o.message());
    }

    @Test
    void aWorldFromBeforeTheGateIsUpgradedFromZero(@TempDir Path world) throws IOException {
        populate(world);
        WorldGate.Outcome o = WorldGate.open(world, V020);
        assertEquals(WorldGate.Kind.UPGRADE, o.kind());
        assertEquals(SaveVersion.ZERO, o.saved());
        assertTrue(Files.isRegularFile(world.resolve("yearofice/backups/pre-0.0.0/level.dat")));
    }

    @Test
    void anOlderJarRefusesANewerSave(@TempDir Path world) throws IOException {
        populate(world);
        WorldGate.open(world, V070);
        List<String> before = Backup.list(world);
        WorldGate.NewerWorldException e = assertThrows(WorldGate.NewerWorldException.class, () -> WorldGate.open(world, V021));
        assertEquals("This world was saved by a newer Year of Ice (0.7). Update the mod to open it.", e.getMessage());
        assertEquals(V070, WorldGate.read(world), "a refused open changes nothing");
        assertEquals(before, Backup.list(world));
    }

    @Test
    void openingTwiceChangesNothing(@TempDir Path world) throws IOException {
        populate(world);
        WorldGate.open(world, V020);
        WorldGate.open(world, V021);
        String stamp = Files.readString(WorldGate.stampFile(world));
        List<String> backups = Backup.list(world);
        WorldGate.Outcome again = WorldGate.open(world, V021);
        assertEquals(WorldGate.Kind.SAME, again.kind());
        assertNull(again.backup());
        assertEquals(stamp, Files.readString(WorldGate.stampFile(world)));
        assertEquals(backups, Backup.list(world));
    }

    @Test
    void theFirstBackupOfAVersionIsKept(@TempDir Path world) throws IOException {
        populate(world);
        WorldGate.open(world, V020);
        Path first = Backup.make(world, V020);
        file(world, "level.dat", "CHANGED");
        assertEquals(first, Backup.make(world, V020));
        assertEquals("LEVEL", Files.readString(first.resolve("level.dat")));
        assertNotNull(first);
    }
}
