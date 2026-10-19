package com.yearofice.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds the {@code yearofice:} ids the mod registers, from source and data, without starting Minecraft. It stands in
 * for the registry dump ({@code build/registries.json}, written by a GameTest server run) until that exists.
 *
 * <ul>
 *   <li>Java: in a file that uses {@code DeferredRegister}, every {@code .register*("id", ...)} call; in a file that
 *       handles a {@code Register*Event}, every {@code fromNamespaceAndPath(MOD_ID or "yearofice", "id")}.</li>
 *   <li>Data: datapack-registry and bootstrap folders (tags, biomes, world presets, Curios slots, damage types,
 *       species, fish, water life, plants).</li>
 * </ul>
 * GameTests are not inventory entries yet (the doc lists them per tab), so they are not scanned.
 */
final class RegisteredIds {
    /** An id and, for data files, the inventory type it must have ({@code null} matches any type). */
    record Found(String id, String type, String where) {}

    private static final Pattern REGISTER_CALL = Pattern.compile("\\.register\\w*\\(\\s*\"([a-z0-9_./]+)\"");
    private static final Pattern RESOURCE_ID = Pattern.compile(
        "fromNamespaceAndPath\\(\\s*(?:\\w+\\.)?(?:MOD_ID|\"yearofice\")\\s*,\\s*\"([a-z0-9_./]+)\"\\s*\\)");
    private static final Pattern REGISTER_EVENT = Pattern.compile("\\bRegister\\w*Event\\b");

    /** Data folders under data/yearofice/ whose files are registry entries, with the inventory type of each. */
    static final Map<String, String> DATA_FOLDERS = Map.of(
        "tags", "tag",
        "worldgen/biome", "biome",
        "worldgen/world_preset", "worldgen_hook",
        "curios/slots", "curios_slot",
        "species", "mob",
        "fish", "mob",
        "water_life", "mob",
        "plant", "plant_block");

    private RegisteredIds() {}

    static Set<Found> scan(Path root) {
        Set<Found> out = new LinkedHashSet<>();
        Path java = root.resolve("src/main/java");
        if (Files.isDirectory(java)) {
            for (Path p : files(java, ".java")) out.addAll(scanJava(read(p), root.relativize(p).toString()));
        }
        for (String res : List.of("src/main/resources/data/yearofice", "src/generated/resources/data/yearofice")) {
            Path base = root.resolve(res);
            for (Map.Entry<String, String> e : DATA_FOLDERS.entrySet()) {
                Path dir = base.resolve(e.getKey());
                if (!Files.isDirectory(dir)) continue;
                for (Path p : files(dir, ".json")) out.add(new Found(dataId(dir, p, e.getKey()), e.getValue(), root.relativize(p).toString()));
            }
        }
        return out;
    }

    /** The registrations in one Java source file. */
    static Set<Found> scanJava(String src, String where) {
        Set<Found> out = new LinkedHashSet<>();
        if (src.contains("DeferredRegister")) {
            Matcher m = REGISTER_CALL.matcher(src);
            while (m.find()) out.add(new Found(m.group(1), null, where));
        }
        if (REGISTER_EVENT.matcher(src).find()) {
            Matcher m = RESOURCE_ID.matcher(src);
            while (m.find()) out.add(new Found(m.group(1), null, where));
        }
        return out;
    }

    /** tags/<registry>/<path>.json is the tag <path>; any other folder's id is the file's path below it. */
    private static String dataId(Path dir, Path file, String folder) {
        Path rel = dir.relativize(file);
        if (folder.equals("tags") && rel.getNameCount() > 1) {
            // tags/worldgen/biome/x.json and tags/block/x.json: drop the registry folders.
            int skip = rel.getName(0).toString().equals("worldgen") ? 2 : 1;
            rel = rel.subpath(Math.min(skip, rel.getNameCount() - 1), rel.getNameCount());
        }
        String s = rel.toString().replace('\\', '/');
        return s.substring(0, s.length() - ".json".length());
    }

    private static List<Path> files(Path dir, String ext) {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(ext)).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
