package com.yearofice.body;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.body.clock.BodyClock;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Collapse and the Endless SOS, as a stub (Minecraft development › Collapse, SOS and the flight). This slice runs
 * the state machine straight through in one call: it logs the flight, tells the player "SOS sent 14:22, day 143"
 * (a priority-3 {@code warn} line on the HUD bus, so it shows through the close clock and goes to the journal when
 * the messenger is not live),
 * sets the return body (BMI 19, Condition 80, warmth, water and energy full) and teleports the player to the
 * return point, the world spawn until the outpost bar exists.
 *
 * <p>Later, with M4: the 4 s collapse and {@code BodyCamera}, {@code SosFlightScreen} and the "why I left" line,
 * the 14-day closed-form jump with chunk catch-up, the {@code left_pack} block, the starting kit and the journal.
 * Hardcore here only ends the run as a spectator; {@code RunEndedScreen} and the run record are M4.
 */
public final class SosFlow {
    private SosFlow() {}

    public enum Cause {
        STARVATION("starvation"), HYPOTHERMIA("hypothermia"), CONDITION("condition"), DROWNING("drowning"),
        COMMAND("command"), FELL("fell"), MANUAL("manual");

        public final String id;

        Cause(String id) { this.id = id; }
    }

    public static final String FLIGHTS_KEY = "yearofice:flights_out";

    /** Starts (and, in this stub, finishes) the flight out. Ignored while one is already under way. */
    public static void trigger(ServerPlayer p, Cause cause) {
        BodyState s = BodyAttachments.of(p);
        if (s.sos != 0) return;
        long minute = BodyTicker.clock.minute(p.server);
        String when = BodyClock.hhmm(minute);
        int day = BodyClock.day(minute);
        YearOfIce.LOG.info("SOS: {} collapsed ({}) at {}, day {}, BMI {}, warmth {}; flight out (stub)",
                p.getGameProfile().getName(), cause.id, when, day, String.format("%.1f", s.bmi()),
                String.format("%.1f", s.warmth));
        s.sos = 1;
        HudBus.get().post(p, HudLine.of(HudLineType.WARN, "sos_sent", "body", HudArg.text(when), HudArg.of(day),
                HudArg.key("messenger.yearofice.cause." + cause.id)));

        if (p.serverLevel().getLevelData().isHardcore()) {
            p.setGameMode(GameType.SPECTATOR);
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "run_ended", "body"));
            s.sos = 2;
            return;
        }

        s.sos = 3;
        BodyModel.sosReturn(s, BodyConfig.tuning());
        teleportToReturnPoint(p);
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        p.resetFallDistance();
        countFlight(p);
        BodyTicker.sync(p, minute, true);
    }

    /** The return point: the gravel bar below the outpost once it exists; the overworld spawn until then. */
    public static void teleportToReturnPoint(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        p.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
    }

    static void countFlight(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(FLIGHTS_KEY, persisted.getInt(FLIGHTS_KEY) + 1);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static int flightsOut(ServerPlayer p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(FLIGHTS_KEY);
    }
}
