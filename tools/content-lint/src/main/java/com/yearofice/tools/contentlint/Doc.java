package com.yearofice.tools.contentlint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads sections of the blueprint's Markdown exports. */
final class Doc {
    private static final Pattern HEADING = Pattern.compile("^(#+)\\s+(.*?)\\s*$");
    private static final Pattern BACKTICK = Pattern.compile("`([^`]+)`");
    private static final Pattern PLAIN_ID = Pattern.compile("yearofice:([a-z0-9_./]+)");
    static final Pattern ID = Pattern.compile("[a-z0-9_.]+");

    private Doc() {}

    static List<String> lines(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        for (String l : Files.readAllLines(file, StandardCharsets.UTF_8)) out.add(l.replace("\\_", "_"));
        return out;
    }

    /** The lines under the first heading whose text is {@code heading}, up to the next heading of the same or a higher level. */
    static List<String> section(List<String> lines, String heading, String where) {
        int start = -1, level = 0;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = HEADING.matcher(lines.get(i));
            if (m.matches() && m.group(2).equals(heading)) { start = i + 1; level = m.group(1).length(); break; }
        }
        if (start < 0) throw new IllegalStateException("heading '" + heading + "' not found in " + where);
        List<String> out = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            Matcher m = HEADING.matcher(lines.get(i));
            if (m.matches() && m.group(1).length() <= level && !inFence(lines, i)) break;
            out.add(lines.get(i));
        }
        return out;
    }

    /** True when line i sits inside a ``` fence (a "# comment" in a code block is not a heading). */
    private static boolean inFence(List<String> lines, int i) {
        boolean in = false;
        for (int j = 0; j < i; j++) if (lines.get(j).startsWith("```")) in = !in;
        return in;
    }

    /** Ids named in the text: backticked words (namespace optional) and plain yearofice: ids. */
    static Set<String> ids(List<String> text) {
        Set<String> out = new LinkedHashSet<>();
        for (String l : text) {
            Matcher b = BACKTICK.matcher(l);
            while (b.find()) {
                String w = b.group(1).trim();
                if (w.startsWith("#")) w = w.substring(1);
                if (w.startsWith("yearofice:")) w = w.substring("yearofice:".length());
                if (ID.matcher(w).matches()) out.add(w);
            }
            Matcher p = PLAIN_ID.matcher(l);
            while (p.find()) out.add(p.group(1));
        }
        return out;
    }

    /** Raw backticked words, for checking that every word in a passage is accounted for. */
    static List<String> backticked(String line) {
        List<String> out = new ArrayList<>();
        Matcher b = BACKTICK.matcher(line);
        while (b.find()) out.add(b.group(1).trim());
        return out;
    }
}
