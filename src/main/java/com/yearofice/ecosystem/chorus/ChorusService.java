package com.yearofice.ecosystem.chorus;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.small.LifeConfig;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.life.Chorus;
import com.yearofice.sim.time.Calendar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The dawn chorus at runtime (Birds and the air › The dawn chorus): C = v(month) x g(t - dawn) x w(weather), mixed from
 * at most {@code chorus_voices} voices. Every 20 seconds a player in a chorus hears one of three beds, louder with C.
 * Original Year of Ice code.
 */
public final class ChorusService {
    private ChorusService() {}

    /** Overrides for the debug command and the GameTests: NaN follows the clock and the weather. */
    public static volatile double pinnedVolume = Double.NaN;
    /** The last volume and voices computed for any player, for {@code /yoi chorus}. */
    public static volatile double lastVolume;
    public static volatile int lastVoices;
    public static volatile long played;

    public static void clear() {
        pinnedVolume = Double.NaN;
        lastVolume = 0;
        lastVoices = 0;
        played = 0;
    }

    public static int sunriseMin(ServerLevel level) {
        long absDay = Calendar.absDayOfMinute(SliceClock.minute(level));
        return ClockService.installed() ? ClockService.get().sun().sunriseMinute(absDay) : 330;
    }

    /** The chorus volume at a player's place now. */
    public static double volume(ServerLevel level, ServerPlayer p) {
        if (!Double.isNaN(pinnedVolume)) return pinnedVolume;
        long minute = SliceClock.minute(level);
        int doy = Calendar.dayOfYearOfMinute(minute);
        boolean rain = false;
        double wind = 0, air = 10;
        if (EnvService.installed()) {
            EnvService env = EnvService.get();
            Precip pr = env.precip(level, p.blockPosition());
            rain = pr == Precip.RAIN || pr == Precip.DRIZZLE;
            wind = env.wind(level, p.blockPosition()).ms();
            air = env.airC(level, p.blockPosition());
        }
        double w = Chorus.weather(rain, wind, air, Calendar.month(doy), false);
        return Chorus.volume(doy, Calendar.minuteOfDayOfMinute(minute), sunriseMin(level), w);
    }

    public static SoundEvent bed(double volume, boolean whiteNights) {
        if (whiteNights) return ChorusSounds.WHITE_NIGHT.get();
        return volume < 0.35 ? ChorusSounds.SPARSE.get() : ChorusSounds.FULL.get();
    }

    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 400 != 97) return;
        ServerLevel level = e.getServer().overworld();
        int doy = Calendar.dayOfYearOfMinute(SliceClock.minute(level));
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !level.canSeeSky(p.blockPosition())) continue;
            double v = volume(level, p);
            lastVolume = v;
            lastVoices = Chorus.voices(v, LifeConfig.tuning().chorusVoices());
            if (lastVoices == 0) continue;
            level.playSound(null, p.blockPosition(), bed(v, Chorus.whiteNights(doy)), SoundSource.AMBIENT, (float) Math.min(1.0, 0.3 + v), 1f);
            played++;
        }
    }
}
