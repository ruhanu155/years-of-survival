package com.yearofice.sim.wood;

/**
 * One row of the Tools table (Trees, felling and firewood › Tools), as {@code data_maps/item/tool.json} gives it.
 *
 * @param fellMaxCm        the thickest trunk the tool fells, in cm; 0 when it fells nothing, a large number for "Any"
 * @param swings25         S, swings for a 25 cm trunk
 * @param swingMin         game minutes a swing
 * @param limbMin          game minutes to limb a felled tree; 0 when it cannot
 * @param buckMin          game minutes to buck one armload (4 rounds); 0 when it cannot
 * @param splitStrikes     strikes to split a round off the crack; 0 when it cannot split
 * @param splitCrackStrikes strikes to split a round on the crack
 */
public record ToolStats(double fellMaxCm, int swings25, double swingMin, double limbMin, double buckMin, int splitStrikes,
                        int splitCrackStrikes) {

    /** The crate hatchet's row: fells up to 45 cm, 6 swings at 25 cm, 4 min a swing, limb 10, buck 3, split 3 (1). */
    public static final ToolStats CRATE_HATCHET = new ToolStats(45, 6, 4, 10, 3, 3, 1);
    /** The felling axe's row: any trunk, 4 swings at 25 cm, 2.5 min a swing, limb 5, buck 2, split 2. */
    public static final ToolStats FELLING_AXE = new ToolStats(1000, 4, 2.5, 5, 2, 2, 1);
    /** The wooden axe's row: up to 12 cm, 15 swings at 25 cm, 4 min, limb 10, cannot buck or split. */
    public static final ToolStats WOODEN_AXE = new ToolStats(12, 15, 4, 10, 0, 0, 0);

    public boolean fells() { return fellMaxCm > 0 && swings25 > 0 && swingMin > 0; }

    public boolean bucks() { return buckMin > 0; }

    public boolean splits() { return splitStrikes > 0; }
}
