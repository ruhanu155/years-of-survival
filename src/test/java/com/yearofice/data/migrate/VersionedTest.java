package com.yearofice.data.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

/** The versioned codec and its steps (Saves across versions): pure functions on the raw map, no Minecraft running. */
class VersionedTest {
    record Thing(String name, double dueDh, String curve) {}

    static final Codec<Thing> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Thing::name),
            Codec.DOUBLE.fieldOf("due_dh").forGetter(Thing::dueDh),
            Codec.STRING.optionalFieldOf("curve", "none").forGetter(Thing::curve)).apply(i, Thing::new));

    static Migrations steps() {
        return new Migrations().addJson(JsonParser.parseString("""
            { "steps": [
              { "migration": "t:thing", "from": 1, "to": 2, "add": { "curve": "meat" }, "rename": { "due": "due_dh" } },
              { "migration": "t:thing", "from": 2, "to": 3, "scale": { "due_dh": 60 }, "map_values": { "curve": { "meat": "flesh" } }, "remove": ["junk"] }
            ] }""").getAsJsonObject());
    }

    static CompoundTag tag(String snbt) {
        try { return TagParser.parseTag(snbt); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Test
    void v1RecordHasNoVersionFieldAndWritesNone() {
        Codec<Thing> c = Versioned.codec(INNER, "t:thing", 1, "v", steps());
        Tag t = c.encodeStart(NbtOps.INSTANCE, new Thing("a", 2, "x")).getOrThrow();
        assertFalse(((CompoundTag) t).contains("v"));
    }

    @Test
    void anOldRecordIsUpgradedBeforeTheCodecDecodes() {
        Codec<Thing> v2 = Versioned.codec(INNER, "t:thing", 2, "v", steps());
        Thing t = v2.parse(NbtOps.INSTANCE, tag("{name:\"a\",due:12.0d}")).getOrThrow();
        assertEquals(new Thing("a", 12, "meat"), t);
        CompoundTag saved = (CompoundTag) v2.encodeStart(NbtOps.INSTANCE, t).getOrThrow();
        assertEquals(2, saved.getInt("v"));
    }

    @Test
    void stepsChainAndTheSecondStepScalesMapsAndRemoves() {
        Codec<Thing> v3 = Versioned.codec(INNER, "t:thing", 3, "v", steps());
        Thing t = v3.parse(NbtOps.INSTANCE, tag("{name:\"a\",due:2.0d,junk:1b}")).getOrThrow();
        assertEquals(new Thing("a", 120, "flesh"), t);
        // Starting at v2 skips the first step.
        Thing u = v3.parse(NbtOps.INSTANCE, tag("{v:2,name:\"b\",due_dh:1.5d,curve:\"meat\"}")).getOrThrow();
        assertEquals(new Thing("b", 90, "flesh"), u);
    }

    @Test
    void addNeverOverwritesAndRenameKeepsTheValue() {
        Codec<Thing> v2 = Versioned.codec(INNER, "t:thing", 2, "v", steps());
        Thing t = v2.parse(NbtOps.INSTANCE, tag("{name:\"a\",due:3.0d,curve:\"ember\"}")).getOrThrow();
        assertEquals("ember", t.curve());
        assertEquals(3, t.dueDh());
    }

    @Test
    void aRecordFromANewerJarIsRefusedNotReadWrong() {
        Codec<Thing> v2 = Versioned.codec(INNER, "t:thing", 2, "v", steps());
        DataResult<Thing> r = v2.parse(NbtOps.INSTANCE, tag("{v:3,name:\"a\",due_dh:1.0d}"));
        assertTrue(r.error().isPresent());
        assertTrue(r.error().get().message().contains("newer Year of Ice"), r.error().get().message());
    }

    @Test
    void aMissingStepIsAnErrorNotASilentSkip() {
        Migrations m = new Migrations();
        Codec<Thing> c = Versioned.codec(INNER, "t:thing", 2, "v", m);
        assertTrue(c.parse(NbtOps.INSTANCE, tag("{name:\"a\",due_dh:1.0d}")).error().get().message().contains("no migration step from v1"));
    }

    @Test
    void migrationIsIdempotent() {
        Codec<Thing> v3 = Versioned.codec(INNER, "t:thing", 3, "v", steps());
        Thing t = v3.parse(NbtOps.INSTANCE, tag("{name:\"a\",due:2.0d}")).getOrThrow();
        Tag once = v3.encodeStart(NbtOps.INSTANCE, t).getOrThrow();
        Thing again = v3.parse(NbtOps.INSTANCE, once).getOrThrow();
        assertEquals(t, again);
        assertEquals(once, v3.encodeStart(NbtOps.INSTANCE, again).getOrThrow());
    }

    @Test
    void javaStepsAndOtherVersionKeysWork() {
        Migrations m = new Migrations().register("t:thing", 1, 2, d -> d.set("curve", d.createString("java")));
        Codec<Thing> c = Versioned.codec(INNER, "t:thing", 2, "schema", m);
        Thing t = c.parse(NbtOps.INSTANCE, tag("{name:\"a\",due_dh:1.0d}")).getOrThrow();
        assertEquals("java", t.curve());
        assertEquals(2, ((CompoundTag) c.encodeStart(NbtOps.INSTANCE, t).getOrThrow()).getInt("schema"));
    }

    @Test
    void stepsMustGoUpAndNotRepeat() {
        assertThrows(IllegalArgumentException.class, () -> new Migrations().register("x", 2, 2, d -> d));
        Migrations m = new Migrations().register("x", 1, 2, d -> d);
        assertThrows(IllegalArgumentException.class, () -> m.register("x", 1, 3, d -> d));
    }

    @Test
    void theShippedMigrationsLoad() {
        // Empty until the first versioned record changes; the file must exist and parse.
        Migrations.shared();
        Dynamic<Tag> d = new Dynamic<>(NbtOps.INSTANCE, tag("{a:1}"));
        assertEquals(d.getValue(), Migrations.shared().upgrade("yearofice:none", d, 1, "v").getValue());
    }
}
