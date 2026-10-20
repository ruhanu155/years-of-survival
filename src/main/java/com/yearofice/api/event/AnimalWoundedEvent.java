package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Posted on {@code NeoForge.EVENT_BUS} by {@code hunt} when an arrow wounds an animal without killing it outright
 * (Blueprint › The hit-zone model; Wounds that last). The wound itself lives in {@code hunt.wound.WoundTracker}, which
 * also runs the flight, the bed and the blood trail; ecosystem listens to raise a wounded bear's aggression one step,
 * to name it "an old arrow wound" on first look and to count meetings toward Known.
 *
 * @param channel {@code VITALS}, {@code LIVER}, {@code GUT} or {@code LEG}
 * @param zone    the zone that decided the shot
 */
public final class AnimalWoundedEvent extends Event {
    private final LivingEntity animal;
    private final @Nullable ServerPlayer shooter;
    private final String channel, zone;

    public AnimalWoundedEvent(LivingEntity animal, @Nullable ServerPlayer shooter, String channel, String zone) {
        this.animal = animal;
        this.shooter = shooter;
        this.channel = channel;
        this.zone = zone;
    }

    public LivingEntity animal() { return animal; }
    public @Nullable ServerPlayer shooter() { return shooter; }
    public String channel() { return channel; }
    public String zone() { return zone; }
}
