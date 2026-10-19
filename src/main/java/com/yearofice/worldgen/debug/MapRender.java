package com.yearofice.worldgen.debug;

import com.yearofice.worldgen.CoarseRivers;
import com.yearofice.worldgen.Landmarks;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.WaterStage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Debug-map layers for M1 (water, current, fishing spots, rock, set pieces, the 8 km world), drawn with AWT only.
 * Shared by {@code tools/debugmap} and {@code /yoi worldgen map}; it reads a finished {@link Pipeline.Result} and
 * never runs a stage. The core is 1 px = 2 m, north up.
 */
public final class MapRender {
    private MapRender() {}

    public static final String[] LAYERS = {"m1", "water", "current", "spots", "rock", "places", "world"};

    public static BufferedImage layer(String name, Pipeline.Result r) {
        return switch (name) {
            case "m1" -> m1(r);
            case "water" -> water(r, true);
            case "current" -> current(r);
            case "spots" -> spots(r, true);
            case "rock" -> rock(r);
            case "places" -> places(r, true);
            case "world" -> world(r);
            default -> throw new IllegalArgumentException("no layer " + name + "; layers: " + String.join(", ", LAYERS));
        };
    }

    // ------------------------------------------------------------------------------------------- palette

    private static final double[] STOP_M = {120, 170, 260, 420, 600, 760, 900};
    private static final int[] STOP_RGB = {0x7d8a55, 0x627a43, 0x4d6b3c, 0x7c7a52, 0x8e8270, 0xa9a39b, 0xeeeeee};

    static int tint(double h) {
        if (h <= STOP_M[0]) return STOP_RGB[0];
        for (int i = 1; i < STOP_M.length; i++) {
            if (h <= STOP_M[i]) return mix(STOP_RGB[i - 1], STOP_RGB[i], (h - STOP_M[i - 1]) / (STOP_M[i] - STOP_M[i - 1]));
        }
        return STOP_RGB[STOP_RGB.length - 1];
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
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

    static double shade(float[] h, int n, int x, int z, double cellM) {
        int xl = Math.max(0, x - 1), xr = Math.min(n - 1, x + 1), zu = Math.max(0, z - 1), zd = Math.min(n - 1, z + 1);
        double dx = (h[z * n + xr] - h[z * n + xl]) / ((xr - xl) * cellM);
        double dz = (h[zd * n + x] - h[zu * n + x]) / ((zd - zu) * cellM);
        double nx = -dx, ny = 1, nz = -dz;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double d = (nx * -0.5 + ny * 0.7071 + nz * -0.5) / len;
        return 0.35 + 0.85 * Math.max(0, d);
    }

    static final int MAIN = 0x2a6fdb, CREEK = 0x4f9be0, LAKE = 0x1f4e9a, OXBOW = 0x2f7f8f, POOL = 0x00c8ff;
    static final int BAR = 0xd8c79c, CUT = 0x9c4a22, BOG = 0x5d6e3a, SHELF = 0x3f7fc0, ROCK = 0x9a5a4a, IRON = 0xff7a00,
            RUST = 0xc0602a;

    /** Hillshade with tint and 50 m contours over the carved ground; {@code dim} fades it for overlays. */
    static BufferedImage base(Pipeline.Result r, double dim) {
        float[] h = r.ground();
        int n = r.erosion().cells();
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            double v = h[z * n + x];
            int c = scale(tint(v), shade(h, n, x, z, 2.0));
            if (x + 1 < n && z + 1 < n) {
                int b0 = (int) Math.floor(v / 50), b1 = (int) Math.floor(h[z * n + x + 1] / 50), b2 = (int) Math.floor(h[(z + 1) * n + x] / 50);
                if (b0 != b1 || b0 != b2) c = scale(c, Math.max(b0, Math.max(b1, b2)) % 5 == 0 ? 0.6 : 0.82);
            }
            if (dim < 1) c = mix(0xf0f0f0, c, dim);
            img.setRGB(x, z, c);
        }
        return img;
    }

