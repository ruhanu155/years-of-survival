package com.yearofice.gametest;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyCodec;
import com.yearofice.core.YearOfIce;
import com.yearofice.data.migrate.Aliases;
import com.yearofice.data.migrate.MigrateSystem;
import com.yearofice.data.migrate.SaveVersion;
import com.yearofice.data.migrate.WorldGate;
import com.yearofice.item.inventory.PackAttachments;
import com.yearofice.sim.body.BodyState;
import com.yearofice.story.StoryAttachments;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * {@code data/migrate_fixtures} on a live server (Minecraft development › Saves across versions › The migration
 * test; Ready for development › M3). The newest fixture folder under {@code src/test/resources/saves} (the Gradle
 * run passes its path as {@code yearofice.fixtures}) is loaded through the real registries and attachment types:
 * <ul>
 *   <li>{@code migrate/fixtures_stacks}: every fixture stack decodes and re-encodes unchanged, with no unknown id,
 *       and every {@code yearofice:} data component the game registers is carried by some stack;</li>
 *   <li>{@code migrate/fixtures_attachments}: the player attachments (body, onboarding, pack rules) set from the
 *       fixtures save, reload and save again identically through the registered types;</li>
 *   <li>{@code migrate/world_stamp}: this world carries the jar's save stamp, and {@code /yoi migrate status} runs.</li>
 * </ul>
 * The same fixtures also run in JUnit ({@code SaveFixturesTest}) where no server is needed.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class MigrateGameTests {
    private MigrateGameTests() {}

    static final String BATCH = "m3_migrate";

    @GameTestGenerator
    public static Collection<TestFunction> migrateTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:migrate/fixtures_stacks", MigrateGameTests::fixturesStacks));
        out.add(test("yearofice:migrate/fixtures_attachments", MigrateGameTests::fixturesAttachments));
        out.add(test("yearofice:migrate/world_stamp", MigrateGameTests::worldStamp));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static net.minecraft.gametest.framework.GameTestAssertException fail(String m) {
        return new net.minecraft.gametest.framework.GameTestAssertException(m);
    }

    static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, 100, 0, true, body);
    }

    /** The newest fixture folder (one with an {@code attachments} folder), found from the Gradle-provided root. */
    static Path newest(GameTestHelper h) {
        String root = System.getProperty("yearofice.fixtures");
        if (root == null) throw fail("system property yearofice.fixtures is not set; the Gradle gameTestServer run passes it");
        try (Stream<Path> s = Files.list(Path.of(root))) {
            return s.filter(p -> Files.isDirectory(p.resolve("attachments")))
                    .max(Comparator.comparing(p -> SaveVersion.parse(p.getFileName().toString())))
                    .orElseThrow(() -> fail("no fixture folder under " + root));
        } catch (IOException e) {
            throw fail("cannot list " + root + ": " + e.getMessage());
        }
    }

    static Tag snbt(GameTestHelper h, Path file) {
        try {
            return TagParser.parseTag("{v:" + Files.readString(file, StandardCharsets.UTF_8).trim() + "}").get("v");
        } catch (Exception e) {
            throw fail(file + ": " + e.getMessage());
        }
    }

    /** Lists of compounds sorted by text, so hash-ordered saves compare equal. */
    static Tag canonical(Tag t) {
        if (t instanceof CompoundTag c) {
            CompoundTag out = new CompoundTag();
            for (String k : c.getAllKeys()) out.put(k, canonical(c.get(k)));
            return out;
        }
        if (t instanceof ListTag l) {
            List<Tag> items = new ArrayList<>();
            for (Tag e : l) items.add(canonical(e));
            if (!items.isEmpty() && items.get(0) instanceof CompoundTag) items.sort(Comparator.comparing(Tag::toString));
            ListTag out = new ListTag();
            out.addAll(items);
            return out;
        }
        return t;
    }

    static void fixturesStacks(GameTestHelper h) {
        Path dir = newest(h);
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
        ListTag stacks = (ListTag) snbt(h, dir.resolve("stacks.snbt"));
        h.assertTrue(!stacks.isEmpty(), "the stacks fixture is empty");
        Map<String, Integer> seen = new TreeMap<>();
        for (Tag t : stacks) {
            String id = ((CompoundTag) t).getString("id");
            ItemStack s = ItemStack.CODEC.parse(ops, t).getOrThrow(m -> fail(id + " does not decode: " + m));
            h.assertTrue(!s.isEmpty(), id + " decoded to an empty stack: unknown id? (an alias is missing)");
            Tag back = ItemStack.CODEC.encodeStart(ops, s).getOrThrow(m -> fail(id + " does not encode: " + m));
            h.assertTrue(canonical(t).equals(canonical(back)), id + " changed in a round trip: " + t + " -> " + back);
            for (var c : s.getComponents()) {
                ResourceLocation k = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.type());
                if (k != null && k.getNamespace().equals(YearOfIce.MOD_ID)) seen.merge(k.toString(), 1, Integer::sum);
            }
        }
        for (ResourceLocation k : BuiltInRegistries.DATA_COMPONENT_TYPE.keySet()) {
            if (k.getNamespace().equals(YearOfIce.MOD_ID) && !seen.containsKey(k.toString()))
                throw fail("no fixture stack carries the component " + k);
        }
        h.assertTrue(Aliases.shared().problems().isEmpty(), "alias problems: " + Aliases.shared().problems());
        h.succeed();
    }

    static void fixturesAttachments(GameTestHelper h) {
        Path dir = newest(h);
        ServerPlayer p = LayerGameTests.player(h, "migrate_fixture");
        var provider = h.getLevel().registryAccess();
        Tag body = snbt(h, dir.resolve("attachments/body.snbt"));
        Tag onboarding = snbt(h, dir.resolve("attachments/onboarding.snbt"));
        Tag pack = snbt(h, dir.resolve("attachments/pack_rules.snbt"));
        BodyState bs = BodyCodec.CODEC.parse(NbtOps.INSTANCE, body).getOrThrow(m -> fail("body: " + m));
        p.setData(BodyAttachments.BODY, bs);
        p.setData(StoryAttachments.ONBOARDING, com.yearofice.story.OnboardingRecord.CODEC.parse(NbtOps.INSTANCE, onboarding)
                .getOrThrow(m -> fail("onboarding: " + m)));
        p.setData(PackAttachments.PACK_RULES, PackAttachments.Rules.CODEC.parse(NbtOps.INSTANCE, pack)
                .getOrThrow(m -> fail("pack_rules: " + m)));

        CompoundTag saved = p.serializeAttachments(provider);
        h.assertTrue(saved != null, "the player saved no attachments");
        check(h, saved, "yearofice:body", body);
        check(h, saved, "yearofice:onboarding", onboarding);
        check(h, saved, "yearofice:pack_rules", pack);

        // Load it into a second player and save again: nothing moves.
        ServerPlayer q = LayerGameTests.player(h, "migrate_fixture_b");
        CompoundTag wrapper = new CompoundTag();
        p.saveWithoutId(wrapper);
        q.load(wrapper);
        CompoundTag again = q.serializeAttachments(provider);
        h.assertTrue(again != null, "the reloaded player saved no attachments");
        h.assertTrue(BodyCodec.sameAsSaved(bs, q.getData(BodyAttachments.BODY)), "the body changed across save and load");
        check(h, again, "yearofice:body", body);
        check(h, again, "yearofice:onboarding", onboarding);
        check(h, again, "yearofice:pack_rules", pack);
        h.succeed();
    }

    static void check(GameTestHelper h, CompoundTag saved, String key, Tag fixture) {
        h.assertTrue(saved.contains(key), "attachment " + key + " was not saved");
        h.assertTrue(canonical(saved.get(key)).equals(canonical(fixture)), key + " saved as " + saved.get(key) + ", fixture " + fixture);
    }

    static void worldStamp(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Path world = server.getWorldPath(LevelResource.ROOT);
        try {
            SaveVersion stamp = WorldGate.read(world);
            h.assertTrue(stamp != null, "no save_version.json in " + world);
            h.assertTrue(stamp.equals(MigrateSystem.jarVersion()), "stamp " + stamp + " vs jar " + MigrateSystem.jarVersion());
        } catch (IOException e) {
            throw fail("cannot read the stamp: " + e.getMessage());
        }
        h.assertTrue(MigrateSystem.last() != null, "the gate did not run when the server started");
        try {
            int r = server.getCommands().getDispatcher().execute("yoi migrate status", server.createCommandSourceStack());
            h.assertTrue(r == 1, "/yoi migrate status returned " + r);
        } catch (Exception e) {
            throw fail("/yoi migrate status failed: " + e.getMessage());
        }
        h.succeed();
    }
}
