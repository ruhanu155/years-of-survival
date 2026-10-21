package com.yearofice.data.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Codec;
import com.yearofice.body.BodyCodec;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.item.inventory.PackAttachments;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.part.Part;
import com.yearofice.story.OnboardingRecord;
import com.yearofice.world.attach.TreeMarksData;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.junit.jupiter.api.Test;

/**
 * {@code data/migrate_fixtures} (Saves across versions › The migration test): a snapshot of every attachment and
 * SavedData the mod saves, per released version, under {@code src/test/resources/saves/<version>/}. For every
 * version folder, every fixture loads through the live codec or loader with no error, and loading the result again
 * changes nothing. The newest folder is the current format, so it must also round-trip byte for byte, and it must
 * cover everything the source registers: a new attachment, SavedData or component without a fixture fails the build.
 * Check 5 (the block-column hash) and the container, fire and trap checks need a running server; the GameTest
 * {@code migrate/fixtures_live} runs the item stacks and the real attachments on a live player.
 */
class SaveFixturesTest {
    interface Rt { Tag roundTrip(Tag in) throws Exception; }

    static <T> Rt codec(Codec<T> c) {
        return in -> c.encodeStart(Snbt.OPS, c.parse(Snbt.OPS, in).getOrThrow()).getOrThrow();
    }

    static Object call(Class<?> cls, String name, Object target, Object... args) throws Exception {
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(cls + "." + name);
    }

    static Rt saved(Class<?> cls) {
        return in -> {
            SavedData d = (SavedData) call(cls, "load", null, (CompoundTag) in, null);
            return d.save(new CompoundTag(), null);
        };
    }

    static final Map<String, Rt> ATTACHMENTS = new TreeMap<>();
    static final Map<String, Rt> SAVED_DATA = new TreeMap<>();

    static {
        ATTACHMENTS.put("body", codec(BodyCodec.CODEC));
        ATTACHMENTS.put("onboarding", codec(OnboardingRecord.CODEC));
        ATTACHMENTS.put("field_notes", codec(com.yearofice.hud.fieldnote.FieldNoteRecord.CODEC));
        ATTACHMENTS.put("pack_rules", codec(PackAttachments.Rules.CODEC));
        ATTACHMENTS.put("carcass", codec(CarcassRecord.CODEC));
        ATTACHMENTS.put("placed_mask", in -> (Tag) call(com.yearofice.world.attach.PlacedMaskData.class, "write", call(
                com.yearofice.world.attach.PlacedMaskData.class, "read", null, in)));
        ATTACHMENTS.put("trees", in -> TreeMarksData.load(in).saveTag());
        ATTACHMENTS.put("plant_notes", codec(com.yearofice.plant.PlantAttachments.NOTES_CODEC));
        ATTACHMENTS.put("patches", codec(com.yearofice.plant.PatchData.CODEC));
        SAVED_DATA.put("yearofice_clock", saved(com.yearofice.story.clock.ClockData.class));
        SAVED_DATA.put("yearofice_run", saved(com.yearofice.body.RunState.class));
        SAVED_DATA.put("yearofice_story", saved(com.yearofice.story.DayOne.Data.class));
        SAVED_DATA.put("yearofice_slots", saved(com.yearofice.ecosystem.spawn.SlotData.class));
        SAVED_DATA.put("yearofice_fish_stocks", saved(com.yearofice.hunt.fishing.SliceSpots.Stocks.class));
        SAVED_DATA.put("yearofice_fish_gear", saved(com.yearofice.hunt.fishing.PassiveGearService.Book.class));
        SAVED_DATA.put("yearofice_weather", saved(com.yearofice.env.weather.WeatherData.class));
        SAVED_DATA.put("yearofice_run_ledger", saved(com.yearofice.hunt.fishing.RunLedger.class));
    }

    static String latest() {
        List<String> v = Snbt.versions();
        return v.get(v.size() - 1);
    }

    static Rt rt(String file) {
        String folder = file.substring(0, file.indexOf('/'));
        String name = file.substring(file.indexOf('/') + 1, file.length() - ".snbt".length());
        Rt r = (folder.equals("attachments") ? ATTACHMENTS : SAVED_DATA).get(name);
        assertTrue(r != null, "fixture " + file + " has no loader in SaveFixturesTest");
        return r;
    }

    @Test
    void everyFixtureOfEveryVersionLoadsAndIsStableOnASecondLoad() throws Exception {
        int n = 0;
        for (String v : Snbt.versions()) {
            for (String folder : List.of("attachments", "saved_data")) {
                for (String f : Snbt.files(v, folder)) {
                    Rt r = rt(f);
                    Tag once = r.roundTrip(Snbt.read(v, f));
                    Tag twice = r.roundTrip(once);
                    assertEquals(Snbt.canonical(once), Snbt.canonical(twice), v + "/" + f + " changed on a second load");
                    n++;
                }
            }
        }
        assertTrue(n >= 10, "fixtures went missing: " + n);
    }

