package com.yearofice.story.keepsake;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.EncounterOutcomeEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.RadioMath;
import com.yearofice.story.Journal;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The valley remembers (Creative concepts › 15; Legends and named animals): a wolf or bear the player met and walked away
 * from gets a name from {@code story/animal_names.json}, one per species per player, and a later meeting is journalled by
 * that name. ponytail: the name belongs to the species' first survived meeting, not to one individual animal record;
 * upgrade: the {@code yearofice:legend} attachment on the animal once LegendManager lands.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class NamedAnimals {
    private NamedAnimals() {}

    static final String KEY = "yearofice:named_animals";

    private static JsonObject names;

    private static synchronized JsonObject names() {
        if (names == null) {
            try (var in = NamedAnimals.class.getResourceAsStream("/data/yearofice/story/animal_names.json")) {
                names = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return names;
    }

    /** The name this player gave the species, or "" when none yet. */
    public static String nameOf(ServerPlayer p, String species) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY).getString(species);
    }

    /** A survived meeting: names the species on the first, journals a callback on later ones. Returns the name, or "" for a species with no names. */
    public static String met(ServerPlayer p, String species, long minute) {
        JsonObject n = names();
        if (!n.has(species)) return "";
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag book = root.getCompound(KEY);
        String name = book.getString(species), shown = species.replace('_', ' ');
        int day = (int) Math.floorDiv(minute, 1440L);
        if (name.isEmpty()) {
            JsonArray pool = n.getAsJsonArray(species);
            name = pool.get(RadioMath.pick(p.getUUID().getLeastSignificantBits(), species.hashCode(), pool.size())).getAsString();
            book.putString(species, name);
            Journal.record(p, "named_animal", species, n.get("first_line").getAsString().replace("{name}", name).replace("{species}", shown), minute, day);
        } else {
            Journal.record(p, "named_animal_again", species, n.get("return_line").getAsString().replace("{name}", name).replace("{species}", shown), minute, day);
        }
        root.put(KEY, book);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        return name;
    }

    @SubscribeEvent
    public static void encounter(EncounterOutcomeEvent e) {
        if (!e.outcome().equals("retreat")) return; // the animal backed off and the player walked away
        met(e.player(), e.species(), ClockService.installed() ? ClockService.get().nowMinute() : 0);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("keepsake")
                .then(Commands.literal("give").then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Keepsakes.ids(), b))
                        .executes(c -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
                            if (Keepsakes.get(id) == null) { c.getSource().sendFailure(Component.literal("No keepsake " + id)); return 0; }
                            c.getSource().getPlayerOrException().getInventory().add(KeepsakeRegistry.stack(id));
                            return 1;
                        })))));
    }
}
