package com.yearofice.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Remembers which attempt won the start-site search for a (level seed, worldgen source hash), so the next boot builds
 * only that attempt instead of re-running every failed one. The search is a pure function of the seed, so the cached
 * attempt gives exactly the layers a full search gives (proved by the {@code worldgen/layer_cache_matches} GameTest).
 * The hash covers the worldgen and sim classes plus the worldgen data, so any change to either drops the entry.
 * No Minecraft imports; every failure to read or write the cache falls back to the full search.
 */
public final class LayerCache {
    private LayerCache() {}

    private static final String[] PREFIXES = {"com/yearofice/worldgen/", "com/yearofice/sim/", "com/yearofice/core/hash/",
            "data/yearofice/worldgen_params/", "data/yearofice/worldgen/", "data/yearofice/setpiece/", "data/yearofice/archetype/"};
    private static volatile String sourceHash;

    /** The cached winning attempt, or -1 (no entry, stale hash, no directory, unreadable). */
    public static int load(Path dir, long levelSeed) {
        if (dir == null) return -1;
        String h = sourceHash();
        Path f = dir.resolve(levelSeed + ".properties");
        if (h == null || !Files.isRegularFile(f)) return -1;
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            if (!h.equals(p.getProperty("hash"))) return -1;
            int a = Integer.parseInt(p.getProperty("attempt", "-1").trim());
            return a >= 0 ? a : -1;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    public static void store(Path dir, long levelSeed, int attempt) {
        String h = sourceHash();
        if (dir == null || h == null) return;
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(levelSeed + ".properties.tmp");
            Files.writeString(tmp, "hash=" + h + "\nattempt=" + attempt + "\n", StandardCharsets.UTF_8);
            Files.move(tmp, dir.resolve(levelSeed + ".properties"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            // a read-only world folder just means no cache
        }
    }

    /** SHA-256 over the worldgen sources (classes and data), or null when the code location cannot be read. */
    public static String sourceHash() {
        String h = sourceHash;
        if (h != null) return h;
        try {
            ClassLoader cl = LayerCache.class.getClassLoader();
            List<Path> roots = new ArrayList<>();
            for (String probe : new String[] {"com/yearofice/worldgen/LayerCache.class", WorldgenParams.RESOURCE}) {
                URL u = cl.getResource(probe);
                if (u == null) return null;
                Path r = root(u, probe);
                if (r == null) { System.getLogger("yearofice").log(System.Logger.Level.WARNING, "layer cache disabled: no root for " + u); return null; }
                if (!roots.contains(r)) roots.add(r);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Path rp : roots) {
                if (Files.isDirectory(rp)) hashDir(md, rp);
                else hashZip(md, rp);
            }
            return sourceHash = HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            System.getLogger("yearofice").log(System.Logger.Level.WARNING, "layer cache disabled: cannot hash worldgen sources", e);
            return null;
        }
    }

    /** The classpath root a probe resource sits under, as a path (directory, jar, or the mod loader's union filesystem). */
    private static Path root(URL u, String probe) throws Exception {
        Path p;
        try {
            p = Path.of(u.toURI());
        } catch (RuntimeException e) {
            String s = u.toString();
            if (!s.startsWith("jar:")) return null;
            return Path.of(new java.net.URI(s.substring(4, s.indexOf("!/"))));
        }
        for (int i = 0, up = probe.split("/").length; i < up; i++) p = p.getParent();
        return p;
    }

    private static boolean wanted(String name) {
        for (String p : PREFIXES) if (name.startsWith(p)) return true;
        return false;
    }

    private static void hashDir(MessageDigest md, Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String p : PREFIXES) {
            Path d = root.resolve(p);
            if (!Files.isDirectory(d)) continue;
            try (var s = Files.walk(d)) { s.filter(Files::isRegularFile).forEach(files::add); }
        }
        Collections.sort(files);
        for (Path f : files) {
            md.update(root.relativize(f).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
            md.update(Files.readAllBytes(f));
        }
    }

    private static void hashZip(MessageDigest md, Path jar) throws IOException {
        try (ZipFile z = new ZipFile(jar.toFile())) {
            List<? extends ZipEntry> es = Collections.list(z.entries());
            es.sort((a, b) -> a.getName().compareTo(b.getName()));
            for (ZipEntry e : es) {
                if (e.isDirectory() || !wanted(e.getName())) continue;
                md.update(e.getName().getBytes(StandardCharsets.UTF_8));
                try (InputStream in = z.getInputStream(e)) { md.update(in.readAllBytes()); }
            }
        }
    }
}
