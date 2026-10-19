package com.yearofice.tools.debugmap;

import com.yearofice.worldgen.Landform;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.RiverPath;
import com.yearofice.worldgen.SeedStage;
import com.yearofice.worldgen.debug.MapRender;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Desktop renderer of the worldgen layers. For each seed it runs stages 1–5 (with rerolls), then writes
 * PNGs: M0's {@code overview} (eroded height, hillshade, tint, 50 m contours, river path), {@code landform}
 * (stage 2 before erosion), {@code hardness}, {@code sediment}, {@code erosion_change}, {@code coarse} (the
 * 8 km world at 32 m); M1's {@code m1} (everything), {@code water}, {@code current}, {@code spots},
 * {@code rock}, {@code places} and {@code world}; and, for several seeds, contact sheets ({@code sheet.png} for
 * M0, {@code sheet-m1.png} and {@code sheet-world.png} for M1).
 *
 * <p>Args: {@code seed=N}, {@code seeds=a..b} or {@code seeds=a,b,c}, {@code attempt=K} (first attempt),
 * {@code rerolls=R} (render R successive attempts per seed, ignoring checks), {@code out=dir}.
 */
public final class DebugMap {
    private DebugMap() {}

    public static void main(String[] args) throws IOException {
        Map<String, String> a = new LinkedHashMap<>();
        for (String s : args) for (String part : s.trim().split("\\s+")) {
            int eq = part.indexOf('=');
            if (eq > 0) a.put(part.substring(0, eq), part.substring(eq + 1));
        }
        List<Long> seeds = new ArrayList<>();
        if (a.containsKey("seeds")) {
            String v = a.get("seeds");
            if (v.contains("..")) {
                long lo = Long.parseLong(v.substring(0, v.indexOf(".."))), hi = Long.parseLong(v.substring(v.indexOf("..") + 2));
                for (long s = lo; s <= hi; s++) seeds.add(s);
            } else {
                for (String s : v.split(",")) seeds.add(Long.parseLong(s.trim()));
            }
        } else {
            seeds.add(Long.parseLong(a.getOrDefault("seed", "3141592")));
        }
        int attempt = Integer.parseInt(a.getOrDefault("attempt", "0"));
        int rerolls = Integer.parseInt(a.getOrDefault("rerolls", "0"));
        File out = new File(a.getOrDefault("out", "build/debugmap"));
        out.mkdirs();

        Pipeline pipe = Pipeline.standard();
        List<BufferedImage> sheet = new ArrayList<>();
        List<String> sheetLabels = new ArrayList<>();
        List<BufferedImage> sheetM1 = new ArrayList<>();
        List<BufferedImage> sheetWorld = new ArrayList<>();
        for (long seed : seeds) {
            List<Pipeline.Result> results = new ArrayList<>();
            if (rerolls > 0) {
                for (int k = attempt; k < attempt + rerolls; k++) results.add(pipe.attempt(seed, k));
            } else {
                long t0 = System.nanoTime();
                results.add(pipe.run(seed, attempt));
                System.out.printf("seed %d: stages 1-5 in %.2f s%n", seed, (System.nanoTime() - t0) / 1e9);
            }
            for (Pipeline.Result r : results) {
                File dir = new File(out, "seed-" + seed + (rerolls > 0 || r.attempt() > 0 ? "-a" + r.attempt() : ""));
                dir.mkdirs();
                BufferedImage overview = overview(pipe, r);
                write(overview, new File(dir, "overview.png"));
                write(shaded(r.landform().height(), r.landform().cells(), null, r), new File(dir, "landform.png"));
                write(hardness(r), new File(dir, "hardness.png"));
                write(sediment(r), new File(dir, "sediment.png"));
                write(change(r), new File(dir, "erosion_change.png"));
                write(coarse(pipe, r), new File(dir, "coarse.png"));
                sheet.add(overview);
                sheetLabels.add(label(r));
                if (r.water() != null && r.places() != null) {
                    for (String layer : MapRender.LAYERS) {
                        BufferedImage img = MapRender.layer(layer, r);
                        write(img, new File(dir, layer + ".png"));
                        if (layer.equals("m1")) sheetM1.add(img);
                        if (layer.equals("world")) sheetWorld.add(img);
                    }
                    System.out.println("  M1: " + MapRender.summary(r));
                    System.out.printf("  M1 hashes: water %08x, places %08x, m1 %08x%n", r.waterHash(), r.placesHash(), r.m1Hash());
                    for (String note : r.water().notes()) System.out.println("  note: " + note);
                    for (String note : r.places().notes()) System.out.println("  note: " + note);
                }
                System.out.println(dir.getPath() + "/overview.png  " + label(r) + "  " + statsLine(r));
                for (String f : r.earlierFailures()) System.out.println("  rerolled: " + f);
                for (String f : r.failures()) System.out.println("  FAILED: " + f);
            }
        }
        if (sheet.size() > 1) write(contactSheet(sheet, sheetLabels), new File(out, "sheet.png"));
        if (sheetM1.size() > 1) {
            write(MapRender.sheet(sheetM1, 4, 1024), new File(out, "sheet-m1.png"));
            write(MapRender.sheet(sheetWorld, 4, 768), new File(out, "sheet-world.png"));
            System.out.println(new File(out, "sheet-m1.png").getPath() + "  (M1 contact sheet)");
        }
    }

