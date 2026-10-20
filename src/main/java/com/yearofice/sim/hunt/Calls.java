package com.yearofice.sim.hunt;

/**
 * Calling animals (Blueprint › Setting up the hunt › Calling a bull; Water, calls and encounters › How every call works):
 * <pre>P_respond = p_s * S_s(date) * H_s(hour) * q * c_s * E_s * F(d / R_eff)</pre>
 * R_eff is the call's range times the wind, ground and air factors, capped at 1.5 times the range.
 */
public final class Calls {
    private Calls() {}

    public enum Falloff { LINEAR, FLAT }

    /** Moose rut factor: 0.3 from Sep 1-9, 1.0 from Sep 10 to Oct 5, 0.4 from Oct 6-20, else 0. */
    public static double rut(int month, int day) {
        if (month == 9) return day <= 9 ? 0.3 : 1.0;
        if (month == 10) return day <= 5 ? 1.0 : day <= 20 ? 0.4 : 0.0;
        return 0;
    }

    /** Wind between caller and listener: downwind 1.3, crosswind 1.0, upwind 0.6, from cos between wind-travel and caller-to-listener. */
    public static double windFactor(double cosWindToListener) {
        return cosWindToListener > 0.5 ? 1.3 : cosWindToListener < -0.5 ? 0.6 : 1.0;
    }

    public static double effectiveRange(double rangeM, double wind, double ground, double air) {
        return Math.min(1.5 * rangeM, rangeM * wind * ground * air);
    }

    public static double falloff(Falloff f, double d, double reff) {
        if (reff <= 0 || d > reff) return 0;
        return f == Falloff.FLAT ? 1.0 : 1.0 - d / reff;
    }

    /** Practice factor on quality: 0.8 for the first 10 uses, 1.0 to 29, 1.2 from 30. */
    public static double practice(int uses) { return uses < 10 ? 0.8 : uses < 30 ? 1.0 : 1.2; }

    public static double pRespond(double baseP, double season, double hour, double quality, boolean shy, double director,
                                  double distM, double reffM, Falloff f) {
        return clamp01(baseP * season * hour * quality * (shy ? 0.3 : 1.0) * director * falloff(f, distM, reffM));
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
