package com.yearofice.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Strict reader for worldgen data (RECONCILE 6): keys must be snake_case, every key must be read,
 * and a missing or unknown key fails loudly with its path. Pure Java with its own small parser (objects,
 * arrays, numbers, strings, booleans, null; duplicate keys rejected), so the stages need no library and run
 * in plain JUnit and the debug map tool.
 */
public final class GenJson {
    private static final Pattern SNAKE = Pattern.compile("[a-z][a-z0-9]*(_[a-z0-9]+)*");

    private static final Object NULL = new Object();

    private final Map<String, Object> obj;
    private final String path;
    private final TreeSet<String> read = new TreeSet<>();

    private GenJson(Map<String, Object> obj, String path) {
        this.obj = obj;
        this.path = path;
        for (String k : obj.keySet()) {
            if (!SNAKE.matcher(k).matches()) {
                throw new IllegalArgumentException(path + ": key '" + k + "' is not snake_case");
            }
        }
    }

    /** Reads a classpath resource such as {@code data/yearofice/worldgen_params/valley.json}. */
    public static GenJson resource(String resource) {
        ClassLoader cl = GenJson.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("missing worldgen data: " + resource);
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), resource);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + resource, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static GenJson parse(String json, String name) {
        Parser p = new Parser(json, name);
        Object root = p.value();
        p.end();
        if (!(root instanceof Map)) throw new IllegalArgumentException(name + ": root is not an object");
        return new GenJson((Map<String, Object>) root, name);
    }

    private Object need(String key) {
        Object e = obj.get(key);
        if (e == null || e == NULL) throw new IllegalArgumentException(path + ": missing '" + key + "'");
        read.add(key);
        return e;
    }

    public boolean has(String key) {
        Object e = obj.get(key);
        if (e != null) read.add(key);
        return e != null && e != NULL;
    }

    public double num(String key) {
        Object e = need(key);
        if (!(e instanceof Double d)) throw new IllegalArgumentException(path + ": '" + key + "' is not a number");
        return d;
    }

    public int integer(String key) {
        double d = num(key);
        if (d != Math.floor(d)) throw new IllegalArgumentException(path + ": '" + key + "' must be an integer");
        return (int) d;
    }

    public String str(String key) {
        Object e = need(key);
        if (!(e instanceof String v)) throw new IllegalArgumentException(path + ": '" + key + "' is not a string");
        return v;
    }

    @SuppressWarnings("unchecked")
    public GenJson obj(String key) {
        Object e = need(key);
        if (!(e instanceof Map)) throw new IllegalArgumentException(path + ": '" + key + "' is not an object");
        return new GenJson((Map<String, Object>) e, path + "." + key);
    }

    /** Reads {@code {"min": a, "max": b}} with a ≤ b. */
    public Range range(String key) {
        GenJson r = obj(key);
        Range out = new Range(r.num("min"), r.num("max"));
        r.done();
        if (out.min() > out.max()) throw new IllegalArgumentException(path + "." + key + ": min > max");
        return out;
    }

    public boolean bool(String key) {
        Object e = need(key);
        if (!(e instanceof Boolean b)) throw new IllegalArgumentException(path + ": '" + key + "' is not a boolean");
        return b;
    }

    public boolean bool(String key, boolean fallback) { return has(key) ? bool(key) : fallback; }

    public double num(String key, double fallback) { return has(key) ? num(key) : fallback; }

    public String str(String key, String fallback) { return has(key) ? str(key) : fallback; }

    private List<?> list(String key) {
        Object e = need(key);
        if (!(e instanceof List<?> l)) throw new IllegalArgumentException(path + ": '" + key + "' is not an array");
        return l;
    }

    /** An array of strings. */
    public List<String> strings(String key) {
        List<String> out = new ArrayList<>();
        int k = 0;
        for (Object o : list(key)) {
            if (!(o instanceof String v)) throw new IllegalArgumentException(path + "." + key + "[" + k + "] is not a string");
            out.add(v);
            k++;
        }
        return List.copyOf(out);
    }

    /** An array of numbers. */
    public double[] nums(String key) {
        List<?> l = list(key);
        double[] out = new double[l.size()];
        for (int k = 0; k < out.length; k++) {
            if (!(l.get(k) instanceof Double d)) throw new IllegalArgumentException(path + "." + key + "[" + k + "] is not a number");
            out[k] = d;
        }
        return out;
    }

    /** An array of objects, each strict like any other. */
    @SuppressWarnings("unchecked")
    public List<GenJson> objects(String key) {
        List<GenJson> out = new ArrayList<>();
        int k = 0;
        for (Object o : list(key)) {
            if (!(o instanceof Map)) throw new IllegalArgumentException(path + "." + key + "[" + k + "] is not an object");
            out.add(new GenJson((Map<String, Object>) o, path + "." + key + "[" + k + "]"));
            k++;
        }
        return List.copyOf(out);
    }

    /** The object's keys in file order; every key counts as read. */
    public List<String> keys() {
        read.addAll(obj.keySet());
        return List.copyOf(obj.keySet());
    }

    public String path() { return path; }

    /** Fails if the object holds a key nobody read. Call once every field is read. */
    public void done() {
        List<String> unknown = new ArrayList<>();
        for (String k : new TreeSet<>(obj.keySet())) if (!read.contains(k)) unknown.add(k);
        if (!unknown.isEmpty()) throw new IllegalArgumentException(path + ": unknown keys " + unknown);
    }

    /** Recursive-descent JSON parser (RFC 8259), strict: no comments, no trailing commas. */
    private static final class Parser {
        private final String s;
        private final String name;
        private int i;

        Parser(String s, String name) { this.s = s; this.name = name; }

        private IllegalArgumentException err(String what) {
            return new IllegalArgumentException(name + ": " + what + " at offset " + i);
        }

        private void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++; else break;
            }
        }

        void end() {
            ws();
            if (i != s.length()) throw err("trailing content");
        }

        Object value() {
            ws();
            if (i >= s.length()) throw err("unexpected end");
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return NULL; }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                if (i >= s.length() || s.charAt(i) != '"') throw err("expected key");
                String k = string();
                ws();
                if (i >= s.length() || s.charAt(i) != ':') throw err("expected ':'");
                i++;
                if (m.put(k, value()) != null) throw err("duplicate key '" + k + "'");
                ws();
                if (i >= s.length()) throw err("unclosed object");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw err("expected ',' or '}'");
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(value());
                ws();
                if (i >= s.length()) throw err("unclosed array");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw err("expected ',' or ']'");
            }
        }

        private String string() {
            StringBuilder b = new StringBuilder();
            i++;
            while (true) {
                if (i >= s.length()) throw err("unclosed string");
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                if (i >= s.length()) throw err("bad escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> b.append(e);
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw err("bad unicode escape");
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw err("bad escape");
                }
            }
        }

        private Double number() {
            int start = i;
            if (i < s.length() && s.charAt(i) == '-') i++;
            while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw err("unexpected character '" + s.charAt(i) + "'");
            try {
                return Double.parseDouble(s.substring(start, i)); // exactly rounded on every JVM
            } catch (NumberFormatException e) {
                throw err("bad number");
            }
        }
    }

    /** A closed numeric range drawn uniformly. */
    public record Range(double min, double max) {
        public double draw(com.yearofice.core.hash.Mulberry32 rng) { return min + (max - min) * rng.nextDouble(); }

        /** Uniform integer in [min, max] inclusive. */
        public int drawInt(com.yearofice.core.hash.Mulberry32 rng) {
            int lo = (int) min, hi = (int) max;
            return lo + rng.nextInt(hi - lo + 1);
        }

        public boolean contains(double v) { return v >= min && v <= max; }
    }
}
