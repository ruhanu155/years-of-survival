package com.yearofice.ecosystem.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.sim.ecosystem.AnimalState;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * The Brain every slice animal runs (Minecraft development › Brain: sensors, memories and activities): the three
 * sense sensors, the {@code threat} and {@code look_target} memories, the core {@link Think} behaviour that
 * decays awareness and picks the state, and one activity per state of the slice's state machine, each holding a
 * {@link Act} behaviour that moves the body. No vanilla goal, sensor or behaviour runs.
 */
public final class YoiBrain {
    private YoiBrain() {}

    public static List<MemoryModuleType<?>> memories() {
        return List.of(EcosystemRegistry.THREAT.get(), EcosystemRegistry.LOOK_TARGET.get());
    }

    public static List<SensorType<? extends Sensor<? super YoiAnimal>>> sensors() {
        return List.of(EcosystemRegistry.SIGHT.get(), EcosystemRegistry.HEARING.get(), EcosystemRegistry.SCENT.get());
    }

    public static Brain<YoiAnimal> makeBrain(Brain<YoiAnimal> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new Think()));
        for (AnimalState s : AnimalState.values()) brain.addActivity(activity(s), 10, ImmutableList.of(new Act(s)));
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(activity(AnimalState.ROUTINE));
        brain.useDefaultActivity();
        return brain;
    }

    public static Activity activity(AnimalState s) {
        return switch (s) {
            case ROUTINE -> EcosystemRegistry.ROUTINE.get();
            case ALERT -> EcosystemRegistry.ALERT.get();
            case FREEZE -> EcosystemRegistry.FREEZE.get();
            case FLEE -> EcosystemRegistry.FLEE.get();
        };
    }

    static final int FOREVER = 1_000_000;

    /** Core: awareness decay, the threat memory and the state transition, every tick. */
    static final class Think extends Behavior<YoiAnimal> {
        Think() { super(ImmutableMap.of(), FOREVER, FOREVER); }

        @Override
        protected boolean canStillUse(ServerLevel level, YoiAnimal a, long t) { return true; }

        @Override
        protected void tick(ServerLevel level, YoiAnimal a, long t) { a.think(level); }

        @Override
        protected boolean timedOut(long t) { return false; }
    }

    /** One state's movement; runs while its activity is the active one. */
    static final class Act extends Behavior<YoiAnimal> {
        final AnimalState state;

        Act(AnimalState state) {
            super(ImmutableMap.of(), FOREVER, FOREVER);
            this.state = state;
        }

        @Override
        protected void start(ServerLevel level, YoiAnimal a, long t) { a.enterState(state); }

        @Override
        protected boolean canStillUse(ServerLevel level, YoiAnimal a, long t) { return a.getBrain().isActive(activity(state)); }

        @Override
        protected void tick(ServerLevel level, YoiAnimal a, long t) { a.tickState(level, state); }

        @Override
        protected boolean timedOut(long t) { return false; }
    }
}
