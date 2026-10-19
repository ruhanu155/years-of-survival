package com.yearofice.sim.ecosystem;

/**
 * A species' senses (Blueprint › Senses by species): the distance it sees a moving player, hears one walking and
 * smells one downwind, in metres, and its field of view in degrees (prey 300, predators 200).
 */
public record Senses(double sightM, double hearingM, double smellM, double fovDeg) {
    public Senses {
        if (sightM < 0 || hearingM < 0 || smellM < 0) throw new IllegalArgumentException("negative sense range");
        if (fovDeg <= 0 || fovDeg > 360) throw new IllegalArgumentException("fov must be in (0, 360]");
    }
}
