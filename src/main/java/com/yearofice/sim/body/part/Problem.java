package com.yearofice.sim.body.part;

/**
 * One injury or illness problem in the record's {@code problems} list (at most 16). Field names follow the
 * schema-2 codec: the schema-1 fields {@code id, part, stage, timer, dressed, infected} plus {@code sub, pad,
 * binding, clean_until, splint, sling, wrap, cooled, started, next_mark} from the Body tab. Times are game
 * minutes; −1 means "not set".
 *
 * @param splint game minute the splint (or rib wrap) went on, or −1
 * @param wrap   game minute the sprain wrap went on, or −1 (the sprain timer runs only once wrapped)
 */
public record Problem(String id, Part part, String sub, int stage, int timer, boolean dressed, boolean infected,
                      String pad, String binding, long cleanUntil, long splint, boolean sling, long wrap,
                      boolean cooled, long started, long nextMark) {

    public Problem {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id");
        if (part == null) throw new IllegalArgumentException("part");
        if (sub == null) sub = "";
        if (pad == null) pad = "";
        if (binding == null) binding = "";
    }

    public static Problem fresh(String id, Part part, String sub, int stage, long now) {
        return new Problem(id, part, sub, stage, 0, false, false, "", "", -1, -1, false, -1, false, now, -1);
    }

    public Problem dressed(String pad, String binding, long cleanUntil) {
        return new Problem(id, part, sub, stage, timer, true, infected, pad, binding, cleanUntil, splint, sling, wrap,
                cooled, started, nextMark);
    }

    public Problem splinted(long now) {
        return new Problem(id, part, sub, stage, 0, dressed, infected, pad, binding, cleanUntil, now, sling, wrap,
                cooled, started, nextMark);
    }

    public Problem withSling(boolean on) {
        return new Problem(id, part, sub, stage, timer, dressed, infected, pad, binding, cleanUntil, splint, on, wrap,
                cooled, started, nextMark);
    }

    public Problem wrapped(long now) {
        return new Problem(id, part, sub, stage, timer, dressed, infected, pad, binding, cleanUntil, splint, sling, now,
                cooled, started, nextMark);
    }

    public Problem withStage(int s) {
        return new Problem(id, part, sub, s, timer, dressed, infected, pad, binding, cleanUntil, splint, sling, wrap,
                cooled, started, nextMark);
    }

    public Problem withTimer(int t, long started, long nextMark) {
        return new Problem(id, part, sub, stage, t, dressed, infected, pad, binding, cleanUntil, splint, sling, wrap,
                cooled, started, nextMark);
    }

    public boolean isSplinted() { return splint >= 0; }

    /** Condition lost a game minute to bleeding: an undressed cut bleeds 1 (minor) or 3 (deep). */
    public double bleedPerMinute() {
        if (!Problems.CUT.equals(id) || dressed) return 0;
        return stage >= 2 ? 3 : 1;
    }
}
