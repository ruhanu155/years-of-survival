package com.yearofice.client.sound;

import com.yearofice.api.clock.ClientTime;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.hud.MusicPlan;
import com.yearofice.sound.YoiSounds;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * Music tied to Spirits (Art and audio production › Music tied to Spirits): vanilla music is replaced through
 * {@link SelectMusicEvent}, and {@link MusicPlan} says when one of our pieces may play. Danger (the close clock or
 * any danger cue) fades music out over 3 s and keeps it off for 5 real minutes. The three inputs the body and fire
 * systems own are suppliers set by them: Spirits (default 50, Steady), distance to the nearest lit fire (default far)
 * and the danger flag.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class MusicDirector {
    private MusicDirector() {}

    public static volatile Supplier<Float> spirits = () -> 50f;
    public static volatile DoubleSupplier fireDistanceM = () -> 999;
    public static volatile Supplier<Boolean> danger = () -> false;

    private static final Random RNG = new Random();
    private static Piece playing;
    private static long tick, lastEnd = -1_000_000, lastDanger = -1_000_000;

    /** Dawn window: the first 90 game minutes after 05:00 (the clock's dawn at 64N varies; the Look lane refines it). */
    static boolean dawnNow() {
        double m = ClientTime.minuteD() % 1440;
        return m >= 5 * 60 && m < 6.5 * 60;
    }

    @SubscribeEvent
    public static void select(SelectMusicEvent e) { e.setMusic(null); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        tick++;
        if (mc.player == null || mc.level == null) { stop(); return; }
        boolean inDanger = danger.get();
        if (inDanger) lastDanger = tick;
        if (playing != null) {
            if (inDanger) playing.fadeOut();
            if (playing.isStopped()) { playing = null; lastEnd = tick; }
            return;
        }
        MusicPlan.Piece p = MusicPlan.choose(spirits.get(), dawnNow(), fireDistanceM.getAsDouble(), tick - lastEnd,
                tick - lastDanger, inDanger, RNG.nextDouble());
        if (p == MusicPlan.Piece.NONE) return;
        SoundEvent ev = switch (p) {
            case DAWN -> YoiSounds.MUSIC_DAWN.get();
            case DAY -> YoiSounds.MUSIC_DAY.get();
            default -> YoiSounds.MUSIC_FIRE.get();
        };
        playing = new Piece(ev);
        mc.getSoundManager().play(playing);
    }

    private static void stop() {
        if (playing != null) { Minecraft.getInstance().getSoundManager().stop(playing); playing = null; }
    }

    /** A piece as a streamed, non-positional sound with a 3 s fade-out. */
    static final class Piece extends AbstractTickableSoundInstance {
        private int fade = -1;

        Piece(SoundEvent ev) {
            super(ev, SoundSource.MUSIC, RandomSource.create());
            this.looping = false;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.7f;
        }

        void fadeOut() { if (fade < 0) fade = MusicPlan.FADE_TICKS; }

        @Override
        public void tick() {
            if (fade >= 0) {
                volume = 0.7f * fade / MusicPlan.FADE_TICKS;
                if (fade-- == 0) stop();
            }
        }
    }
}
