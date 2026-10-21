package com.yearofice.client.weather;

import com.yearofice.core.YearOfIce;
import com.yearofice.env.weather.StrikePayload;
import com.yearofice.env.weather.WeatherRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** The strike payload on the client: a white flash that fades with distance, the crack at once, the thunder after the sound's travel time. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class StrikeFx {
    private StrikeFx() {}

    private record Thunder(BlockPos pos, int dueTick, float vol) {}

    private static final List<Thunder> PENDING = new ArrayList<>();
    private static float flash;
    private static int tick;

    public static void on(StrikePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        double d = Math.sqrt(mc.player.blockPosition().distSqr(p.pos()));
        flash = (float) Math.max(flash, Math.max(0.0, 1.0 - d / 3000.0) * p.power());
        mc.level.playLocalSound(p.pos(), WeatherRegistry.STRIKE.get(), SoundSource.WEATHER, (float) Math.max(0.2, 8 - d / 400), 1f, false);
        PENDING.add(new Thunder(p.pos(), tick + (int) Math.min(400, d / 17), (float) Math.max(0.3, 6 - d / 600)));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        tick++;
        flash = Math.max(0, flash - 0.08f);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        PENDING.removeIf(t -> {
            if (t.dueTick() > tick) return false;
            mc.level.playLocalSound(t.pos(), WeatherRegistry.THUNDER.get(), SoundSource.WEATHER, t.vol(), 1f, false);
            return true;
        });
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post e) {
        if (flash < 0.02f) return;
        int a = (int) (Math.min(1f, flash) * 200);
        e.getGuiGraphics().fill(0, 0, e.getGuiGraphics().guiWidth(), e.getGuiGraphics().guiHeight(), (a << 24) | 0xFFFFFF);
    }
}