    static String label(Pipeline.Result r) {
        SeedStage.SeedParams s = r.seed();
        return "seed " + s.levelSeed() + " a" + s.attempt() + " " + s.archetype().path() + (s.mirror() ? " mirrored" : "");
    }

    static String statsLine(Pipeline.Result r) {
        Pipeline.Stats st = r.stats();
        return String.format("h %.0f-%.0f m, floor median %.1f m (%.0f-%.0f), landform %08x, height %08x, faces %d kept %.0f%%, mass %.4f",
                st.minM(), st.maxM(), st.floorMedianM(), st.floorMinM(), st.floorMaxM(), r.landformHash(),
                r.heightHash(), st.hardFaces(), 100 * st.hardFacesKept(), st.massBalance());
    }

    static void write(BufferedImage img, File f) throws IOException { ImageIO.write(img, "png", f); }

    // Hypsometric stops: valley floor, spruce slopes, benches, alpine, rock, snow.
    private static final double[] STOP_M = {120, 170, 260, 420, 600, 760, 900};
    private static final int[] STOP_RGB = {0x7d8a55, 0x627a43, 0x4d6b3c, 0x7c7a52, 0x8e8270, 0xa9a39b, 0xeeeeee};

    static int tint(double h) {
        if (h <= STOP_M[0]) return STOP_RGB[0];
        for (int i = 1; i < STOP_M.length; i++) {
            if (h <= STOP_M[i]) {
                double t = (h - STOP_M[i - 1]) / (STOP_M[i] - STOP_M[i - 1]);
                return mix(STOP_RGB[i - 1], STOP_RGB[i], t);
            }
        }
        return STOP_RGB[STOP_RGB.length - 1];
    }

    static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int scale(int rgb, double f) {
        int r = clamp((int) (((rgb >> 16) & 255) * f)), g = clamp((int) (((rgb >> 8) & 255) * f)), b = clamp((int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    /** Lambert hillshade with the light from the north-west at 45°, cell size 2 m. */
    static double shade(float[] h, int n, int x, int z, double cellM) {
        int xl = Math.max(0, x - 1), xr = Math.min(n - 1, x + 1), zu = Math.max(0, z - 1), zd = Math.min(n - 1, z + 1);
        double dx = (h[z * n + xr] - h[z * n + xl]) / ((xr - xl) * cellM);
        double dz = (h[zd * n + x] - h[zu * n + x]) / ((zd - zu) * cellM);
        double nx = -dx, ny = 1, nz = -dz;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double lx = -0.5, ly = 0.7071, lz = -0.5; // from the north-west, up
        double d = (nx * lx + ny * ly + nz * lz) / len;
        return 0.35 + 0.85 * Math.max(0, d);
    }

    static BufferedImage shaded(float[] h, int n, Graphics2DPainter extra, Pipeline.Result r) {
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
                double v = h[z * n + x];
                int c = scale(tint(v), shade(h, n, x, z, 2.0));
                // 50 m contours, 250 m index contours.
                if (x + 1 < n && z + 1 < n) {
                    int b0 = (int) Math.floor(v / 50), b1 = (int) Math.floor(h[z * n + x + 1] / 50), b2 = (int) Math.floor(h[(z + 1) * n + x] / 50);
                    if (b0 != b1 || b0 != b2) {
                        boolean index = Math.max(b0, Math.max(b1, b2)) % 5 == 0;
                        c = scale(c, index ? 0.55 : 0.8);
                    }
                }
                img.setRGB(x, z, c);
            }
        }
        if (extra != null) extra.paint(img);
        return img;
    }

    interface Graphics2DPainter { void paint(BufferedImage img); }

    static BufferedImage overview(Pipeline pipe, Pipeline.Result r) {
        int n = r.erosion().cells();
        float[] h = r.erosion().height();
        BufferedImage img = shaded(h, n, null, r);
        // Rock: hard cells steeper than 40°.
        double rise = Math.tan(Math.toRadians(40)) * 2.0;
        for (int z = 1; z < n - 1; z++) for (int x = 1; x < n - 1; x++) {
            int idx = z * n + x;
            if (r.landform().hardnessAt(idx) > 0.6) {
                double c = h[idx];
                double m = Math.max(Math.max(Math.abs(c - h[idx - 1]), Math.abs(c - h[idx + 1])),
                        Math.max(Math.abs(c - h[idx - n]), Math.abs(c - h[idx + n])));
                if (m > rise) img.setRGB(x, z, mix(img.getRGB(x, z) & 0xFFFFFF, 0x9a5a4a, 0.55));
            }
        }
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPath(g, r, 1.0);
        Landform.Shape sh = r.landform().shape();
        if (sh.basin() != null) {
            Landform.Shape.Basin b = sh.basin();
            double cx = r.landform().mirror() ? 2048 - b.centerX() : b.centerX();
            g.setColor(new Color(60, 110, 200, 160));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval((int) ((cx - b.lengthM() / 2) / 2), (int) ((b.centerZ() - b.widthM() / 2) / 2),
                    (int) (b.lengthM() / 2), (int) (b.widthM() / 2));
        }
        if (sh.canyon() != null) {
            Landform.Shape.Canyon c = sh.canyon();
            g.setColor(new Color(220, 80, 40));
            for (double at : c.stepAt()) {
                double x = r.landform().mirror() ? 2048 - at : at;
                double z = r.landform().path().zAt(at);
                g.fillOval((int) (x / 2) - 4, (int) (z / 2) - 4, 8, 8);
            }
        }
        text(g, new String[] {label(r), statsLine(r).replace(", landform", "\nlandform")}, 8, 18);
        g.dispose();
        return img;
    }

    static void drawPath(Graphics2D g, Pipeline.Result r, double s) {
        RiverPath p = r.landform().path();
        g.setColor(new Color(40, 90, 220, 200));
        g.setStroke(new BasicStroke(2f));
        for (int i = 1; i < p.corePointCount(); i++) {
            double x0 = p.coreX(i - 1), x1 = p.coreX(i);
            if (r.landform().mirror()) { x0 = 2048 - x0; x1 = 2048 - x1; }
            g.drawLine((int) (x0 / 2 * s), (int) (p.coreZ(i - 1) / 2 * s), (int) (x1 / 2 * s), (int) (p.coreZ(i) / 2 * s));
        }
    }

    static void text(Graphics2D g, String[] lines, int x, int y) {
        try {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            int yy = y;
            for (String block : lines) for (String line : block.split("\n")) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRect(x - 4, yy - 14, g.getFontMetrics().stringWidth(line) + 8, 18);
                g.setColor(Color.WHITE);
                g.drawString(line, x, yy);
                yy += 19;
            }
        } catch (Throwable t) {
            // Headless JVMs without fonts still get the map, just no caption.
        }
    }

