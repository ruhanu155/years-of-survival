package com.yearofice.body.injury;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.injury.ProblemClock;
import com.yearofice.sim.body.injury.Stumble;
import com.yearofice.sim.body.injury.Treatments;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The body tick's injury half: bleeding and ended problems per step, and the rough-ground stumble roll. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class InjuryTick {
    private InjuryTick() {}

    public static final TagKey<Block> ROUGH_GROUND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "rough_ground"));

    /** Called from {@code BodyTicker.step}/{@code stepPlan} after the model: ends old problems, then bleeds. */
    public static void step(BodyState s, long start, int minutes, BodyTuning t) {
        long end = start + minutes;
        s.parts = ProblemClock.expire(s.parts, end);
        s.parts = com.yearofice.sim.body.injury.Infection.advance(s.parts, start, minutes, t.bleedScale());
        s.parts = Treatments.bleed(s.parts, minutes, t.bleedScale(), end);
    }

    private static final Map<UUID, double[]> WALKED = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !BodyTicker.stepped(p) || !p.onGround()) return;
        double[] w = WALKED.computeIfAbsent(p.getUUID(), k -> new double[] {p.getX(), p.getZ(), 0});
        double d = Math.hypot(p.getX() - w[0], p.getZ() - w[1]);
        w[0] = p.getX();
        w[1] = p.getZ();
        if (d > 5) return; // a teleport, not a walk
        BlockPos below = p.blockPosition().below();
        if (!p.level().getBlockState(below).is(ROUGH_GROUND)) return;
        w[2] += d;
        if (w[2] < 100) return;
        w[2] -= 100;
        BodyState b = BodyAttachments.of(p);
        long now = BodyTicker.clock.minute(p.server);
        boolean wet = p.isInWaterOrRain() || b.wetness() > 0.5;
        boolean loaded = false;
        try {
            var load = com.yearofice.api.item.LoadService.get();
            loaded = load.kg(p) > 0.8 * load.maxKg(p);
        } catch (IllegalStateException notInstalled) {
            // no load service yet
        }
        boolean dark = p.level().getMaxLocalRawBrightness(p.blockPosition()) < 5;
        boolean stick = InjuryService.holdsWalkingStick(p);
        InjuryService.walk100m(p, new Stumble.Factors(p.isSprinting(), wet, loaded, b.energy <= 3, dark, stick,
                InjuryService.injuredFactor(b.parts, now, stick), false));
    }
}
