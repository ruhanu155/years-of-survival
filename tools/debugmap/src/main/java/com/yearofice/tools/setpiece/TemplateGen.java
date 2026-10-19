package com.yearofice.tools.setpiece;

import com.yearofice.worldgen.SetPieceDef;
import com.yearofice.worldgen.debug.StructureNbt;
import com.yearofice.worldgen.debug.StructureNbt.Builder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the M1 placeholder templates for every set-piece variant to
 * {@code src/main/resources/data/yearofice/structure/setpiece/<id>/<variant>.nbt}, each the size of its
 * {@code footprint_m} and {@code height_m}, built from vanilla blocks with the door on the south (+z) side.
 * They stand in until the real builds are authored in a creative test world and saved with structure blocks;
 * no air is saved, no jigsaw is used, and nothing holds loot yet. Also writes the GameTest pad
 * {@code structure/gametest/setpiece_pad.nbt}. Run with {@code ./gradlew setpieceTemplates}.
 */
public final class TemplateGen {
    private TemplateGen() {}

    public static void main(String[] args) throws IOException {
        File root = new File(args.length > 0 ? args[0] : "src/main/resources/data/yearofice/structure");
        int n = 0;
        for (SetPieceDef d : SetPieceDef.loadAll()) {
            for (SetPieceDef.Variant v : d.variants()) {
                Builder b = new Builder(v.footprintX(), v.heightM(), v.footprintZ());
                build(v.template(), b, v.footprintX(), v.heightM(), v.footprintZ());
                File f = new File(root, "setpiece/" + v.template() + ".nbt");
                f.getParentFile().mkdirs();
                try (OutputStream o = new FileOutputStream(f)) { StructureNbt.write(b.build(), o); }
                n++;
            }
        }
        Builder pad = new Builder(34, 22, 34);
        pad.box(0, 0, 0, 33, 0, 33, "stone");
        File f = new File(root, "gametest/setpiece_pad.nbt");
        f.getParentFile().mkdirs();
        try (OutputStream o = new FileOutputStream(f)) { StructureNbt.write(pad.build(), o); }
        System.out.println("wrote " + n + " set-piece templates and the GameTest pad under " + root);
    }

    static final String LOG = "spruce_log[axis=y]", LOGX = "spruce_log[axis=x]", LOGZ = "spruce_log[axis=z]",
            PLANK = "spruce_planks", SLAB = "spruce_slab[type=bottom]", FENCE = "spruce_fence", STONE = "cobblestone",
            MOSS = "moss_block", GLASS = "glass", WHITE = "white_concrete", ORANGE = "orange_concrete",
            GREY = "light_gray_concrete", RUST = "brown_terracotta", GRAVEL = "gravel", ICE = "packed_ice";

