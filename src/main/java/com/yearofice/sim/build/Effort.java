package com.yearofice.sim.build;

import java.util.Locale;
import java.util.Optional;

/** A job's effort rate in kcal per hour (Crafting and building in depth › Recipes): light 100, medium 250, high 450. */
public enum Effort {
    LIGHT(100), MEDIUM(250), HIGH(450);

    public final double kcalPerHour;

    Effort(double kcalPerHour) { this.kcalPerHour = kcalPerHour; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Optional<Effort> byId(String id) {
        if (id == null) return Optional.empty();
        for (Effort e : values()) if (e.id().equals(id)) return Optional.of(e);
        return Optional.empty();
    }
}
