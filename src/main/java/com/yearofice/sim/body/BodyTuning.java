package com.yearofice.sim.body;

/**
 * The body dials of {@code yearofice-common.toml [body]} (Blueprint › Realism presets; Body tab › Tuning and
 * debug; RECONCILE 135). Pure Java: the adapter in {@code body.BodyConfig} fills one of these from the toml, and
 * tests use the preset constants directly, so GameTests always run on {@link #STANDARD}.
 *
 * @param calorieBurn      the Calorie burn slider, × on every activity rate
 * @param kcalPerKg        kcal per kg of body weight: 2,400 at Standard and Brutal, 3,500 on Gentle (RECONCILE 135)
 * @param coldScale        the Cold slider: × on warmth loss only
 * @param waterNeed        the Water need slider: × on every water drain
 * @param unboiledIllness  chance one drink of untreated water makes the player ill
 * @param stomachKg        stomach capacity
 * @param sosBmi           BMI at or below which the messenger sends SOS
 * @param returnBmi        BMI the player comes back at after a flight out
 * @param sipLitres        one drink from open water
 * @param kitWarmthC       dry warmth of the starting clothes, °C (Blueprint: about +11 for the table's May kit; tuned so a
 *                         fireless first night is miserable on Standard, fatal only on Brutal)
 */
public record BodyTuning(String preset, double calorieBurn, double kcalPerKg, double coldScale, double waterNeed,
                         double unboiledIllness, double bleedScale, double infectionScale, boolean digitLoss,
                         double stomachKg, double sosBmi, double returnBmi, double sipLitres, double kitWarmthC) {

    public static final BodyTuning GENTLE = new BodyTuning("gentle", 0.5, 3500, 0.5, 0.75, 0.15, 0.5, 0.5, false,
            1.8, 17, 19, 0.25, 11);
    public static final BodyTuning STANDARD = new BodyTuning("standard", 1.0, 2400, 1.0, 1.0, 0.30, 1.0, 1.0, true,
            1.8, 17, 19, 0.25, 4.5);
    public static final BodyTuning BRUTAL = new BodyTuning("brutal", 1.25, 2400, 1.3, 1.2, 0.40, 1.5, 1.5, true,
            1.8, 17, 19, 0.25, 2);

    /** The same dials with another starting-kit warmth (0 = bare skin, for tests of the bare-skin rules). */
    public BodyTuning withKit(double c) {
        return new BodyTuning(preset, calorieBurn, kcalPerKg, coldScale, waterNeed, unboiledIllness, bleedScale,
                infectionScale, digitLoss, stomachKg, sosBmi, returnBmi, sipLitres, c);
    }

    public static BodyTuning of(String preset) {
        return switch (preset.toLowerCase(java.util.Locale.ROOT)) {
            case "gentle" -> GENTLE;
            case "brutal" -> BRUTAL;
            default -> STANDARD;
        };
    }
}