    /** A small log cabin, door gap on the south wall. */
    static void cabin(Builder b, int x0, int z0, int w, int d, int wallH, String roof, boolean caved) {
        b.walls(x0, 0, z0, x0 + w - 1, wallH - 1, z0 + d - 1, LOG);
        int door = x0 + w / 2;
        b.set(door, 0, z0 + d - 1, null).set(door, 1, z0 + d - 1, null);
        b.set(x0, 1, z0 + d / 2, GLASS);
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            if (caved && (x + z) % 3 == 0) continue;
            b.set(x, wallH, z, roof);
        }
        b.set(x0 + 1, 0, z0 + 1, "chest[facing=south]");
    }

    static void build(String template, Builder b, int sx, int sy, int sz) {
        int cx = sx / 2, cz = sz / 2;
        switch (template) {
            case "outpost/day_one" -> {
                cabin(b, 1, 1, 5, 6, 3, PLANK, true);
                b.box(8, 0, 8, 10, 0, 10, STONE).set(9, 0, 9, null); // the fire ring
                b.set(8, 0, 2, "barrel[facing=up]"); // the crate
                b.set(9, 0, 2, "lectern[facing=south]"); // the radio table
            }
            case "plane_wreck/nose_down_bar" -> {
                b.box(2, 0, cz, 11, 1, cz, WHITE); // fuselage, nose in the gravel
                b.box(1, 0, cz - 1, 2, 0, cz + 1, GREY);
                b.box(5, 1, 1, 7, 1, sz - 2, GREY); // the wing
                b.box(11, 2, cz, 12, 5, cz, ORANGE); // the tail, high
                b.box(10, 4, cz - 2, 12, 4, cz + 2, WHITE);
                b.set(6, 2, cz, GLASS);
            }
            case "plane_wreck/treetops" -> {
                b.box(3, 2, cz, 11, 3, cz, WHITE);
                b.box(10, 3, cz, 11, 4, cz, ORANGE);
                b.box(1, 0, 1, 4, 0, 2, GREY); // a sheared wing on the ground
                b.box(8, 0, sz - 3, 12, 0, sz - 2, GREY);
                for (int y = 0; y < 2; y++) { b.set(4, y, cz, LOG); b.set(9, y, cz, LOG); }
            }
            case "plane_wreck/half_sunk_beach" -> {
                b.box(2, 0, cz, 12, 1, cz, WHITE);
                b.box(12, 1, cz, 13, 4, cz, ORANGE);
                b.box(6, 1, 2, 8, 1, sz - 3, GREY);
                b.set(4, 2, cz, GLASS);
            }
            case "arvid_cabin/sod_roof" -> {
                cabin(b, 1, 1, 5, 6, 3, MOSS, true);
                for (int[] leg : new int[][] {{7, 1}, {9, 1}, {7, 3}, {9, 3}}) b.box(leg[0], 0, leg[1], leg[0], 2, leg[1], FENCE);
                b.box(7, 3, 1, 9, 3, 3, PLANK); // the cache on four legs
                b.box(7, 4, 1, 9, 5, 3, LOG);
            }
            case "arvid_cabin/dugout" -> {
                b.box(0, 0, 0, sx - 1, 3, sz - 3, "coarse_dirt"); // the bank it is cut into
                b.walls(1, 0, sz - 3, sx - 2, 2, sz - 2, LOGX);
                b.set(cx, 0, sz - 2, null).set(cx, 1, sz - 2, null); // the doorway
                b.box(1, 0, 1, sx - 2, 2, sz - 4, null);
                b.set(2, 0, 2, "chest[facing=south]");
            }
            case "arvid_cabin/line_cabin" -> {
                cabin(b, 0, 0, 5, 5, 3, PLANK, false);
                b.box(3, 4, 1, 3, 5, 1, "iron_bars"); // the stovepipe
            }
            case "mining_camp/placer_camp" -> {
                for (int x = 1; x < sx - 1; x++) b.set(x, 1, cz, SLAB); // the sluice line
                for (int x = 1; x < sx - 1; x += 4) b.box(x, 0, cz, x, 0, cz, FENCE);
                for (int[] p : new int[][] {{3, 3}, {9, 2}, {15, 4}, {20, sz - 4}}) b.box(p[0], 0, p[1], p[0] + 2, 1, p[1] + 2, GRAVEL);
                cabin(b, 2, sz - 8, 6, 6, 2, PLANK, true); // the collapsed bunkhouse
                b.box(sx - 4, 0, sz - 3, sx - 4, 2, sz - 3, PLANK); // the grave board
            }
            case "mining_camp/prospect_adit" -> {
                b.box(1, 0, 0, sx - 2, sy - 3, 6, STONE); // the face it is driven into
                b.box(cx - 1, 0, 0, cx + 1, 2, 6, null);
                b.box(cx - 2, 0, 6, cx - 2, 3, 6, LOG).box(cx + 2, 0, 6, cx + 2, 3, 6, LOG).box(cx - 2, 3, 6, cx + 2, 3, 6, LOGX);
                b.box(cx - 2, 0, 10, cx - 2, sy - 1, 10, LOG).box(cx + 2, 0, 10, cx + 2, sy - 1, 10, LOG);
                b.box(cx - 2, sy - 1, 10, cx + 2, sy - 1, 10, LOGX); // the headframe
                b.box(cx - 3, 0, sz - 3, cx - 1, 1, sz - 2, GRAVEL);
            }
            case "mining_camp/bucket_dredge" -> {
                b.box(2, 0, 4, sx - 3, 3, sz - 5, RUST); // the hull
                b.box(4, 4, 6, sx - 5, 7, sz - 8, "spruce_planks");
                b.box(cx - 1, 4, 2, cx + 1, sy - 1, 3, "iron_bars"); // the gantry, 14 m
                b.box(cx - 1, 2, 0, cx + 1, 6, 1, "iron_block");
                b.box(4, 8, 6, sx - 5, 8, sz - 8, "dark_oak_slab[type=bottom]");
            }
            case "fire_lookout/tower_intact" -> {
                for (int[] c : new int[][] {{1, 1}, {sx - 2, 1}, {1, sz - 2}, {sx - 2, sz - 2}}) b.box(c[0], 0, c[1], c[0], 11, c[1], LOG);
                for (int y = 2; y < 11; y += 3) b.box(2, y, cz, sx - 3, y, cz, LOGX); // cross braces
                b.box(0, 12, 0, sx - 1, 12, sz - 1, PLANK);
                b.walls(1, 13, 1, sx - 2, 15, sz - 2, GLASS);
                b.box(1, 16, 1, sx - 2, 16, sz - 2, PLANK);
                b.set(cx, 17, cz, "lightning_rod");
                for (int y = 0; y < 12; y++) b.set(cx + (y % 2), y, sz - 1 - (y / 4), "spruce_stairs[facing=north]");
            }
            case "fire_lookout/ground_cab" -> {
                b.box(0, 0, 0, sx - 1, 0, sz - 1, STONE);
                b.walls(1, 1, 1, sx - 2, 3, sz - 2, GLASS);
                b.box(1, 4, 1, sx - 2, 4, sz - 2, PLANK);
                b.set(cx, 1, sz - 2, null).set(cx, 2, sz - 2, null);
            }
            case "fire_lookout/tower_collapsed" -> {
                for (int[] c : new int[][] {{1, 1}, {sx - 2, 1}, {1, sz - 2}, {sx - 2, sz - 2}}) b.box(c[0], 0, c[1], c[0], 0, c[1], STONE);
                b.box(1, 1, 1, 1, 4, 1, LOG); // one leg still standing
                b.box(2, 0, 3, sx - 1, 0, 3, LOGX).box(3, 0, 6, 3, 0, sz - 1, LOGZ); // the fallen ones
                b.box(cx, 0, sz - 2, cx, 2, sz - 2, "spruce_stairs[facing=north]");
            }
            case "hot_spring/roadhouse_pool" -> {
                b.walls(1, 0, 1, 9, 3, 8, LOG); // the ruined roadhouse
                b.set(5, 0, 8, null).set(5, 1, 8, null);
                b.box(1, 4, 1, 9, 4, 8, PLANK).box(4, 4, 3, 6, 4, 6, null);
                b.box(3, 0, 3, 3, 0, 3, "blast_furnace[facing=south]"); // the 60 kg stove
                b.walls(11, 0, 4, sx - 2, 0, sz - 3, STONE); // the pool rim
                b.box(12, 0, 5, sx - 3, 0, sz - 4, null);
            }
            case "hot_spring/bog_pools" -> {
                for (int[] p : new int[][] {{2, 2, 3}, {9, 4, 2}, {5, 11, 3}, {12, 12, 2}}) b.walls(p[0], 0, p[1], p[0] + p[2] + 1, 0, p[1] + p[2] + 1, "tuff");
                b.set(1, 0, sz - 2, FENCE);
            }
            case "hot_spring/cliff_pool" -> {
                b.box(0, 0, 0, sx - 1, sy - 1, 2, STONE); // the foot of the face
                b.walls(2, 0, 3, sx - 3, 0, sz - 2, STONE);
                b.box(3, 0, 4, sx - 4, 0, sz - 3, null);
            }
            case "ice_cave/cold_trap" -> {
                b.box(0, 0, 0, sx - 1, sy - 1, sz - 1, STONE);
                b.box(2, 0, 0, sx - 3, sy - 3, sz - 1, null); // the mouth, open to the south
                b.box(2, 0, 0, sx - 3, 0, sz - 2, ICE); // glare ice floor
                b.box(2, sy - 3, 1, sx - 3, sy - 3, 2, "blue_ice");
                b.set(2, 1, sz - 1, "stripped_spruce_wood[axis=y]"); // the post carved "D. 1909"
                b.set(cx, 1, 0, "bone_block[axis=x]"); // the tusk in the back wall
            }
            case "ice_cave/snowfield_cave" -> {
                b.box(0, 0, 0, sx - 1, sy - 1, sz - 1, "snow_block");
                b.box(2, 0, 0, sx - 3, sy - 3, sz - 1, null);
                b.box(2, 0, 0, sx - 3, 0, sz - 2, ICE);
            }
            case "ice_cave/miners_tunnel" -> {
                b.box(0, 0, 0, sx - 1, sy - 1, sz - 1, "coarse_dirt");
                b.box(2, 0, 0, sx - 3, 2, sz - 1, null);
                for (int z = 1; z < sz; z += 3) {
                    b.box(1, 0, z, 1, 2, z, LOG).box(sx - 2, 0, z, sx - 2, 2, z, LOG).box(1, 3, z, sx - 2, 3, z, LOGX);
                }
                b.box(2, 0, 0, sx - 3, 0, sz - 2, ICE);
            }
            case "beaver_valley/dam_staircase", "beaver_valley/great_dam", "beaver_valley/drained_meadow" -> {
                // Arvid's last line cabin, ruined; the ponds are overlays (M9), not template blocks.
                b.walls(1, 0, 1, 6, 1, 6, LOG);
                b.set(3, 1, 6, null).set(4, 1, 6, null).set(3, 0, 6, null);
                b.box(1, 2, 1, 2, 2, 6, LOG);
                b.box(1, 0, 1, 1, 3, 1, "iron_bars"); // the stretcher frame
                if (template.endsWith("great_dam")) b.box(0, 0, 7, 7, 0, 7, "stripped_spruce_log[axis=x]");
                if (template.endsWith("dam_staircase")) b.box(0, 0, 7, 3, 1, 7, "stripped_spruce_log[axis=x]");
            }
            default -> throw new IllegalArgumentException("no placeholder for " + template);
        }
    }
}
