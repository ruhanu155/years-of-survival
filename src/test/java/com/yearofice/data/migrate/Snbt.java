package com.yearofice.data.migrate;

import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

/** Fixture files under {@code src/test/resources/saves/<version>/} (data/migrate_fixtures): SNBT snapshots. */
final class Snbt {
    private Snbt() {}

    static final Path SAVES = Path.of("").toAbsolutePath().resolve("src/test/resources/saves");
    static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;

    static Tag read(Path p) {
        try {
            String s = Files.readString(p, StandardCharsets.UTF_8).trim();
            // The parser reads a compound or a list; a bare value is wrapped.
            return TagParser.parseTag("{v:" + s + "}").get("v");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(p + ": " + e.getMessage(), e);
        }
    }

    static Tag read(String version, String file) { return read(SAVES.resolve(version).resolve(file)); }

    /** Every fixture version folder that holds a world snapshot (an {@code attachments} folder), oldest first. */
    static List<String> versions() {
        try (Stream<Path> s = Files.list(SAVES)) {
            return s.filter(p -> Files.isDirectory(p.resolve("attachments"))).map(p -> p.getFileName().toString())
                    .sorted(java.util.Comparator.comparing(SaveVersion::parse)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The {@code .snbt} fixture paths below a folder of a version, relative, sorted. */
    static List<String> files(String version, String folder) {
        Path dir = SAVES.resolve(version).resolve(folder);
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> folder + "/" + p.getFileName()).filter(n -> n.endsWith(".snbt")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A copy with every list of compounds sorted by text, so hash-ordered saves compare equal. */
    static Tag canonical(Tag t) {
        if (t instanceof CompoundTag c) {
            CompoundTag out = new CompoundTag();
            for (String k : c.getAllKeys()) out.put(k, canonical(c.get(k)));
            return out;
        }
        if (t instanceof ListTag l) {
            List<Tag> items = new ArrayList<>();
            for (Tag e : l) items.add(canonical(e));
            if (!items.isEmpty() && items.get(0) instanceof CompoundTag) items.sort(java.util.Comparator.comparing(Tag::toString));
            ListTag out = new ListTag();
            out.addAll(items);
            return out;
        }
        return t;
    }
}
