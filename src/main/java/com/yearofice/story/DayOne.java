package com.yearofice.story;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.WeatherModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Blueprint › Start site › Day 1 rules, the world half of the guided first night: on a fresh world with the guided
 * night on, the clock is set once to 12:00 on May 1 ("The player wakes at 12:00 on May 1"), through
 * {@link ClockService#setNow}; and May 1's weather is fixed at cloudy, 7 °C high, −3 °C low, wind 2 m/s, no
 * precipitation, with the chain starting from it, through {@link EnvService#fixFirstDay} on every start of such a
 * world. The decision is saved in SavedData {@code yearofice_story} so it is taken once, when the world is new; a
 * world created before this, or with the guided night off, keeps its clock and its drawn weather.
 *
 * <p>Not done here (they are other layers' Day 1 rules): the clock at half speed until the first fire
 * ({@link DayOneClock}, the Clock's {@code day_one} rate), no predator in the Active tier near the outpost until 06:00 (Ecosystem), hypothermia held at shivering (Body).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class DayOne {
    private DayOne() {}

    /** 12:00 on May 1 of year 1, in game deciseconds. */
    public static final long WAKE_DS = 12 * 60 * Calendar.DS_PER_MINUTE;

    /** What the last start found and did (the GameTest reads it; parallel tests may move the clock later). */
    public record Start(boolean fresh, boolean dayOne, long clockDsAfter) {}

    private static volatile Start last;

    public static Start last() { return last; }

    /** SavedData {@code yearofice_story}, version 1: whether the day-1 decision was taken, and its answer. */
    public static final class Data extends SavedData {
        public static final String ID = "yearofice_story";
        boolean decided;
        boolean dayOne;

        public boolean decided() { return decided; }
        public boolean dayOne() { return dayOne; }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.decided = tag.getBoolean("day_one_decided");
            d.dayOne = tag.getBoolean("day_one");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("day_one_decided", decided);
            tag.putBoolean("day_one", dayOne);
            return tag;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Data::new, Data::load, null), Data.ID);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { apply(e.getServer()); }

    /** Decides once, on a fresh world; then fixes day 1's weather on every start of a day-1 world. */
    static void apply(MinecraftServer server) {
        if (!ClockService.installed()) return;
        Data d = data(server);
        ClockService clock = ClockService.get();
        boolean fresh = !d.decided && clock.nowDs() == 0;
        if (!d.decided) {
            d.decided = true;
            d.dayOne = fresh && StoryConfig.enabled();
            d.setDirty();
            if (d.dayOne) clock.setNow(WAKE_DS);
        }
        if (d.dayOne && EnvService.installed()) {
            for (ServerLevel l : server.getAllLevels()) EnvService.get().fixFirstDay(l, WeatherModel.FixedDay.BLUEPRINT_DAY_1);
        }
        last = new Start(fresh, d.dayOne, clock.nowDs());
        if (fresh) YearOfIce.LOG.info("[story] day 1: {} ({})", Calendar.format(clock.nowDs()), d.dayOne ? "woke at noon, weather fixed" : "guided night off");
    }
}
