package com.yearofice.sim.env;

/**
 * The Environment field's answer to "how cold, wet, windy, snowy, icy and bright is it here?"
 * (Minecraft development › Layer 2, plus the fields RECONCILE 138 approved).
 *
 * <p>This is the pure-Java shape the sim cores read. DV1 puts the shared record in {@code api};
 * until the Studio lead lands it there, this record is the contract, and {@code api.EnvSample}
 * should either be this record or convert to it one field to one field. Units follow the field
 * table: °C, m/s, cm, mm/h, metres. {@code airC} is already the room temperature when
 * {@code roomId != 0}.
 */
public record EnvSample(
        double airC,
        double feelsLikeC,
        double groundC,
        double waterC,
        double moisture,
        double windDirDeg,
        double windMs,
        double gustMs,
        double snowCm,
        boolean crust,
        double iceCm,
        Precip precip,
        double precipMmH,
        double visibilityM,
        int light,
        double heatBonusC,
        int fireDanger,
        int roomId,
        // RECONCILE 138
        double cloudPct,
        double pressureHpa,
        double pressureTrend,
        double humidityPct,
        String airMass,
        double glazeMm,
        double snowIceMm,
        double smoke) {

    public EnvSample {
        if (precip == null) precip = Precip.NONE;
        if (airMass == null) airMass = "";
    }

    /** Wind chill at 1.5 m before clothing, as the env layer fills {@code feels_like_c}: 0.8 °C per m/s, at most 16, only below 10 °C. */
    public static double envFeelsLike(double airC, double windMs) {
        return airC - (airC < 10 ? Math.min(16.0, 0.8 * Math.max(0, windMs)) : 0);
    }

    /** A dry, clear, outdoor sample with everything else neutral. The usual starting point for tests and debug hooks. */
    public static EnvSample outdoors(double airC, double windMs) {
        return new EnvSample(airC, envFeelsLike(airC, windMs), airC, 4, 30, 0, windMs, windMs, 0, false, 0,
                Precip.NONE, 0, 10_000, 15, 0, 0, 0, 0, 1013, 0, 60, "", 0, 0, 0);
    }

    public EnvSample withPrecip(Precip p, double mmH) {
        return new EnvSample(airC, feelsLikeC, groundC, waterC, moisture, windDirDeg, windMs, gustMs, snowCm, crust,
                iceCm, p, mmH, visibilityM, light, heatBonusC, fireDanger, roomId, cloudPct, pressureHpa,
                pressureTrend, humidityPct, airMass, glazeMm, snowIceMm, smoke);
    }

    /** Inside room {@code id} at room temperature {@code roomC}: wind drops out (Warmth › Shelter). */
    public EnvSample inRoom(int id, double roomC) {
        return new EnvSample(roomC, roomC, groundC, waterC, moisture, windDirDeg, 0, 0, snowCm, crust,
                iceCm, Precip.NONE, 0, visibilityM, light, heatBonusC, fireDanger, id, cloudPct, pressureHpa,
                pressureTrend, humidityPct, airMass, glazeMm, snowIceMm, smoke);
    }

    public EnvSample withHeatBonus(double c) {
        return new EnvSample(airC, feelsLikeC, groundC, waterC, moisture, windDirDeg, windMs, gustMs, snowCm, crust,
                iceCm, precip, precipMmH, visibilityM, light, c, fireDanger, roomId, cloudPct, pressureHpa,
                pressureTrend, humidityPct, airMass, glazeMm, snowIceMm, smoke);
    }

    public EnvSample withWater(double c) {
        return new EnvSample(airC, feelsLikeC, groundC, c, moisture, windDirDeg, windMs, gustMs, snowCm, crust,
                iceCm, precip, precipMmH, visibilityM, light, heatBonusC, fireDanger, roomId, cloudPct, pressureHpa,
                pressureTrend, humidityPct, airMass, glazeMm, snowIceMm, smoke);
    }

    public boolean indoors() { return roomId != 0; }
}
