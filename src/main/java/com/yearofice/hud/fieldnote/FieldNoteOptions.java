package com.yearofice.hud.fieldnote;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client option {@code field_note_cards} ({@code yearofice-client.toml}): On, First year only, or Off. */
public final class FieldNoteOptions {
    private FieldNoteOptions() {}

    public enum Mode { ON, FIRST_YEAR, OFF }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MODE = b.comment("Field-note cards: ON, FIRST_YEAR (only in the first game year) or OFF. OFF still files them in the notebook.")
                .defineEnum("field_note_cards", Mode.ON);
        SPEC = b.build();
    }

    /** Does a card show for this setting? */
    public static boolean shows(Mode m, boolean firstYear) { return m == Mode.ON || m == Mode.FIRST_YEAR && firstYear; }
}
