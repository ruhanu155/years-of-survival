package com.yearofice.story.debug;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.fire.FireStateEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.time.Rate;
import com.yearofice.story.DayOneClock;
import com.yearofice.story.Onboarding;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * {@code clock/day1_half_speed_until_fire} (Blueprint › Start site › Day 1 rules). It moves the shared clock, so it runs
 * in a batch of its own ({@value #BATCH}), puts the clock back when done and clears any pinned rate after the batch,
 * so no other test ever sees its clock. The bot is detached: only this test asks rates for it.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class DayOneGameTests {
    private DayOneGameTests() {}

    static final String BATCH = "m2_5_clock_day1";
    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static List<TestFunction> dayOneTests() {
        return List.of(new TestFunction(BATCH, "yearofice:clock/day1_half_speed_until_fire", TEMPLATE, 200, 0, true,
                DayOneGameTests::halfSpeedUntilFire));
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        ClockService.get().forceRate(null);
        if (ClockService.get().dayLengthMin() != 24) ClockService.get().setDayLength(24);
    }

    static ServerPlayer bot(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 1, 1)));
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    /**
     * A guided-night bot with no fire at 13:00 on May 1 runs the world at half the World rate (15 ds a tick on the
     * 24-minute day, against 30); the rule ends at the first fire (a {@code FireStateEvent} at the bot's ring) and
     * at 22:00 without one; a player not on the night never asks it.
     */
    static void halfSpeedUntilFire(GameTestHelper h) {
        ClockService c = ClockService.get();
        long before = c.nowDs();
        c.forceRate(null);
        if (c.dayLengthMin() != 24) c.setDayLength(24);
        c.setNow(13 * 60 * 600L);
        ServerPlayer p = bot(h, "day-one-bot"), late = bot(h, "no-night-bot"), cut = bot(h, "cutoff-bot");
        cut.moveTo(cut.getX(), cut.getY() + 40, cut.getZ()); // out of reach of the bot's fire
        Onboarding.onJoin(p, 13 * 60);
        Onboarding.onJoin(cut, 13 * 60);
        Onboarding.skip(late, 13 * 60);
        BlockPos ring = h.absolutePos(new BlockPos(1, 1, 0));
        int[] tick = {0};
        long[] mark = {0, 0, 0, 0};
        h.onEachTick(() -> {
            int t = tick[0]++;
            long now = c.nowDs();
            if (t == 2) mark[0] = now;
            if (t == 22) {
                mark[1] = now;
                // The first fire: the tinder catches at the bot's ring, 1 m away.
                NeoForge.EVENT_BUS.post(new FireStateEvent(h.getLevel(), ring, FireState.EMBER, FireState.TINDER,
                        new FireHeat(ring.getX(), ring.getY(), ring.getZ(), FireState.TINDER, FireSetting.LOW, 10, 1), "test"));
            }
            if (t == 24) mark[2] = now;
            if (t == 44) mark[3] = now;
            if (t < 44) {
                boolean asked = DayOneClock.ask(p, c.nowMinute());
                if (t < 22 && !asked) h.fail("no fire at " + c.nowMinute() + ": Day 1 should be asked");
                if (t >= 22 && asked) h.fail("the fire is lit: Day 1 should not be asked");
                if (DayOneClock.ask(late, c.nowMinute())) h.fail("a player not on the guided night asked Day 1");
                if (t == 10 && c.rate() != Rate.DAY_ONE) h.fail("world rate " + c.rate().id() + ", want day_one");
                if (t == 30 && c.rate() != Rate.WORLD) h.fail("world rate " + c.rate().id() + " after the fire, want world");
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(tick[0] > 44, "running " + tick[0]);
            try {
                long half = mark[1] - mark[0], full = mark[3] - mark[2];
                h.assertTrue(half == 20L * 15, "no fire: 20 ticks moved " + half + " ds, want 300 (half of World's 30 a tick)");
                h.assertTrue(full == 20L * 30, "after the fire: 20 ticks moved " + full + " ds, want 600");
                // 22:00 ends it without a fire; the cutoff bot never lit one.
                h.assertTrue(DayOneClock.applies(cut, 21 * 60 + 59), "21:59 with no fire is still half speed");
                h.assertTrue(!DayOneClock.applies(cut, 22 * 60), "22:00 ends half speed");
                h.assertTrue(!DayOneClock.applies(cut, 1440 + 13 * 60), "day 2 is never half speed");
            } finally {
                Onboarding.forget(p);
                Onboarding.forget(late);
                Onboarding.forget(cut);
                c.setNow(before);
            }
        });
    }
}
