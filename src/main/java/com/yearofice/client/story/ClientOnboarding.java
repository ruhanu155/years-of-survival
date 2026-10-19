package com.yearofice.client.story;

import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.story.net.OnboardingSyncPayload;
import org.jetbrains.annotations.Nullable;

/** The client's copy of its own guided-night state: the last {@code onboarding_sync}. Client code reads only this. */
public final class ClientOnboarding {
    private ClientOnboarding() {}

    private static volatile @Nullable OnboardingSyncPayload latest;

    public static void accept(OnboardingSyncPayload p) { latest = p; }

    public static @Nullable OnboardingSyncPayload get() { return latest; }

    public static void clear() { latest = null; }

    public static boolean active(@Nullable OnboardingSyncPayload p) {
        return p != null && p.status() == FirstNight.Status.ACTIVE.ordinal();
    }

    public static FirstNightStep step(OnboardingSyncPayload p) {
        FirstNightStep[] all = FirstNightStep.values();
        return all[Math.max(0, Math.min(all.length - 1, p.step()))];
    }
}
