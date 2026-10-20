package com.yearofice.data.migrate;

/**
 * A mod version as the save promise sees it: {@code major.minor.patch}, with any {@code -beta.1} or {@code +build}
 * suffix dropped (Minecraft development › Saves across versions). The promise holds within one MAJOR; messages to the
 * player name only {@code major.minor}.
 */
public record SaveVersion(int major, int minor, int patch) implements Comparable<SaveVersion> {
    /** The version of a world with no stamp yet (made before the gate existed). */
    public static final SaveVersion ZERO = new SaveVersion(0, 0, 0);

    public static SaveVersion parse(String text) {
        String s = text.trim();
        if (s.startsWith("v")) s = s.substring(1);
        int cut = s.length();
        for (char c : new char[] {'-', '+'}) {
            int i = s.indexOf(c);
            if (i >= 0) cut = Math.min(cut, i);
        }
        String[] p = s.substring(0, cut).split("\\.");
        if (p.length < 1 || p.length > 3) throw new IllegalArgumentException("not a version: " + text);
        try {
            return new SaveVersion(Integer.parseInt(p[0]), p.length > 1 ? Integer.parseInt(p[1]) : 0,
                    p.length > 2 ? Integer.parseInt(p[2]) : 0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a version: " + text, e);
        }
    }

    @Override
    public int compareTo(SaveVersion o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    /** {@code 0.7}, as the player-facing messages print it. */
    public String shortName() { return major + "." + minor; }

    @Override
    public String toString() { return major + "." + minor + "." + patch; }
}
