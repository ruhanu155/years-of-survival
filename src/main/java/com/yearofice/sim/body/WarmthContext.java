package com.yearofice.sim.body;

import com.yearofice.sim.env.ShelterState;

/**
 * The rest of the feels-like inputs that are not in the environment sample or the clothes: the open
 * shelter the player is in, the minute's activity, and the body term (BMI band modifier plus the low-fat
 * winter penalty, worked out by the body model).
 */
public record WarmthContext(ShelterState shelter, Activity activity, double bodyModC) {

    public static final WarmthContext RESTING = new WarmthContext(ShelterState.NONE, Activity.REST, 0);

    public WarmthContext {
        if (shelter == null) shelter = ShelterState.NONE;
        if (activity == null) activity = Activity.REST;
    }

    public WarmthContext with(Activity a) { return new WarmthContext(shelter, a, bodyModC); }

    public WarmthContext with(ShelterState s) { return new WarmthContext(s, activity, bodyModC); }
}