    static BufferedImage hardness(Pipeline.Result r) {
        int n = r.landform().cells();
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < n * n; i++) {
            double v = r.landform().hardnessAt(i);
            int c = v > 0.6 ? mix(0x804030, 0xffd0a0, (v - 0.6) / 0.4) : mix(0x101010, 0x707070, v / 0.6);
            img.setRGB(i % n, i / n, c);
        }
        return img;
    }

    static BufferedImage sediment(Pipeline.Result r) {
        int n = r.erosion().cells();
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        byte[] s = r.erosion().sediment();
        for (int i = 0; i < n * n; i++) {
            double m = (s[i] & 0xFF) / 10.0;
            img.setRGB(i % n, i / n, mix(0x202020, 0xe8d090, Math.min(1, m / 4.0)));
        }
        return img;
    }

    static BufferedImage change(Pipeline.Result r) {
        int n = r.erosion().cells();
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        float[] a = r.landform().height(), b = r.erosion().height();
        for (int i = 0; i < n * n; i++) {
            double d = b[i] - a[i];
            int c = d < 0 ? mix(0x808080, 0xc02020, Math.min(1, -d / 2.0)) : mix(0x808080, 0x2040d0, Math.min(1, d / 2.0));
            img.setRGB(i % n, i / n, c);
        }
        return img;
    }

    static BufferedImage coarse(Pipeline pipe, Pipeline.Result r) {
        int cn = r.landform().coarseCells();
        float[] c = r.landform().coarse();
        int s = 2;
        BufferedImage img = new BufferedImage(cn * s, cn * s, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < cn; z++) for (int x = 0; x < cn; x++) {
            double v = c[z * cn + x];
            double hv = shade(c, cn, x, z, 32.0);
            int rgb = scale(v > 900 ? mix(tint(900), 0xffffff, Math.min(1, (v - 900) / 500)) : tint(v), hv);
            for (int dz = 0; dz < s; dz++) for (int dx = 0; dx < s; dx++) img.setRGB(x * s + dx, z * s + dz, rgb);
        }
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 255, 255, 120));
        for (int k = 1; k < 4; k++) { // the 16 region cells A1–D4
            g.drawLine(k * cn * s / 4, 0, k * cn * s / 4, cn * s);
            g.drawLine(0, k * cn * s / 4, cn * s, k * cn * s / 4);
        }
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(cn * s / 4, cn * s / 2, cn * s / 4, cn * s / 4); // B3, the core
        g.dispose();
        return img;
    }

    static BufferedImage contactSheet(List<BufferedImage> maps, List<String> labels) {
        int cols = Math.min(4, maps.size()), rows = (maps.size() + cols - 1) / cols, t = 512;
        BufferedImage img = new BufferedImage(cols * t, rows * t, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int i = 0; i < maps.size(); i++) {
            g.drawImage(maps.get(i), (i % cols) * t, (i / cols) * t, t, t, null);
        }
        g.dispose();
        return img;
    }
}
