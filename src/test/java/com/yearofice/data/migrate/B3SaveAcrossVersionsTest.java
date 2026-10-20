package com.yearofice.data.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.body.BodyCodec;
import com.yearofice.sim.body.BodyState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prototype B3, a save across versions (Content pipeline › Risks and the first prototypes). "Build 0.2.0": a save
 * with a {@code fresh} stack, a renamed block and a body record (the 0.2.0 fixture folder). "Build 0.2.1": one
 * versioned-codec step (fresh v1 to v2), one alias (a block and an item), one retired component (dampness, which
 * becomes moisture), all in the 0.2.1 fixture folder. Pass: the migration test's checks hold, the backup folder
 * exists, and the old jar refuses the new save with the update message.
 */
class B3SaveAcrossVersionsTest {
    static final SaveVersion V020 = SaveVersion.parse("0.2.0"), V021 = SaveVersion.parse("0.2.1");

    record FreshV2(String state, int clock, double dueDh, double lifeDh, String curve) {}

    static final Codec<FreshV2> FRESH_INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("state").forGetter(FreshV2::state),
            Codec.INT.fieldOf("clock").forGetter(FreshV2::clock),
            Codec.DOUBLE.fieldOf("due_dh").forGetter(FreshV2::dueDh),
            Codec.DOUBLE.fieldOf("life_dh").forGetter(FreshV2::lifeDh),
            Codec.STRING.optionalFieldOf("curve", "none").forGetter(FreshV2::curve)).apply(i, FreshV2::new));

    static JsonObject json(String version, String file) throws IOException {
        return JsonParser.parseString(Files.readString(Snbt.SAVES.resolve(version).resolve(file), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** The 0.2.1 jar's knowledge: its steps, aliases and retired-component rules. */
    record Jar021(Migrations migrations, Aliases aliases, List<LegacyConverter.Rule> rules) {
        static Jar021 load() throws IOException {
            Migrations m = new Migrations().addJson(json("0.2.1", "migrations.json"));
            Aliases a = new Aliases().addJson(json("0.2.1", "block.json")).addJson(json("0.2.1", "item.json"));
            // dampness (a fraction, retired) becomes moisture (a percent): the legacy type stays registered, this rewrites it.
            LegacyConverter.Rule r = new LegacyConverter.Rule("yearofice:dampness", "yearofice:moisture",
                    v -> v.createInt((int) Math.round(v.asDouble(0) * 100)));
            return new Jar021(m, a, List.of(r));
        }

        Codec<FreshV2> fresh() { return Versioned.codec(FRESH_INNER, "yearofice:fresh", 2, "v", migrations); }
    }

    static void copy(String version, String file, Path world, String dest) throws IOException {
        Path to = world.resolve(dest);
        Files.createDirectories(to.getParent());
        Files.copy(Snbt.SAVES.resolve(version).resolve(file), to);
    }

    /** Builds the 0.2.0 world on disk. */
    static void build020(Path world) throws IOException {
        WorldGate.open(world, V020);
        Files.writeString(world.resolve("level.dat"), "LEVEL", StandardCharsets.UTF_8);
        Files.createDirectories(world.resolve("region"));
        Files.writeString(world.resolve("region/r.0.0.mca"), "REGION", StandardCharsets.UTF_8);
        copy("0.2.0", "attachments/body.snbt", world, "playerdata/steve.snbt");
        copy("0.2.0", "stacks.snbt", world, "yearofice/stacks.snbt");
        copy("0.2.0", "block_ref.snbt", world, "yearofice/block_ref.snbt");
    }

    @Test
    void theNewJarUpgradesBacksUpAndTheOldJarRefuses(@TempDir Path world) throws IOException {
        build020(world);
        assertEquals(WorldGate.Kind.SAME, WorldGate.open(world, V020).kind(), "0.2.0 opens its own save");

        WorldGate.Outcome up = WorldGate.open(world, V021);
        assertEquals(WorldGate.Kind.UPGRADE, up.kind());
        Path backup = world.resolve("yearofice/backups/pre-0.2.0");
        assertTrue(Files.isDirectory(backup), "the backup folder exists");
        assertTrue(Files.isRegularFile(backup.resolve("level.dat")));
        assertTrue(Files.isRegularFile(backup.resolve("playerdata/steve.snbt")));
        assertTrue(Files.isRegularFile(backup.resolve("yearofice/stacks.snbt")));
        assertFalse(Files.exists(backup.resolve("region")), "region files are not copied");

        WorldGate.NewerWorldException e = assertThrows(WorldGate.NewerWorldException.class, () -> WorldGate.open(world, V020));
        assertEquals("This world was saved by a newer Year of Ice (0.2). Update the mod to open it.", e.getMessage());
        assertEquals(V021, WorldGate.read(world), "the refusal wrote nothing");
    }

    @Test
    void theSavedThingsComeThroughTheStepTheAliasAndTheLegacyRule(@TempDir Path world) throws IOException {
        build020(world);
        Jar021 jar = Jar021.load();
        assertTrue(jar.aliases().problems().isEmpty());

        // The renamed block resolves.
        CompoundTag ref = (CompoundTag) Snbt.read(world.resolve("yearofice/block_ref.snbt"));
        assertEquals("yearofice:fire_ring", jar.aliases().resolve("block", ref.getString("block")));

        // The body record: schema 1 loads at schema 2 with every part at 100.
        BodyState body = BodyCodec.CODEC.parse(Snbt.OPS, Snbt.read(world.resolve("playerdata/steve.snbt"))).getOrThrow();
        assertEquals(77.0, body.kg, 1e-6);
        assertEquals(100, body.parts.hp(com.yearofice.sim.body.part.Part.HEAD));

        // The stacks: alias on the id, retired component rewritten, fresh stepped from v1 to v2.
        ListTag stacks = (ListTag) Snbt.read(world.resolve("yearofice/stacks.snbt"));
        CompoundTag sinew = null, meat = null;
        for (Tag t : stacks) {
            LegacyConverter.Converted<Tag> c = LegacyConverter.convertStack(new Dynamic<>(Snbt.OPS, t), jar.rules(), jar.aliases());
            CompoundTag out = (CompoundTag) c.stack().getValue();
            if (out.getString("id").endsWith("sinew")) sinew = out; else meat = out;
        }
        assertEquals("yearofice:sinew", sinew.getString("id"));
        assertEquals(12, sinew.getInt("count"), "same count after aliasing");
        CompoundTag sc = sinew.getCompound("components");
        assertFalse(sc.contains("yearofice:dampness"), "the retired component is gone");
        assertEquals(35, sc.getInt("yearofice:moisture"));

        assertEquals("yearofice:snowshoe_hare_meat", meat.getString("id"));
        FreshV2 f = jar.fresh().parse(Snbt.OPS, meat.getCompound("components").get("yearofice:fresh")).getOrThrow();
        assertEquals(new FreshV2("raw", 1, 30.0, 96.0, "meat"), f);
        CompoundTag saved = (CompoundTag) jar.fresh().encodeStart(Snbt.OPS, f).getOrThrow();
        assertEquals(2, saved.getInt("v"));
        assertFalse(saved.contains("due"));
    }

    @Test
    void migrationIsIdempotentAndAFreshStackNeedsNoStep(@TempDir Path world) throws IOException {
        build020(world);
        Jar021 jar = Jar021.load();
        ListTag stacks = (ListTag) Snbt.read(world.resolve("yearofice/stacks.snbt"));
        for (Tag t : stacks) {
            LegacyConverter.Converted<Tag> once = LegacyConverter.convertStack(new Dynamic<>(Snbt.OPS, t), jar.rules(), jar.aliases());
            LegacyConverter.Converted<Tag> twice = LegacyConverter.convertStack(once.stack(), jar.rules(), jar.aliases());
            assertFalse(twice.changed(), "a second pass changes nothing");
            assertEquals(once.stack().getValue(), twice.stack().getValue());
        }
        // A fresh component already at v2 passes through the codec unchanged.
        Tag current = jar021Encoded(jar);
        assertEquals(current, jar.fresh().encodeStart(Snbt.OPS, jar.fresh().parse(Snbt.OPS, current).getOrThrow()).getOrThrow());
    }

    private static Tag jar021Encoded(Jar021 jar) {
        return jar.fresh().encodeStart(Snbt.OPS, new FreshV2("cooked", 1, 2.5, 8.0, "meat")).getOrThrow();
    }

    @Test
    void theLegacyRuleDropsOrKeepsByItsReplacement() {
        Aliases none = new Aliases();
        LegacyConverter.Rule drop = new LegacyConverter.Rule("yearofice:old", null, null);
        LegacyConverter.Rule move = new LegacyConverter.Rule("yearofice:older", "yearofice:newer", null);
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "yearofice:x");
        CompoundTag comps = new CompoundTag();
        comps.putInt("yearofice:old", 1);
        comps.putInt("yearofice:older", 5);
        stack.put("components", comps);
        LegacyConverter.Converted<Tag> c = LegacyConverter.convertStack(new Dynamic<>(Snbt.OPS, stack), List.of(drop, move), none);
        assertTrue(c.changed());
        CompoundTag out = ((CompoundTag) c.stack().getValue()).getCompound("components");
        assertEquals(1, out.size());
        assertEquals(5, out.getInt("yearofice:newer"));

        comps.putInt("yearofice:newer", 9);
        CompoundTag out2 = ((CompoundTag) LegacyConverter.convertStack(new Dynamic<>(Snbt.OPS, stack), List.of(move), none).stack().getValue()).getCompound("components");
        assertEquals(9, out2.getInt("yearofice:newer"), "an existing live component is never overwritten");
        assertFalse(out2.contains("yearofice:older"));

        assertFalse(LegacyConverter.convertStack(new Dynamic<>(Snbt.OPS, new CompoundTag()), List.of(drop), none).changed());
    }
}
