package com.yearofice.sim.body;

/**
 * Feels-like temperature and each of its terms, for the vitals screen ("Meter, feels-like and each term
 * of it"). Signs as they enter the sum: wind chill and the wet penalty are positive numbers subtracted.
 */
public record FeelsLike(double airC, double windChillC, double wetPenaltyC, double clothingC, double shelterC,
                        double fireC, double activityC, double bodyC) {

    public double totalC() {
        return airC - windChillC - wetPenaltyC + clothingC + shelterC + fireC + activityC + bodyC;
    }
}
