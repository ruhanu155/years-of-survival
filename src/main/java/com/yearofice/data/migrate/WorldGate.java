package com.yearofice.data.migrate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The version gate on a world (Saves across versions › The promise). {@code <world>/yearofice/save_version.json}
 * holds the version of the jar that last saved the world. On open:
 * <ul>
 *   <li>no stamp and no {@code level.dat}: a new world, stamped with this jar;</li>
 *   <li>no stamp but a {@code level.dat}: a world from before the gate, treated as saved by {@code 0.0.0};</li>
 *   <li>stamp above this jar: refused with {@link NewerWorldException} (a save never opens in an older version);</li>
 *   <li>stamp below this jar: backed up to {@code backups/pre-<old>/}, then stamped;</li>
 *   <li>stamp equal: nothing changes (so a second load is idempotent).</li>
 * </ul>
 * Pure {@code java.nio}: no Minecraft, so the whole gate is unit-tested.
 */
public final class WorldGate {
    private WorldGate() {}

    public static final String DIR = "yearofice";
    public static final String FILE = "save_version.json";

    public enum Kind { NEW, SAME, UPGRADE }

    /** What opening did. {@code backup} is set only for an upgrade. */
    public record Outcome(Kind kind, SaveVersion saved, SaveVersion jar, Path backup) {
        public String message() {
            return switch (kind) {
                case NEW -> "New world, saved by Year of Ice " + jar.shortName() + ".";
                case SAME -> "World saved by Year of Ice " + saved.shortName() + ".";
                case UPGRADE -> upgradeMessage(saved, jar);
            };
        }
    }

    public static final class NewerWorldException extends RuntimeException {
        public final SaveVersion saved;

        NewerWorldException(SaveVersion saved) {
            super("This world was saved by a newer Year of Ice (" + saved.shortName() + "). Update the mod to open it.");
            this.saved = saved;
        }
    }

    public static String upgradeMessage(SaveVersion from, SaveVersion to) {
        return "This world will be upgraded from " + from.shortName() + " to " + to.shortName()
                + ". A backup of your progress is kept. Region files are not copied; use Backup in the world menu for a full copy.";
    }

    public static Path stampFile(Path worldDir) { return worldDir.resolve(DIR).resolve(FILE); }

    /** The stamped version, or {@code null} when the world has none. */
    public static SaveVersion read(Path worldDir) throws IOException {
        Path f = stampFile(worldDir);
        if (!Files.isRegularFile(f)) return null;
        JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        return SaveVersion.parse(o.get("save_version").getAsString());
    }

    public static Outcome open(Path worldDir, SaveVersion jar) throws IOException {
        SaveVersion saved = read(worldDir);
        if (saved == null) {
            boolean existing = Files.isRegularFile(worldDir.resolve("level.dat"));
            if (!existing) {
                write(worldDir, jar);
                return new Outcome(Kind.NEW, jar, jar, null);
            }
            saved = SaveVersion.ZERO;
        }
        int c = saved.compareTo(jar);
        if (c > 0) throw new NewerWorldException(saved);
        if (c == 0) return new Outcome(Kind.SAME, saved, jar, null);
        Path backup = Backup.make(worldDir, saved);
        write(worldDir, jar);
        return new Outcome(Kind.UPGRADE, saved, jar, backup);
    }

    private static void write(Path worldDir, SaveVersion v) throws IOException {
        Path f = stampFile(worldDir);
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling(FILE + ".tmp");
        Files.writeString(tmp, "{\n  \"save_version\": \"" + v + "\"\n}\n", StandardCharsets.UTF_8);
        Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
