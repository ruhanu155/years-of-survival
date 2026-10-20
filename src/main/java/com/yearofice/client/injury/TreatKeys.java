package com.yearofice.client.injury;

import com.mojang.blaze3d.platform.InputConstants;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** The self-care key (default H, rebindable): opens {@link TreatRadial}. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class TreatKeys {
    private TreatKeys() {}

    public static final KeyMapping TREAT = new KeyMapping("key.yearofice.treat", InputConstants.Type.KEYSYM,
            InputConstants.KEY_H, "key.categories.yearofice");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) { e.register(TREAT); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (TREAT.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new TreatRadial());
        }
    }
}
