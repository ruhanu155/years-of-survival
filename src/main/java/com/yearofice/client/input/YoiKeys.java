package com.yearofice.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.yearofice.core.YearOfIce;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * {@code client.input.YoiKeys} (Input and keybinds): the 13 key mappings in the category "Year of Ice", all
 * rebindable. Only the messenger key acts in M2; the rest wait for their systems. On the first launch the three
 * vanilla keys our defaults share (L, X, C) are unbound once, and a marker file keeps a later rebind.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class YoiKeys {
    private YoiKeys() {}

    public static final String CATEGORY = "key.categories.yearofice";
    public static final Map<String, KeyMapping> KEYS = new LinkedHashMap<>();

    static {
        KeyTable.DEFAULTS.forEach((id, code) -> KEYS.put(id, new KeyMapping("key.yearofice." + id, InputConstants.Type.KEYSYM, code, CATEGORY)));
    }

    public static final KeyMapping MESSENGER = KEYS.get("messenger");
    public static final KeyMapping QUICK_EAT = KEYS.get("quick_eat");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) { KEYS.values().forEach(e::register); }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(YoiKeys::firstLaunchUnbinds);
    }

    static void firstLaunchUnbinds() {
        Path marker = FMLPaths.CONFIGDIR.get().resolve("yearofice-keys-first-launch.done");
        if (Files.exists(marker)) return;
        Minecraft mc = Minecraft.getInstance();
        for (KeyMapping k : mc.options.keyMappings) {
            if (KeyTable.UNBIND_ON_FIRST_LAUNCH.contains(k.getName())) k.setKey(InputConstants.UNKNOWN);
        }
        KeyMapping.resetMapping();
        mc.options.save();
        try {
            Files.writeString(marker, "The vanilla keys L, X and C were unbound once for Year of Ice's defaults.\n");
        } catch (java.io.IOException ex) {
            YearOfIce.LOG.warn("Could not write {}", marker, ex);
        }
    }
}
