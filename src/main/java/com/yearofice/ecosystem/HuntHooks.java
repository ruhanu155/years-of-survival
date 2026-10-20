package com.yearofice.ecosystem;

import com.yearofice.api.event.AnimalWoundedEvent;
import com.yearofice.api.event.CallSoundedEvent;
import com.yearofice.ecosystem.clock.SliceClock;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Ecosystem's side of the hunt lane's hooks (M6 lane B): {@code CallSoundedEvent} (each animal of a drawn species
 * within the call's effective range rolls {@code pRespond} and walks to the caller), and {@code AnimalWoundedEvent}
 * (the animal's aggression rises one step and it remembers "an old arrow wound"). Concealment is read in
 * {@code ai.Sensing}. Subscribed on the Forge bus by {@link EcosystemSystem}.
 */
public final class HuntHooks {
    private HuntHooks() {}

    @SubscribeEvent
    public static void onCall(CallSoundedEvent e) {
        Vec3 o = e.origin();
        double r = e.rangeM() * 1.5;
        List<YoiAnimal> near = e.level().getEntitiesOfClass(YoiAnimal.class, new AABB(o, o).inflate(r, 64, r));
        double hour = YoiAnimal.hourNow(e.level());
        for (YoiAnimal a : near) {
            double p = e.pRespond(a.speciesId(), a.position(), 1.0, hour, false, 1.0);
            if (p > 0 && a.getRandom().nextDouble() < p) a.respondToCall(o, e.level().getGameTime() + Math.round(2 * SliceClock.TICKS_PER_MINUTE * 60));
        }
    }

    /** Lane C's water-draw sample (RECONCILE 168): animals in their water act walk to the edge; bears gather at a salmon run. */
    @SubscribeEvent
    public static void onWaterDraw(com.yearofice.api.event.WaterDrawEvent e) {
        Vec3 o = Vec3.atCenterOf(e.edge());
        for (YoiAnimal a : e.level().getEntitiesOfClass(YoiAnimal.class, new AABB(o, o).inflate(64, 24, 64))) {
            boolean bear = a.speciesId().endsWith("bear") || a.speciesId().endsWith("grizzly");
            if (bear && e.runStrength() > 0.2 && a.driveState().hunger > 0.4) {
                e.draw(a.speciesId());
                a.drawnToWater(e.edge());
            } else if (a.wantsWater() && e.strength() > 0.3) {
                e.draw(a.speciesId());
                a.drawnToWater(e.edge());
            }
        }
    }

    @SubscribeEvent
    public static void onWounded(AnimalWoundedEvent e) {
        if (e.animal() instanceof YoiAnimal a) a.noteArrowWound();
    }
}
