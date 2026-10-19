package com.yearofice.ecosystem.debug;

import com.yearofice.item.component.YoiComponents;
import com.mojang.authlib.GameProfile;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.spawn.NoVanillaMobs;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.ecosystem.AnimalState;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, ecosystem: the slice's animals on a real server. Names follow the Blueprint's GameTest ids
 * ({@code yearofice:ecosystem/...}). Threats are detached test players registered with {@link ThreatSources}, moved
 * a step each tick; scent is switched off on the animals so the drawn wind never decides a result. Chunks along a
 * flight are force-loaded for the test so the animal keeps ticking.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class EcoGameTests {
    private EcoGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos AT = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> ecosystemTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:ecosystem/hare_flees_at_flush_distance", 500, EcoGameTests::hareFlees));
        out.add(test("yearofice:ecosystem/kill_leaves_carcass_no_loot", 100, EcoGameTests::killLeavesCarcass));
        out.add(test("yearofice:ecosystem/moose_ignores_player_damage", 100, EcoGameTests::mooseIgnoresDamage));
        out.add(test("yearofice:ecosystem/slice_moose_flees", 900, EcoGameTests::mooseFlees));
        out.add(test("yearofice:ecosystem/no_monsters_full_night", 400, EcoGameTests::noMonstersFullNight));
        out.add(test("yearofice:ecosystem/butcher_hare_yields", 100, EcoGameTests::butcherHare));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, maxTicks, 0, true, body);
    }

    /** A detached server player: real enough to be a threat, attack and hold items, never in the player list. */
    public static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "eco-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        return p;
    }

    /** Chunks this class forced, with how many running tests hold each; one the framework held is never touched. */
    static final java.util.Map<Long, Integer> FORCED = new java.util.HashMap<>();

    static synchronized void force(GameTestHelper h, Vec3 center, int radiusChunks, boolean on) {
        ChunkPos c = new ChunkPos(BlockPos.containing(center));
        ServerLevel level = h.getLevel();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                long key = ChunkPos.asLong(c.x + dx, c.z + dz);
                if (on) {
                    Integer n = FORCED.get(key);
                    if (n != null) FORCED.put(key, n + 1);
                    else if (!level.getForcedChunks().contains(key)) {
                        level.setChunkForced(c.x + dx, c.z + dz, true);
                        FORCED.put(key, 1);
                    }
                } else {
                    Integer n = FORCED.get(key);
                    if (n == null) continue;
                    if (n > 1) FORCED.put(key, n - 1);
                    else {
                        FORCED.remove(key);
                        level.setChunkForced(c.x + dx, c.z + dz, false);
                    }
                }
            }
        }
    }

    static String blocker(GameTestHelper h, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = (int) Math.ceil(d.length() / 0.5);
        for (int i = 1; i < steps; i++) {
            Vec3 q = from.add(d.scale(i / (double) steps));
            BlockPos b = BlockPos.containing(q);
            var st = h.getLevel().getBlockState(b);
            if (!st.isAir() && st.canOcclude()) return st + " at " + b.toShortString() + " eye " + from;
        }
        return "none";
    }

    /** A stone pad high above the test grid, so no other test's blocks stand between an animal and the player. */
    static final int PAD_Y = 30;

    /** The moose's pad is low: a 4 m bank lifts its eyes over the next tests' blocks, and it steps off it unhurt. */
    static final int BANK_Y = 4;

    static BlockPos pad(GameTestHelper h, int r, int y) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) h.setBlock(new BlockPos(1 + dx, y, 1 + dz), net.minecraft.world.level.block.Blocks.STONE);
        return new BlockPos(1, y + 1, 1);
    }

    static void unpad(GameTestHelper h, int r, int y) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) h.setBlock(new BlockPos(1 + dx, y, 1 + dz), net.minecraft.world.level.block.Blocks.AIR);
    }

    static double flat(Vec3 a, Vec3 b) { return Math.hypot(a.x - b.x, a.z - b.z); }

    /**
     * Land animals in depth › Snowshoe hare: a hare freezes at awareness 30 and bolts zigzag inside its flush
     * distance. A player walking straight at it at 3 m/s from 25 m sees it freeze first, then flush at 5 m × 1.5
     * (head-on) = 7.5 m (±1.5 m for awareness reaching 70 a step early and the 5-tick motion sample), and it runs at
     * least 15 m before it stops.
     */
    static void hareFlees(GameTestHelper h) {
        SnowshoeHareEntity hare = h.spawn(EcosystemRegistry.SNOWSHOE_HARE.get(), pad(h, 1, PAD_Y));
        hare.setScentEnabled(false);
        Vec3 start = hare.position();
        double eyeY = start.y;
        force(h, start, 3, true);
        ServerPlayer p = player(h);
        ThreatSources.addForTest(p, hare);
        p.moveTo(start.x - 25, start.y, start.z, -90, 0);
        boolean[] froze = {false};
        double[] fledAt = {-1};
        Vec3[] fleeStart = {null};
        long[] fleeTick = {-1};
        h.onEachTick(() -> {
            AnimalState s = hare.state();
            if (s == AnimalState.FREEZE && fledAt[0] < 0) froze[0] = true;
            if (s == AnimalState.FLEE && fledAt[0] < 0) {
                fledAt[0] = flat(p.position(), hare.position());
                fleeStart[0] = hare.position();
                fleeTick[0] = h.getTick();
            }
            if (fledAt[0] < 0) {
                Vec3 to = hare.position().subtract(p.position());
                Vec3 step = new Vec3(to.x, 0, to.z).normalize().scale(0.15);
                p.moveTo(p.getX() + step.x, eyeY, p.getZ() + step.z, p.getYRot(), 0);
                if (h.getTick() > 300) h.fail("the hare never fled; state " + s + ", awareness " + hare.awareness().max());
                return;
            }
            double ran = flat(fleeStart[0], hare.position());
            if (hare.fleeCoveredM() >= 15 || ran >= 15) {
                h.assertTrue(froze[0], "the hare should freeze before it bolts");
                h.assertTrue(fledAt[0] >= 6.0 && fledAt[0] <= 9.0, "bolted with the player at " + fledAt[0] + " m; flush distance 7.5 m");
                ThreatSources.removeForTest(p);
                force(h, start, 3, false);
                unpad(h, 1, PAD_Y);
                h.succeed();
            } else if (h.getTick() - fleeTick[0] > 120) {
                h.fail("fled only " + hare.fleeCoveredM() + " m in 6 s");
            }
        });
    }

    /**
     * Bestiary › Carcass GameTests 1 (no drops, one body), slice form: a hare hit by a player leaves exactly one
     * {@code carcass_small} and no item or experience, and taking the carcass up gives one {@code whole_game} with
     * its species, weight and freshness. Vanilla's death never runs, so {@code doMobLoot} plays no part.
     */
    static void killLeavesCarcass(GameTestHelper h) {
        SnowshoeHareEntity hare = h.spawn(EcosystemRegistry.SNOWSHOE_HARE.get(), AT);
        ServerPlayer p = player(h);
        p.moveTo(hare.getX() + 1, hare.getY(), hare.getZ(), 0, 0);
        h.runAfterDelay(5, () -> {
            h.assertTrue(hare.hurt(h.getLevel().damageSources().playerAttack(p), 1f), "the hit did not take");
            h.runAfterDelay(10, () -> {
                AABB box = new AABB(hare.position(), hare.position()).inflate(8);
                ServerLevel l = h.getLevel();
                List<CarcassEntity> carcasses = l.getEntitiesOfClass(CarcassEntity.class, box);
                h.assertTrue(hare.isRemoved(), "the living hare should be gone");
                h.assertTrue(carcasses.size() == 1, "carcasses " + carcasses.size());
                h.assertTrue(l.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "vanilla loot dropped");
                h.assertTrue(l.getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(), "experience dropped");
                CarcassEntity c = carcasses.get(0);
                h.assertTrue(c.record().species().equals(SnowshoeHareEntity.ID) && c.record().cause().equals("player"), "record " + c.record());
                c.interact(p, InteractionHand.MAIN_HAND);
                ItemStack whole = ItemStack.EMPTY;
                for (ItemStack s : p.getInventory().items) if (s.is(EcosystemRegistry.WHOLE_GAME.get())) whole = s;
                CarcassRecord r = WholeGameItem.record(whole);
                h.assertTrue(r != null && r.species().equals(SnowshoeHareEntity.ID), "whole_game record " + r);
                h.assertTrue(r.weightG() >= 1300 && r.weightG() <= 1600, "weight " + r.weightG() + " g");
                Fresh f = whole.get(YoiComponents.FRESH.get());
                h.assertTrue(f != null && f.state() == FoodState.RAW, "fresh " + f);
                // Unopened: k 0.6 on lean meat's 6 days raw at 5 °C.
                h.assertTrue(Math.abs(f.lifeDh() - 6 * 24 / 0.7 * 0.6) < 1e-6, "life " + f.lifeDh());
                h.assertTrue(c.isRemoved(), "the carcass entity should be taken up");
                h.succeed();
            });
        });
    }

    /** The passive moose: with {@code [ecosystem] moose_huntable} off (every preset until M6), player hits do nothing. */
    static void mooseIgnoresDamage(GameTestHelper h) {
        h.assertTrue(!EcoConfig.mooseHuntable(), "the flag should be off on Standard");
        MooseEntity moose = h.spawn(EcosystemRegistry.MOOSE.get(), AT);
        moose.setScentEnabled(false);
        ServerPlayer p = player(h);
        p.moveTo(moose.getX() + 2, moose.getY(), moose.getZ(), 0, 0);
        float hp = moose.getHealth();
        h.runAfterDelay(5, () -> {
            for (int i = 0; i < 5; i++) {
                h.assertFalse(moose.hurt(h.getLevel().damageSources().playerAttack(p), 1000f), "the hit took");
            }
            h.runAfterDelay(5, () -> {
                h.assertTrue(moose.isAlive() && !moose.isRemoved(), "the moose died");
                h.assertTrue(moose.getHealth() == hp, "health " + moose.getHealth() + " of " + hp);
                h.assertTrue(h.getLevel().getEntitiesOfClass(CarcassEntity.class, moose.getBoundingBox().inflate(8),
                        c -> c.record().species().equals(MooseEntity.ID)).isEmpty(), "a moose carcass appeared");
                h.assertTrue(moose.awareness().max() >= 99, "a hit should alarm it, awareness " + moose.awareness().max());
                h.succeed();
            });
        });
    }

    /**
     * Ready for development M2.5 test 7, {@code ecosystem/slice_moose_flees}: the moose flees a player who closes to
     * 40 m and never charges. The moose faces the trail the player walks in on (its blind cone behind would leave
     * only its ears, 40 m) from 80 m at 3 m/s; the player stops at 40 m and the moose must be fleeing
     * by then, and over the next 15 s it only opens the gap.
     */
    static void mooseFlees(GameTestHelper h) {
        MooseEntity moose = h.spawn(EcosystemRegistry.MOOSE.get(), pad(h, 2, BANK_Y));
        moose.setScentEnabled(false);
        Vec3 start = moose.position();
        double groundY = start.y;
        force(h, start, 6, true);
        ServerPlayer p = player(h);
        ThreatSources.addForTest(p, moose);
        p.moveTo(start.x - 80, groundY + 1, start.z, -90, 0);
        double[] fledAt = {-1};
        double[] closest = {Double.MAX_VALUE};
        long[] fleeTick = {-1};
        StringBuilder track = new StringBuilder();
        h.onEachTick(() -> {
            double d = flat(p.position(), moose.position());
            closest[0] = Math.min(closest[0], d);
            if (moose.state() == AnimalState.FLEE && fledAt[0] < 0) {
                fledAt[0] = d;
                fleeTick[0] = h.getTick();
            }
            if (fledAt[0] < 0 && moose.awareness().max() < 30) {
                // A moose with its head up toward the trail: it faces the way the player comes from.
                float yaw = (float) com.yearofice.ecosystem.ai.Sensing.bearing(moose.position(), p.position());
                moose.setYRot(yaw);
                moose.setYHeadRot(yaw);
                moose.setYBodyRot(yaw);
            }
            if (d > 39.8 && fledAt[0] < 0) {
                p.moveTo(p.getX() + 0.15, groundY + 1, p.getZ(), p.getYRot(), 0);
            } else if (fledAt[0] < 0 && h.getTick() > 600) {
                h.fail("the moose did not flee a player at 40 m; state " + moose.state() + ", awareness " + moose.awareness().max()
                        + " | cover " + com.yearofice.ecosystem.ai.Sensing.cover(h.getLevel(), moose.getEyePosition(), p.getEyePosition())
                        + " yaw " + moose.getYHeadRot() + " bearing " + com.yearofice.ecosystem.ai.Sensing.bearing(moose.getEyePosition(), p.position())
                        + " near " + ThreatSources.near(h.getLevel(), moose.getEyePosition(), 60).size() + " d " + d
                        + " posture " + ThreatSources.posture(p, h.getLevel().getGameTime()) + " senses " + moose.species().senses()
                        + " activity " + moose.getBrain().getActiveNonCoreActivity() + " blocker " + blocker(h, moose.getEyePosition(), p.getEyePosition()));
            }
            if (fledAt[0] >= 0 && (h.getTick() - fleeTick[0]) % 20 == 0) {
                Vec3 m = moose.position().subtract(start);
                track.append(String.format(" [%d: %.1f %.1f %.1f %s %s]", h.getTick() - fleeTick[0], m.x, m.y, m.z, moose.moveHint(), moose.getNavigation().isDone()));
            }
            if (fledAt[0] >= 0 && h.getTick() - fleeTick[0] >= 300) {
                h.assertTrue(fledAt[0] >= 39.5, "fled only when the player was at " + fledAt[0] + " m");
                h.assertTrue(closest[0] >= 39.5, "the moose came to " + closest[0] + " m: it must never close");
                h.assertTrue(d >= fledAt[0] + 15, "gap " + d + " m after 15 s of flight from " + fledAt[0] + " | state " + moose.state()
                        + " covered " + moose.fleeCoveredM() + " y " + moose.getY() + " from " + start.y + " onGround " + moose.onGround()
                        + " speed " + moose.getDeltaMovement().horizontalDistance() * 20 + " nav " + moose.getNavigation().isDone() + " " + moose.fleeDebug() + track);
                ThreatSources.removeForTest(p);
                force(h, start, 6, false);
                unpad(h, 2, BANK_Y);
                h.succeed();
            }
        });
    }

    /**
     * {@code ecosystem/no_monsters_full_night} (Minecraft engine › No monsters): a December night is 17 game hours
     * of dark. The test makes one spawn attempt per game minute of that night, 1,020 in all, for every type the
     * biome's monster and creature lists name plus the custom spawners' phantom, pillager and wandering trader,
     * through the same {@code finalizeSpawn} and {@code addFreshEntity} calls the natural spawner makes, and a
     * spawn-egg spawn of each. The counter over {@code EntityJoinLevelEvent} must stay at zero, and no
     * {@code minecraft:} mob may stand in the area. (The mod has no biomes of its own yet; the test level's biome
     * stands in until the World department's biome list lands.)
     */
    static void noMonstersFullNight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(AT);
        List<EntityType<?>> types = new ArrayList<>();
        for (MobCategory cat : new MobCategory[] {MobCategory.MONSTER, MobCategory.CREATURE, MobCategory.AMBIENT}) {
            for (MobSpawnSettings.SpawnerData d : level.getBiome(base).value().getMobSettings().getMobs(cat).unwrap()) types.add(d.type);
        }
        types.add(EntityType.PHANTOM);
        types.add(EntityType.PILLAGER);
        types.add(EntityType.WANDERING_TRADER);
        types.add(EntityType.ZOMBIE);
        types.add(EntityType.WOLF);
        int before = NoVanillaMobs.JOINED.get();
        int[] attempts = {0};
        h.onEachTick(() -> {
            for (int i = 0; i < 6 && attempts[0] < 17 * 60; i++, attempts[0]++) {
                EntityType<?> t = types.get(attempts[0] % types.size());
                BlockPos at = base.offset(level.random.nextInt(9) - 4, 0, level.random.nextInt(9) - 4);
                Entity e = t.create(level);
                if (e instanceof Mob m) {
                    m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
                    level.addFreshEntityWithPassengers(m);
                }
                if (attempts[0] % 60 == 0) t.spawn(level, at, MobSpawnType.SPAWN_EGG);
            }
            if (attempts[0] >= 17 * 60) {
                h.assertTrue(NoVanillaMobs.JOINED.get() == before, (NoVanillaMobs.JOINED.get() - before) + " vanilla mobs joined the level");
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32),
                        m -> BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace().equals("minecraft"));
                h.assertTrue(mobs.isEmpty(), mobs.size() + " minecraft mobs stand in the area");
                h.succeed();
            }
        });
    }

    /**
     * Bestiary › Dressing by size class: a hare dresses (gut 3, skin 5, joint 5) in 13 game minutes, a 52-tick hold
     * at the Work rate, into 9 units of meat that keep the carcass's freshness, a pelt and sinew.
     */
    static void butcherHare(GameTestHelper h) {
        SnowshoeHareEntity hare = h.spawn(EcosystemRegistry.SNOWSHOE_HARE.get(), AT);
        h.runAfterDelay(2, () -> {
            CarcassRecord r = DeathPipeline.record(hare, hare.species(), "test", com.yearofice.ecosystem.clock.SliceClock.minuteD(h.getLevel()));
            h.assertTrue(Butchering.holdTicks(r) == 52, "hold " + Butchering.holdTicks(r) + " ticks");
            ServerPlayer p = player(h);
            ItemStack whole = WholeGameItem.of(r);
            p.setItemInHand(InteractionHand.MAIN_HAND, whole);
            whole.finishUsingItem(h.getLevel(), p);
            int meat = 0, pelt = 0, sinew = 0;
            boolean fresh = true;
            for (ItemStack s : p.getInventory().items) {
                if (s.is(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get())) {
                    meat += s.getCount();
                    Fresh f = s.get(YoiComponents.FRESH.get());
                    fresh &= f != null && f.state() == FoodState.RAW && Math.abs(f.lifeDh() - 6 * 24 / 0.7) < 1e-6;
                }
                if (s.is(EcosystemRegistry.HARE_PELT.get())) pelt += s.getCount();
                if (s.is(EcosystemRegistry.SINEW.get())) sinew += s.getCount();
            }
            h.assertTrue(meat == 9 && pelt == 1 && sinew == 1, "meat " + meat + ", pelt " + pelt + ", sinew " + sinew);
            h.assertTrue(fresh, "meat should carry the carcass's freshness on its own raw life");
            boolean left = false;
            for (ItemStack s : p.getInventory().items) left |= s.is(EcosystemRegistry.WHOLE_GAME.get());
            h.assertTrue(!left, "the carcass should be used up");
            hare.discard();
            h.succeed();
        });
    }
}