    static void paintWater(BufferedImage img, Pipeline.Result r, boolean banks) {
        WaterStage.Layers w = r.water();
        int n = w.cells();
        float[] eroded = r.erosion().height();
        for (int i = 0; i < n * n; i++) {
            int x = i % n, z = i / n;
            int base = img.getRGB(x, z) & 0xFFFFFF;
            int t = w.type()[i];
            if (t != WaterStage.DRY) {
                int c = switch (t) {
                    case WaterStage.MAIN -> MAIN;
                    case WaterStage.CREEK -> CREEK;
                    case WaterStage.LAKE -> LAKE;
                    case WaterStage.OXBOW -> OXBOW;
                    default -> POOL;
                };
                if (t == WaterStage.LAKE || t == WaterStage.OXBOW) {
                    double d = w.depth(eroded, i);
                    c = mix(0x6fa8e0, c, Math.min(1, d / 8.0));
                    if (banks && w.bank()[i] == WaterStage.WEED_SHELF) c = mix(c, 0x5fae6f, 0.35);
                    if (banks && w.bank()[i] == WaterStage.DROP_OFF) c = mix(c, 0x0a1f50, 0.5);
                }
                img.setRGB(x, z, c);
            } else if (banks) {
                int b = w.bank()[i];
                if (b == WaterStage.BAR) img.setRGB(x, z, mix(base, BAR, 0.8));
                else if (b == WaterStage.CUT_BANK) img.setRGB(x, z, CUT);
                else if (b == WaterStage.BOG) img.setRGB(x, z, ((x + z) % 4 == 0) ? 0x3a4a22 : mix(base, BOG, 0.55));
            }
        }
    }

