package com.yearofice.sim.body.part;

import com.yearofice.sim.body.Zone;
import java.util.Locale;

/**
 * The ten body parts (Body, injury and sickness › Parts, zones and shares). Declaration order is the
 * {@code part_hp} order in the schema-2 record: head, torso, left_arm, right_arm, left_hand, right_hand,
 * left_leg, right_leg, left_foot, right_foot.
 */
public enum Part {
    HEAD(Zone.HEAD, Group.HEAD, Side.NONE, 0.10, 10, "scalp", "face", "ear", "eye"),
    TORSO(Zone.TORSO, Group.TORSO, Side.NONE, 0.26, 10, "chest", "ribs", "back", "belly"),
    LEFT_ARM(Zone.TORSO, Group.ARMS, Side.LEFT, 0.07, 0, "upper_arm", "elbow", "forearm"),
    RIGHT_ARM(Zone.TORSO, Group.ARMS, Side.RIGHT, 0.07, 0, "upper_arm", "elbow", "forearm"),
    LEFT_HAND(Zone.HANDS, Group.HANDS, Side.LEFT, 0.05, 0, "wrist", "palm", "fingers", "thumb"),
    RIGHT_HAND(Zone.HANDS, Group.HANDS, Side.RIGHT, 0.05, 0, "wrist", "palm", "fingers", "thumb"),
    LEFT_LEG(Zone.LEGS, Group.LEGS, Side.LEFT, 0.125, 0, "thigh", "knee", "shin"),
    RIGHT_LEG(Zone.LEGS, Group.LEGS, Side.RIGHT, 0.125, 0, "thigh", "knee", "shin"),
    LEFT_FOOT(Zone.FEET, Group.FEET, Side.LEFT, 0.075, 0, "ankle", "heel", "toes"),
    RIGHT_FOOT(Zone.FEET, Group.FEET, Side.RIGHT, 0.075, 0, "ankle", "heel", "toes");

    /** Paired groups split a weight 50/50 between sides unless a row names a side. */
    public enum Group { HEAD, TORSO, ARMS, HANDS, LEGS, FEET }

    public static final int COUNT = 10;
    private static final Part[] VALUES = values();

    public final Zone zone;
    public final Group group;
    public final Side side;
    /** Share s of the body: its zone's share split by size. */
    public final double share;
    /** Lowest health: 10 for head and torso, so no single part causes a collapse. */
    public final int floor;
    private final String[] subs;

    Part(Zone zone, Group group, Side side, double share, int floor, String... subs) {
        this.zone = zone;
        this.group = group;
        this.side = side;
        this.share = share;
        this.floor = floor;
        this.subs = subs;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String[] subs() { return subs.clone(); }

    public int subCount() { return subs.length; }

    public String sub(int i) { return subs[i]; }

    public static Part of(Group g, Side s) {
        for (Part p : VALUES) if (p.group == g && (p.side == s || p.side == Side.NONE)) return p;
        throw new IllegalArgumentException(g + " " + s);
    }

    public static Part byId(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }

    public boolean isLimbOfGait() { return group == Group.LEGS || group == Group.FEET; }
}
