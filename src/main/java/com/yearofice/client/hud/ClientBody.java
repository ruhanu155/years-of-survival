package com.yearofice.client.hud;

import com.yearofice.body.net.BodySyncPayload;
import org.jetbrains.annotations.Nullable;

/**
 * The client's copy of its own vitals: the last {@code body_sync}. Client code reads only this; it never computes
 * a body number itself (server decides, client shows).
 */
public final class ClientBody {
    private ClientBody() {}

    private static volatile @Nullable BodySyncPayload latest;

    public static void accept(BodySyncPayload p) { latest = p; }

    public static @Nullable BodySyncPayload get() { return latest; }

    public static void clear() { latest = null; }

    /** Strip thresholds (Blueprint › What the HUD shows; Minecraft development › The vitals strip). */
    public static boolean warmthWarn(BodySyncPayload p) { return p.warmth() < 70; }

    public static boolean wetWarn(BodySyncPayload p) { return p.wetness() > 0.30f; }

    public static boolean waterWarn(BodySyncPayload p) { return p.water() < 60; }

    public static boolean caloriesWarn(BodySyncPayload p) { return p.balance24h() < -1000; }

    public static boolean energyWarn(BodySyncPayload p) { return p.energy() < 5; }

    public static boolean conditionWarn(BodySyncPayload p) { return p.condition() < 60; }

    /** "You need more food.": the 24 h balance below −1,000 kcal or BMI under 18.5. */
    public static boolean needFood(BodySyncPayload p) { return p.balance24h() < -1000 || p.bmi() < 18.5f; }
}