    @Test
    void theNewestFixturesAreTheCurrentFormatByteForByte() throws Exception {
        String v = latest();
        for (String folder : List.of("attachments", "saved_data")) {
            for (String f : Snbt.files(v, folder)) {
                Tag in = Snbt.read(v, f);
                assertEquals(Snbt.canonical(in), Snbt.canonical(rt(f).roundTrip(in)),
                        v + "/" + f + " no longer round-trips: the format changed. Add a versioned step and a new fixture folder, do not edit this one.");
            }
        }
    }

    @Test
    void newestFixturesCoverEveryAttachmentSavedDataAndComponentInTheSource() throws IOException {
        String v = latest();
        TreeSet<String> attachments = new TreeSet<>(), saved = new TreeSet<>(), components = new TreeSet<>();
        Pattern att = Pattern.compile("register\\(\"([a-z_]+)\",\\s*\\(\\)\\s*->\\s*AttachmentType");
        Pattern sd = Pattern.compile("\"(yearofice_[a-z_]+)\"");
        Pattern comp = Pattern.compile("registerComponentType\\(\"([a-z_]+)\"");
        try (Stream<Path> s = Files.walk(Path.of("").toAbsolutePath().resolve("src/main/java"))) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String text = Files.readString(p);
                for (Matcher m = att.matcher(text); m.find(); ) attachments.add(m.group(1));
                for (Matcher m = comp.matcher(text); m.find(); ) components.add(m.group(1));
                if (text.contains("extends SavedData")) for (Matcher m = sd.matcher(text); m.find(); ) saved.add(m.group(1));
            }
        }
        TreeSet<String> haveAtt = new TreeSet<>(), haveSaved = new TreeSet<>();
        for (String f : Snbt.files(v, "attachments")) haveAtt.add(f.substring(f.indexOf('/') + 1, f.length() - 5));
        for (String f : Snbt.files(v, "saved_data")) haveSaved.add(f.substring(f.indexOf('/') + 1, f.length() - 5));
        assertFalse(attachments.isEmpty() || saved.isEmpty() || components.isEmpty(), "the source scan found nothing");
        assertEquals(attachments, haveAtt, "attachments registered vs fixtures in " + v);
        assertEquals(saved, haveSaved, "SavedData registered vs fixtures in " + v);
        String stacks = Snbt.read(v, "stacks.snbt").toString();
        for (String c : components) assertTrue(stacks.contains("yearofice:" + c), "no fixture stack carries the component yearofice:" + c);
    }

    @Test
    void theBodyOfEveryVersionKeepsItsNumbers() throws Exception {
        for (String v : Snbt.versions()) {
            if (Snbt.files(v, "attachments").stream().noneMatch(f -> f.endsWith("/body.snbt"))) continue;
            CompoundTag raw = (CompoundTag) Snbt.read(v, "attachments/body.snbt");
            BodyState s = BodyCodec.CODEC.parse(Snbt.OPS, raw).getOrThrow();
            double rawBmi = raw.getFloat("kg") / Math.pow(raw.getInt("height_cm") / 100.0, 2);
            double bmi = s.kg / Math.pow(s.heightCm / 100.0, 2);
            assertEquals(rawBmi, bmi, 0.01, v + ": BMI");
            CompoundTag out = (CompoundTag) BodyCodec.CODEC.encodeStart(Snbt.OPS, s).getOrThrow();
            BodyState again = BodyCodec.CODEC.parse(Snbt.OPS, out).getOrThrow();
            assertTrue(BodyCodec.sameAsSaved(s, again), v + ": a field was lost between load and save");
            assertEquals(BodyState.SCHEMA, out.getInt("schema"), v + ": the body is always written at the current schema");
        }
    }

    @Test
    void aSchema1BodyUpgradesItsPartsAndKeepsItsProblems() {
        CompoundTag raw = (CompoundTag) Snbt.read("0.2.0", "attachments/body.snbt");
        assertFalse(raw.contains("part_hp"));
        BodyState s = BodyCodec.CODEC.parse(Snbt.OPS, raw).getOrThrow();
        for (Part p : Part.values()) assertEquals(100, s.parts.hp(p), p.name());
        assertEquals(88.0, s.parts.condition(), 1e-6);
        assertEquals(12.0, s.parts.dSys(), 1e-6, "the old Condition loss all systemic");
        assertEquals(1, s.parts.problems().size());
        assertEquals("cut", s.parts.problems().get(0).id());
        assertEquals(Part.LEFT_HAND, s.parts.problems().get(0).part());
    }

    @Test
    void modSaveDataStaysUnderTheBudget() throws IOException {
        for (String v : Snbt.versions()) {
            long bytes = 0;
            try (Stream<Path> s = Files.walk(Snbt.SAVES.resolve(v))) {
                for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) bytes += Files.size(p);
            }
            assertTrue(bytes < 5_000_000, v + " fixtures are " + bytes + " bytes; the mod save budget is 5 MB");
        }
    }

    @SuppressWarnings("unused")
    private static List<Tag> asList(ListTag l) { return new ArrayList<>(l); }
}
