package com.yearofice.ecosystem.livestock;

import com.yearofice.ecosystem.YoiAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Livestock arriving by supply drop (Blueprint: goats and hens come in a supply drop): grown stock set down beside the crate. */
public final class LivestockDrop {
    private LivestockDrop() {}

    /** Sets {@code n} adult animals of {@code entityId} down around {@code at}; returns how many were made (hens and goats are mostly female). */
    public static int deliver(ServerLevel level, BlockPos at, String entityId, int n) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entityId)).orElse(null);
        int made = 0;
        for (int i = 0; type != null && i < n; i++) {
            var e = type.create(level);
            if (!(e instanceof YoiAnimal a) || !(e instanceof Livestock l)) continue;
            a.moveTo(at.getX() + 2.5 + (i % 3) * 0.9, at.getY(), at.getZ() + 0.5 + (i / 3) * 0.9, 0, 0);
            l.core().female = i % 5 != 4;
            if (l.core().female) l.core().lastBirthDay = com.yearofice.api.clock.ClockService.get().absDay() - 30;
            level.addFreshEntity(a);
            made++;
        }
        return made;
    }
}
