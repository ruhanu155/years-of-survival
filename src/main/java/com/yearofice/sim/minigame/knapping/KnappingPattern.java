/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Adapted for Year of Ice from TerraFirmaCraft/TerraFirmaCraft@c3b4d9536b82e5a9746dd793c586720a74108057,
 * src/main/java/net/dries007/tfc/util/data/KnappingPattern.java
 * (the bitset grid, string parsing and the shifted, mirrored subset match are closely adapted; codecs removed
 * so the class has no Minecraft or DataFixerUpper imports, and the class made immutable).
 */
package com.yearofice.sim.minigame.knapping;

import java.util.List;

/**
 * A knapping grid of up to 5 × 5 cells, each on (stone left) or off (flaked away).
 *
 * <p>A pattern smaller than 5 × 5 implies every cell outside it equals {@code defaultOn}. A grid
 * {@linkplain #matches matches} a pattern if the pattern is found at any shift, plain or mirrored, with the
 * outside cells at the default. Patterns are written as rows where anything other than a space is on:
 * <pre>
 *   "  #  ",
 *   " ### ",
 *   " ### ",
 *   "#####",
 *   " # # "
 * </pre>
 * Year of Ice uses the grid as the core's shape record: each good flake turns off the platform cell it was
 * struck from, the knapping screen draws it, and a recipe may require a target shape. Hide and clay patterns
 * are not in the blueprint, so only stone, flint and glass use it.
 */
public final class KnappingPattern {
    public static final int MAX_WIDTH = 5;
    public static final int MAX_HEIGHT = 5;

    private final int width;
    private final int height;
    private final boolean defaultOn;
    /** Bit {@code x + y * width} is 1 when that cell is on. */
    private final int data;

    private KnappingPattern(int width, int height, int data, boolean defaultOn) {
        this.width = width;
        this.height = height;
        this.data = data & mask(width, height);
        this.defaultOn = defaultOn;
    }

    /** A full grid, every cell on: an unworked core. */
    public static KnappingPattern full() {
        return new KnappingPattern(MAX_WIDTH, MAX_HEIGHT, mask(MAX_WIDTH, MAX_HEIGHT), false);
    }

    /** Parse rows; {@code defaultOn} is required for patterns smaller than 5 × 5. */
    public static KnappingPattern of(boolean defaultOn, String... rows) {
        return parse(List.of(rows), defaultOn);
    }

    public static KnappingPattern parse(List<String> rows, boolean defaultOn) {
        int h = rows.size();
        if (h == 0 || h > MAX_HEIGHT) throw new IllegalArgumentException("pattern must have 1 to " + MAX_HEIGHT + " rows");
        int w = rows.get(0).length();
        if (w == 0 || w > MAX_WIDTH) throw new IllegalArgumentException("pattern must have 1 to " + MAX_WIDTH + " columns");
        int bits = 0;
        for (int r = 0; r < h; r++) {
            String row = rows.get(r);
            if (row.length() != w) throw new IllegalArgumentException("each row must be the same width");
            for (int c = 0; c < w; c++) {
                if (row.charAt(c) != ' ') bits |= 1 << (r * w + c);
            }
        }
        return new KnappingPattern(w, h, bits, defaultOn);
    }

    private static int mask(int w, int h) {
        int n = w * h;
        return n >= 32 ? -1 : (1 << n) - 1;
    }

    public int width() { return width; }

    public int height() { return height; }

    public boolean defaultOn() { return defaultOn; }

    public boolean get(int x, int y) { return ((data >> (x + y * width)) & 1) == 1; }

    /** A copy with one cell set. */
    public KnappingPattern with(int x, int y, boolean on) {
        if (x < 0 || y < 0 || x >= width || y >= height) throw new IndexOutOfBoundsException("cell " + x + "," + y);
        int bit = 1 << (x + y * width);
        return new KnappingPattern(width, height, on ? (data | bit) : (data & ~bit), defaultOn);
    }

    public int onCount() { return Integer.bitCount(data); }

    /**
     * True if {@code other} is found in this grid at some shift, plain or mirrored left to right, with every
     * cell of this grid outside it equal to {@code other.defaultOn()}.
     */
    public boolean matches(KnappingPattern other) {
        for (int dx = 0; dx <= width - other.width; dx++) {
            for (int dy = 0; dy <= height - other.height; dy++) {
                if (matches(other, dx, dy, false) || matches(other, dx, dy, true)) return true;
            }
        }
        return false;
    }

    private boolean matches(KnappingPattern other, int startX, int startY, boolean mirror) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean here = get(x, y);
                if (x < startX || y < startY || x - startX >= other.width || y - startY >= other.height) {
                    if (here != other.defaultOn) return false;
                } else {
                    int ox = mirror ? other.width - 1 - (x - startX) : x - startX;
                    if (here != other.get(ox, y - startY)) return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KnappingPattern p && width == p.width && height == p.height
                && defaultOn == p.defaultOn && data == p.data;
    }

    @Override
    public int hashCode() { return ((data * 31 + width) * 31 + height) * 2 + (defaultOn ? 1 : 0); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            if (y > 0) sb.append('/');
            for (int x = 0; x < width; x++) sb.append(get(x, y) ? '#' : ' ');
        }
        return "Knapping[" + sb + (defaultOn ? ",default_on" : "") + "]";
    }
}
