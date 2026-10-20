package com.yearofice.api.event;

import com.yearofice.sim.hunt.Calls;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/**
 * Posted on {@code NeoForge.EVENT_BUS} by {@code hunt} when a player sounds a call (moose call, squeak call;
 * Blueprint › Calling animals). Ecosystem listens: for each animal of a species in {@link #baseP()} within range it
 * rolls {@link #pRespond} and, on a hit, queues the {@code respond_to_call} activity toward {@link #origin()}.
 *
 * @param callId   call item id, for example {@code moose_call}
 * @param sound    {@code cow_moan}, {@code bull_grunt} or {@code distress_squeak}
 * @param quality  q, 0.3 to 1.2
 * @param rangeM   the call's own range before wind
 * @param flat     flat falloff (squeaks) instead of linear
 * @param baseP    base odds p_s by species id
 * @param windToX  the unit vector the wind blows toward, x
 * @param windToZ  and z
 * @param windMs   wind speed (m/s)
 */
public final class CallSoundedEvent extends Event {
    private final ServerLevel level;
    private final ServerPlayer caller;
    private final Vec3 origin;
    private final String callId, sound;
    private final double quality, rangeM, windToX, windToZ, windMs;
    private final boolean flat;
    private final Map<String, Double> baseP;

    public CallSoundedEvent(ServerLevel level, ServerPlayer caller, Vec3 origin, String callId, String sound, double quality, double rangeM,
                            boolean flat, Map<String, Double> baseP, double windToX, double windToZ, double windMs) {
        this.level = level;
        this.caller = caller;
        this.origin = origin;
        this.callId = callId;
        this.sound = sound;
        this.quality = quality;
        this.rangeM = rangeM;
        this.flat = flat;
        this.baseP = Map.copyOf(baseP);
        this.windToX = windToX;
        this.windToZ = windToZ;
        this.windMs = windMs;
    }

    public ServerLevel level() { return level; }
    public ServerPlayer caller() { return caller; }
    public Vec3 origin() { return origin; }
    public String callId() { return callId; }
    public String sound() { return sound; }
    public double quality() { return quality; }
    public double rangeM() { return rangeM; }
    public boolean flat() { return flat; }
    public Map<String, Double> baseP() { return baseP; }
    public double windMs() { return windMs; }

    /** R_eff at a listener: the range times the wind direction factor (ground and air factors are the listener's side to add), capped at 1.5x. */
    public double windToX() { return windToX; }
    public double windToZ() { return windToZ; }

    public double reffAt(Vec3 listener) {
        double dx = listener.x - origin.x, dz = listener.z - origin.z, d = Math.sqrt(dx * dx + dz * dz);
        double cos = d < 1e-6 ? 0 : (dx * windToX + dz * windToZ) / d;
        double wind = windMs < 1.0 ? 1.0 : Calls.windFactor(cos);
        return Calls.effectiveRange(rangeM, wind, 1.0, 1.0);
    }

    /**
     * The chance that {@code speciesId}'s animal at {@code listener} answers, or 0 when it is not a species this call draws.
     *
     * @param season S_s(date): for the cow moan the rut factor; 1 for a call with no season
     */
    public double pRespond(String speciesId, Vec3 listener, double season, double hour, boolean shy, double director) {
        Double p = baseP.get(speciesId);
        if (p == null) return 0;
        double d = Math.sqrt(listener.distanceToSqr(origin));
        return Calls.pRespond(p, season, hour, quality, shy, director, d, reffAt(listener), flat ? Calls.Falloff.FLAT : Calls.Falloff.LINEAR);
    }
}
