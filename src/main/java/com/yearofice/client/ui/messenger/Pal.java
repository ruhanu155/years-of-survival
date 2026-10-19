package com.yearofice.client.ui.messenger;

/**
 * Palette colours the messenger draws with in code (ARGB), taken from {@code tools/artgen/palette.json} so the
 * live readouts sit in the same palette as the generated textures. The LCD is light; text is dark.
 */
public final class Pal {
    private Pal() {}

    private static int rgb(int hex) { return 0xFF000000 | hex; }

    /** iron 0, the LCD's ink. */
    public static final int INK = rgb(0x25272a);
    /** lichen_pale 0, secondary text and empty bars. */
    public static final int DIM = rgb(0x6a6c5e);
    /** lichen_pale 2, bar troughs. */
    public static final int TROUGH = rgb(0xa6a993);
    /** lichen_pale 4, highlights on the LCD. */
    public static final int LCD_LIGHT = rgb(0xd3d5c0);
    /** berry_red 2: cold, danger, the SOS line. */
    public static final int RED = rgb(0x9c2516);
    /** plastic_orange 2: warm, the active tab. */
    public static final int ORANGE = rgb(0xb0602a);
    /** ice 1: water and wetness. */
    public static final int BLUE = rgb(0x45667a);
    /** flower_yellow 1: energy. */
    public static final int YELLOW = rgb(0x8a6c16);
    /** moss_green 2: healthy. */
    public static final int GREEN = rgb(0x4f5e2b);
    /** wood_cut 1: food. */
    public static final int BROWN = rgb(0x7f6143);
    /** snow 4: text on dark HUD backing. */
    public static final int SNOW = rgb(0xeef2f6);
    /** iron 0 at 60% for HUD backing behind strip numbers. */
    public static final int SHADE = 0x9925272a;
}
