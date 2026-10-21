package com.yearofice.hunt.sign;

import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.sim.ecosystem.Drives;
import com.yearofice.sim.hunt.Sign;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The one hook from the ecosystem's routine tick: a walking animal presses a print every stride, a feeding bout
 * leaves browse where it starts and scat where it ends, and a rest leaves a bed. Species without a file leave none.
 */
public final class SignLaying {
    private SignLaying() {}

    private static final class Mem {
        Vec3 last;
        boolean feeding, resting;
    }

    private static final Map<YoiAnimal, Mem> MEM = new WeakHashMap<>();

    /** Test groups whose animals may lay sign; any other grouped animal (a chase, a sight line) lays none, so a runway keeps only its floor. */
    private static final java.util.Set<String> OPT_IN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void optIn(String group) { OPT_IN.add(group); }

    public static void tick(YoiAnimal a, ServerLevel level) {
        if (a.tickCount % 10 != 0) return;
        if (a.testGroup() != null && !OPT_IN.contains(a.testGroup())) return;
        SignData d = SignData.of(a.speciesId());
        if (d == null) return;
        Mem m = MEM.computeIfAbsent(a, k -> new Mem());
        Vec3 here = a.position();
        if (m.last == null) m.last = here;
        int move = a.moveHint();
        BlockPos foot = a.blockPosition();
        if (move == YoiAnimal.MOVE_WALK || move == YoiAnimal.MOVE_RUN) {
            double dx = here.x - m.last.x, dz = here.z - m.last.z;
            if (Math.hypot(dx, dz) >= d.trackEveryM()) {
                SignService.lay(level, foot, Sign.Kind.TRACK, d.species(), dx, dz, move == YoiAnimal.MOVE_RUN ? 2 : 1);
                m.last = here;
            }
        } else {
            m.last = here;
        }
        boolean feeding = move == YoiAnimal.MOVE_FEED;
        if (feeding && !m.feeding && d.browse()) SignService.lay(level, foot, Sign.Kind.BROWSE, d.species(), 0, 0, 0);
        if (!feeding && m.feeding && d.scat()) SignService.lay(level, foot, Sign.Kind.SCAT, d.species(), 0, 0, 0);
        m.feeding = feeding;
        boolean resting = move == YoiAnimal.MOVE_IDLE && a.species().hasDrives() && a.driveState().act == Drives.Act.BED;
        if (resting && !m.resting && d.bed()) SignService.lay(level, foot, Sign.Kind.BED, d.species(), 0, 0, 0);
        m.resting = resting;
    }
}