    static void paintRock(BufferedImage img, Pipeline.Result r) {
        WaterStage.Layers w = r.water();
        int n = w.cells();
        for (int i = 0; i < n * n; i++) {
            int b = w.rock()[i];
            if (b == 0) continue;
            int x = i % n, z = i / n;
            int base = img.getRGB(x, z) & 0xFFFFFF;
            img.setRGB(x, z, b == WaterStage.FACE_ROCK ? mix(base, ROCK, 0.55) : b == WaterStage.IRON_OUTCROP ? IRON : mix(base, RUST, 0.8));
        }
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static int px(Pipeline.Result r, int cell) { return cell % r.water().cells(); }

    static int pz(Pipeline.Result r, int cell) { return cell / r.water().cells(); }

    static void falls(Graphics2D g, Pipeline.Result r) {
        for (WaterStage.Fall f : r.water().falls()) {
            int x = px(r, f.foot()), z = pz(r, f.foot());
            int[] xs = {x, x - 7, x + 7}, zs = {z - 8, z + 5, z + 5};
            g.setColor(f.guaranteed() ? new Color(255, 160, 0) : new Color(230, 30, 30));
            g.fillPolygon(xs, zs, 3);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1f));
            g.drawPolygon(xs, zs, 3);
        }
    }

    static void crossings(Graphics2D g, Pipeline.Result r) {
        CoarseRivers.Net net = r.water().coarse();
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(3f));
        for (CoarseRivers.Crossing c : List.of(net.entry(), net.exit())) {
            int x = (int) (c.lx() / 2), z = (int) (c.lz() / 2);
            g.drawOval(x - 9, z - 9, 18, 18);
        }
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 0, 200));
        for (CoarseRivers.Crossing c : net.inflows()) {
            int x = (int) (c.lx() / 2), z = (int) (c.lz() / 2);
            g.drawRect(x - 5, z - 5, 10, 10);
        }
    }

    static void text(Graphics2D g, List<String> lines, int x, int y, int size) {
        try {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            int yy = y;
            for (String line : lines) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRect(x - 4, yy - size, g.getFontMetrics().stringWidth(line) + 8, size + 5);
                g.setColor(Color.WHITE);
                g.drawString(line, x, yy);
                yy += size + 5;
            }
        } catch (Throwable t) {
            // Headless JVMs without fonts still get the map.
        }
    }

    static void label(Graphics2D g, String s, int x, int y, Color fg, int size) {
        try {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            int w = g.getFontMetrics().stringWidth(s);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(x - 2, y - size + 1, w + 4, size + 3);
            g.setColor(fg);
            g.drawString(s, x, y);
        } catch (Throwable t) {
            // no fonts
        }
    }

    public static String caption(Pipeline.Result r) {
        return "seed " + r.seed().levelSeed() + " a" + r.attempt() + " " + r.seed().archetype().path()
                + (r.seed().mirror() ? " mirrored" : "");
    }

    /** One-line M1 summary: the numbers the M1 tests check. */
    public static String summary(Pipeline.Result r) {
        WaterStage.Layers w = r.water();
        int majors = 0;
        for (WaterStage.Spot s : r.spots()) if (s.major()) majors++;
        double bigLake = 0;
        for (WaterStage.Lake l : w.lakes()) bigLake = Math.max(bigLake, l.areaM2());
        long passing = r.places().landmarks().stream().filter(l -> l.visiblePoints() >= 3).count();
        return String.format(Locale.ROOT,
                "river %.0f m, %d channel(s), %d oxbow lake(s) | %d creeks | %d lakes (largest %.3f km2) | %d falls | %d spots (%d major) | %d shelters | %d iron | %d set pieces | landmarks %d/16",
                w.main().lengthM(), w.main().lines().size(), w.oxbowLakes(), w.creeks().size(), w.lakes().size(), bigLake / 1e6,
                w.falls().size(), r.spots().size(), majors, w.shelters().size(), w.iron().size(), r.places().placements().size(),
                passing);
    }

    // ---------------------------------------------------------------------------------------- the layers

    static BufferedImage m1(Pipeline.Result r) {
        BufferedImage img = base(r, 1.0);
        paintRock(img, r);
        paintWater(img, r, true);
        Graphics2D g = gfx(img);
        crossings(g, r);
        falls(g, r);
        drawSpots(g, r, false);
        drawPlaces(g, r, false);
        text(g, List.of(caption(r), summary(r)), 8, 20, 14);
        g.dispose();
        return img;
    }

    static BufferedImage water(Pipeline.Result r, boolean caption) {
        BufferedImage img = base(r, 0.8);
        paintWater(img, r, true);
        Graphics2D g = gfx(img);
        crossings(g, r);
        falls(g, r);
        // Oxbows as drawn by the channel stage.
        g.setColor(new Color(0, 255, 200));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {4, 4}, 0));
        for (var ox : r.water().main().oxbows()) {
            for (int i = 1; i < ox.size(); i++) {
                g.drawLine((int) (ox.x()[i - 1] / 2), (int) (ox.z()[i - 1] / 2), (int) (ox.x()[i] / 2), (int) (ox.z()[i] / 2));
            }
        }
        if (caption) text(g, List.of(caption(r) + " | water", "main river blue, creeks light blue, lakes dark, oxbows teal, pools cyan;",
                "bars sand, cut banks brown, bog hatched; red triangles falls (orange: guaranteed); yellow rings coarse crossings"), 8, 20, 14);
        g.dispose();
        return img;
    }

    static BufferedImage current(Pipeline.Result r) {
        BufferedImage img = base(r, 0.5);
        WaterStage.Layers w = r.water();
        int n = w.cells();
        for (int i = 0; i < n * n; i++) {
            if (w.type()[i] == WaterStage.DRY) continue;
            double v = w.speedMS(i);
            int c = (w.flowDir()[i] & 0xFF) == WaterStage.STILL ? 0x203060 : mix(mix(0x2050ff, 0x20e0a0, v / 1.2), 0xff3020, (v - 1.2) / 1.8);
            img.setRGB(i % n, i / n, c);
        }
        Graphics2D g = gfx(img);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.2f));
        for (int z = 4; z < n; z += 12) for (int x = 4; x < n; x += 12) {
            int i = z * n + x;
            int d = w.flowDir()[i] & 0xFF;
            if (w.type()[i] == WaterStage.DRY || d == WaterStage.STILL) continue;
            double a = d * 2 * Math.PI / 64;
            double len = 3 + 3 * w.speedMS(i);
            int ex = (int) (x + StrictMath.cos(a) * len), ez = (int) (z + StrictMath.sin(a) * len);
            g.drawLine(x, z, ex, ez);
            g.fillOval(ex - 1, ez - 1, 3, 3);
        }
        text(g, List.of(caption(r) + " | current: blue slow, green 1.2 m/s (wading limit), red 3 m/s; dark still"), 8, 20, 14);
        g.dispose();
        return img;
    }

    static Color spotColor(String type) {
        return switch (type) {
            case "pool_below_falls" -> new Color(255, 40, 40);
            case "lake_inlet", "lake_outlet" -> new Color(255, 140, 0);
            case "creek_mouth" -> new Color(255, 230, 0);
            case "lake_drop_off" -> new Color(160, 0, 200);
            case "outside_bend" -> new Color(0, 220, 120);
            case "weed_shelf" -> new Color(120, 200, 60);
            case "beaver_pond" -> new Color(150, 90, 40);
            case "spring_lead" -> new Color(0, 230, 255);
            default -> new Color(220, 220, 220);
        };
    }

    static void drawSpots(Graphics2D g, Pipeline.Result r, boolean labels) {
        List<WaterStage.Spot> all = new ArrayList<>(r.spots());
        all.sort((a, b) -> Double.compare(b.q(), a.q()));
        g.setStroke(new BasicStroke(1.5f));
        int rank = 0;
        for (WaterStage.Spot s : all) {
            int x = px(r, s.cell()), z = pz(r, s.cell());
            int rad = (int) (3 + 7 * s.q());
            g.setColor(spotColor(s.type()));
            if (s.major()) g.fillOval(x - rad, z - rad, 2 * rad, 2 * rad);
            else g.drawOval(x - rad, z - rad, 2 * rad, 2 * rad);
            g.setColor(Color.BLACK);
            if (s.major()) g.drawOval(x - rad, z - rad, 2 * rad, 2 * rad);
            if (s.major() && rank < 10) {
                rank++;
                label(g, Integer.toString(rank), x + rad + 1, z + 4, Color.WHITE, 12);
            }
        }
    }

    static BufferedImage spots(Pipeline.Result r, boolean caption) {
        BufferedImage img = base(r, 0.55);
        paintWater(img, r, false);
        Graphics2D g = gfx(img);
        drawSpots(g, r, true);
        if (caption) {
            List<String> lines = new ArrayList<>();
            lines.add(caption(r) + " | fishing spots, size by quality, filled = major; 1-10 the generator's top 10");
            lines.add("red pool below falls, orange lake inlet/outlet, yellow creek mouth, purple drop-off, green bend,");
            lines.add("lime weed shelf, brown beaver pond, cyan spring lead; rings: seams and deep holes");
            List<WaterStage.Spot> all = new ArrayList<>(r.spots());
            all.removeIf(s -> !s.major());
            all.sort((a, b) -> Double.compare(b.q(), a.q()));
            for (int i = 0; i < Math.min(10, all.size()); i++) {
                WaterStage.Spot s = all.get(i);
                lines.add(String.format(Locale.ROOT, "%d. %s q %.2f at %.0f, %.0f, depth %.1f m", i + 1, s.type(), s.q(), s.x(), s.z(), s.depthM()));
            }
            text(g, lines, 8, 20, 13);
        }
        g.dispose();
        return img;
    }

    static BufferedImage rock(Pipeline.Result r) {
        BufferedImage img = base(r, 0.75);
        paintWater(img, r, false);
        paintRock(img, r);
        Graphics2D g = gfx(img);
        for (WaterStage.Face f : r.water().faces()) {
            if (f.rank() == 0) continue;
            int x = px(r, f.foot()), z = pz(r, f.foot());
            g.setColor(Color.BLACK);
            g.fillRect(x - 4, z - 4, 9, 9);
            label(g, Integer.toString(f.rank()), x + 6, z + 4, Color.WHITE, 11);
        }
        g.setColor(new Color(255, 120, 0));
        g.setStroke(new BasicStroke(2f));
        for (WaterStage.IronCluster c : r.water().iron()) {
            int x = px(r, c.centre()), z = pz(r, c.centre());
            g.drawOval(x - 7, z - 7, 14, 14);
        }
        text(g, List.of(caption(r) + " | rock: face_rock (red-brown), iron_outcrop clusters (orange rings), rust stain;",
                r.water().shelters().size() + " shelter faces over 6 m, top 20 numbered (height x aspect, south first)"), 8, 20, 14);
        g.dispose();
        return img;
    }

    static void drawPlaces(Graphics2D g, Pipeline.Result r, boolean detail) {
        int n = r.water().cells();
        double ox = -2048, oz = 0; // core origin, as in valley.json
        for (SetPieces.Placement q : r.places().placements()) {
            if (q.coarse()) continue;
            double cx = (q.x() - ox) / 2, cz = (q.z() - oz) / 2;
            int w = (int) Math.ceil(q.footprintX() / 2.0), d = (int) Math.ceil(q.footprintZ() / 2.0);
            g.setColor(new Color(255, 0, 200));
            g.setStroke(new BasicStroke(2f));
            g.drawRect((int) (cx - w / 2.0) - 2, (int) (cz - d / 2.0) - 2, w + 4, d + 4);
            // Door direction.
            double a = Math.toRadians(q.facingDeg());
            g.drawLine((int) cx, (int) cz, (int) (cx + StrictMath.sin(a) * 14), (int) (cz - StrictMath.cos(a) * 14));
            String name = q.id().substring(10) + (detail ? "/" + q.variant() + " (" + q.site() + ")" : "");
            if (detail && !Double.isNaN(r.places().fromStart(q)) && !q.id().equals("yearofice:outpost")) {
                name += String.format(Locale.ROOT, " %.0f m", r.places().fromStart(q));
            }
            label(g, name, (int) cx + w / 2 + 6, (int) cz + 4, new Color(255, 170, 240), detail ? 13 : 12);
        }
        for (Landmarks.Landmark l : r.places().landmarks()) {
            double cx = (l.x() - ox) / 2, cz = (l.z() - oz) / 2;
            if (cx < 0 || cz < 0 || cx >= n || cz >= n) continue;
            star(g, (int) cx, (int) cz, l.visiblePoints() >= 3 ? new Color(255, 255, 255) : Color.RED);
            if (detail) label(g, l.kind() + " " + l.visiblePoints() + "/" + l.pointsInWorld(), (int) cx + 9, (int) cz - 6, Color.WHITE, 12);
        }
    }

    static void star(Graphics2D g, int x, int z, Color c) {
        int[] xs = new int[10], zs = new int[10];
        for (int k = 0; k < 10; k++) {
            double a = Math.PI / 5 * k - Math.PI / 2, rad = k % 2 == 0 ? 9 : 4;
            xs[k] = (int) (x + StrictMath.cos(a) * rad);
            zs[k] = (int) (z + StrictMath.sin(a) * rad);
        }
        g.setColor(c);
        g.fillPolygon(xs, zs, 10);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawPolygon(xs, zs, 10);
    }

    static BufferedImage places(Pipeline.Result r, boolean caption) {
        BufferedImage img = base(r, 0.8);
        paintWater(img, r, true);
        Graphics2D g = gfx(img);
        drawPlaces(g, r, true);
        if (caption) {
            List<String> lines = new ArrayList<>();
            lines.add(caption(r) + " | set pieces (footprint, door line) and landmarks (stars, points seen of 8)");
            for (SetPieces.Placement q : r.places().placements()) {
                lines.add(String.format(Locale.ROOT, "%s #%d %s on %s%s, %s, y %.0f%s%s", q.id().substring(10), q.instance(), q.variant(),
                        q.site(), q.coarse() ? " (coarse, " + com.yearofice.worldgen.SeedStage.regionCode(q.region()) + ")" : "",
                        q.rotationName(), q.y(), Double.isNaN(r.places().fromStart(q)) ? "" : String.format(Locale.ROOT, ", %.0f m from start", r.places().fromStart(q)),
                        q.relaxed() > 0 ? " RELAXED " + q.relaxed() : ""));
            }
            text(g, lines, 8, 20, 13);
        }
        g.dispose();
        return img;
    }

    /** The 8 km home valley at 32 m: coarse rivers, regions, landmarks and every set piece. */
    static BufferedImage world(Pipeline.Result r) {
        int cn = r.landform().coarseCells();
        float[] c = r.landform().coarse();
        int s = 3;
        BufferedImage img = new BufferedImage(cn * s, cn * s, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < cn; z++) for (int x = 0; x < cn; x++) {
            double v = c[z * cn + x];
            int rgb = scale(v > 900 ? mix(tint(900), 0xffffff, (v - 900) / 500) : tint(v), shade(c, cn, x, z, 32.0));
            for (int dz = 0; dz < s; dz++) for (int dx = 0; dx < s; dx++) img.setRGB(x * s + dx, z * s + dz, rgb);
        }
        CoarseRivers.Net net = r.water().coarse();
        Graphics2D g = gfx(img);
        for (int k = 0; k < cn * cn; k++) {
            double a = net.catchment()[k];
            if (a < 2e5 || net.receiver()[k] < 0) continue;
            int rcv = net.receiver()[k];
            g.setColor(new Color(40, 100, 230));
            g.setStroke(new BasicStroke((float) Math.min(4, 0.6 + StrictMath.log10(a / 2e5))));
            g.drawLine((k % cn) * s + 1, (k / cn) * s + 1, (rcv % cn) * s + 1, (rcv / cn) * s + 1);
        }
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(1f));
        for (int k = 1; k < 4; k++) {
            g.drawLine(k * cn * s / 4, 0, k * cn * s / 4, cn * s);
            g.drawLine(0, k * cn * s / 4, cn * s, k * cn * s / 4);
        }
        for (int reg = 0; reg < 16; reg++) {
            label(g, com.yearofice.worldgen.SeedStage.regionCode(reg) + " " + r.seed().regionRoles().get(reg).name().toLowerCase(Locale.ROOT),
                    (reg % 4) * cn * s / 4 + 4, (reg / 4) * cn * s / 4 + 14, Color.WHITE, 12);
        }
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(cn * s / 4, cn * s / 2, cn * s / 4, cn * s / 4);
        double half = 4096;
        for (Landmarks.Landmark l : r.places().landmarks()) {
            int x = (int) ((l.x() + half) / 32 * s), z = (int) ((l.z() + half) / 32 * s);
            star(g, x, z, l.visiblePoints() >= 3 ? Color.WHITE : Color.RED);
            label(g, l.kind() + " " + l.visiblePoints() + "/" + l.pointsInWorld(), x + 9, z + 4, Color.WHITE, 11);
        }
        for (SetPieces.Placement q : r.places().placements()) {
            int x = (int) ((q.x() + half) / 32 * s), z = (int) ((q.z() + half) / 32 * s);
            g.setColor(new Color(255, 0, 200));
            g.fillRect(x - 4, z - 4, 8, 8);
            label(g, q.id().substring(10), x + 6, z + 14, new Color(255, 170, 240), 11);
        }
        text(g, List.of(caption(r) + " | the 8 km home valley at 32 m: coarse rivers over 0.2 km2, regions and roles, landmarks, set pieces"), 8, cn * s - 10, 13);
        g.dispose();
        return img;
    }

    /** Tiles images into a sheet, {@code cols} wide, each scaled to {@code tile} px. */
    public static BufferedImage sheet(List<BufferedImage> maps, int cols, int tile) {
        cols = Math.min(cols, maps.size());
        int rows = (maps.size() + cols - 1) / cols;
        BufferedImage img = new BufferedImage(cols * tile, rows * tile, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (int i = 0; i < maps.size(); i++) g.drawImage(maps.get(i), (i % cols) * tile, (i / cols) * tile, tile, tile, null);
        g.dispose();
        return img;
    }
}
