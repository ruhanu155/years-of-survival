package com.yearofice.client.ui;

import com.yearofice.api.hud.HudArg;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code client.ui.Units}: how the client formats a HUD line's numbers, metric first (Layer 8). Fixed-point
 * arguments carry hundredths and a unit tag; the tag picks the unit's symbol and decimals. Lang values never carry
 * literal units (linter rule): the number and its unit come from here.
 */
public final class Units {
    private Units() {}

    /** A fixed-point value with its unit: 1250 "kg" is "12.5 kg"; 2150 "c" is "21.5 °C". */
    public static String fixed(long hundredths, String unit) {
        double v = hundredths / 100.0;
        return switch (unit) {
            case "c" -> String.format(Locale.ROOT, "%.0f °C", v);
            case "kg" -> String.format(Locale.ROOT, v < 10 ? "%.1f kg" : "%.0f kg", v);
            case "g" -> String.format(Locale.ROOT, "%.0f g", v);
            case "m" -> v >= 1000 ? String.format(Locale.ROOT, "%.1f km", v / 1000) : String.format(Locale.ROOT, "%.0f m", v);
            case "km" -> String.format(Locale.ROOT, "%.1f km", v);
            case "cm" -> String.format(Locale.ROOT, "%.0f cm", v);
            case "mm" -> String.format(Locale.ROOT, "%.0f mm", v);
            case "l" -> String.format(Locale.ROOT, "%.1f L", v);
            case "ml" -> String.format(Locale.ROOT, "%.0f mL", v);
            case "kcal" -> String.format(Locale.ROOT, "%,.0f kcal", v);
            case "pct" -> String.format(Locale.ROOT, "%.0f%%", v);
            case "min" -> String.format(Locale.ROOT, "%.0f min", v);
            case "h" -> String.format(Locale.ROOT, "%.1f h", v);
            case "ms" -> String.format(Locale.ROOT, "%.0f m/s", v);
            default -> String.format(Locale.ROOT, "%.2f", v);
        };
    }

    /** One argument as a translatable's format argument. */
    public static Object arg(HudArg a) {
        return switch (a.kind()) {
            case INT -> String.format(Locale.ROOT, "%,d", a.value());
            case FIXED -> fixed(a.value(), a.text());
            case ITEM -> {
                ResourceLocation id = ResourceLocation.tryParse(a.text());
                yield id == null ? a.text() : BuiltInRegistries.ITEM.get(id).getDescription();
            }
            case SPECIES -> {
                ResourceLocation id = ResourceLocation.tryParse(a.text());
                yield id == null ? Component.literal(a.text()) : Component.translatable("entity." + id.getNamespace() + "." + id.getPath());
            }
            case TEXT -> a.text();
        };
    }
}
