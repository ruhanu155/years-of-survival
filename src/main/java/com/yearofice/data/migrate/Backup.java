package com.yearofice.data.migrate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * The pre-upgrade backup (Saves across versions › Migration runs lazily and is backed up first): {@code level.dat},
 * {@code playerdata/} and {@code <world>/yearofice/} are copied to {@code <world>/yearofice/backups/pre-<old>/}.
 * Region files are not copied. The first backup of a version is kept; a second upgrade from the same version never
 * overwrites it.
 */
public final class Backup {
    private Backup() {}

    public static final String BACKUPS = "backups";

    public static Path folder(Path worldDir, SaveVersion old) {
        return worldDir.resolve(WorldGate.DIR).resolve(BACKUPS).resolve("pre-" + old);
    }

    /** Copies the progress files; returns the backup folder (existing or new). */
    public static Path make(Path worldDir, SaveVersion old) throws IOException {
        Path target = folder(worldDir, old);
        if (Files.isDirectory(target)) return target;
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        deleteTree(tmp);
        Files.createDirectories(tmp);
        Path levelDat = worldDir.resolve("level.dat");
        if (Files.isRegularFile(levelDat)) Files.copy(levelDat, tmp.resolve("level.dat"));
        copyTree(worldDir.resolve("playerdata"), tmp.resolve("playerdata"), List.of());
        copyTree(worldDir.resolve(WorldGate.DIR), tmp.resolve(WorldGate.DIR), List.of(BACKUPS));
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    /** The backup folders present, oldest name first. */
    public static List<String> list(Path worldDir) throws IOException {
        Path dir = worldDir.resolve(WorldGate.DIR).resolve(BACKUPS);
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isDirectory).map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith("pre-") && !n.endsWith(".tmp")).sorted().toList();
        }
    }

    private static void copyTree(Path from, Path to, List<String> skipTopLevel) throws IOException {
        if (!Files.isDirectory(from)) return;
        try (Stream<Path> s = Files.walk(from)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Path rel = from.relativize(p);
                if (rel.getNameCount() > 0 && !rel.toString().isEmpty() && skipTopLevel.contains(rel.getName(0).toString())) continue;
                Path dest = to.resolve(rel.toString());
                if (Files.isDirectory(p)) Files.createDirectories(dest);
                else Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteTree(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            for (Path q : (Iterable<Path>) s.sorted(java.util.Comparator.reverseOrder())::iterator) Files.delete(q);
        }
    }
}
