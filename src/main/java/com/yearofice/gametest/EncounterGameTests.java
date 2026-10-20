package com.yearofice.gametest;

import com.yearofice.api.event.EncounterStateEvent;
import com.yearofice.api.event.StaggerEvent;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.injury.InjuryService;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sound.EncounterBridge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** Encounter hand clips (M4 lane D2): the server posts StaggerEvent on a trip and the bridge maps events to clips. */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class EncounterGameTests {
    private EncounterGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> encounterTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(new TestFunction("m4_encounter", "yearofice:encounter/stagger_event_fires", LayerGameTests.TEMPLATE, 100, 0, true,
                EncounterGameTests::staggerFires));
        return TestFilter.apply(out);
    }

    static void staggerFires(GameTestHelper h) {
        ServerPlayer p = LayerGameTests.player(h, "encounter-test");
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        List<String> seen = new ArrayList<>();
        Consumer<StaggerEvent> l = e -> seen.add(e.outcome());
        NeoForge.EVENT_BUS.addListener(l);
        try {
            InjuryService.stumble(p, 0.1, 0);
            InjuryService.stumble(p, 0.95, 0);
        } finally {
            NeoForge.EVENT_BUS.unregister(l);
        }
        h.assertTrue(seen.equals(List.of("stumble", "fall")), "StaggerEvent per trip, got " + seen);
        h.assertTrue(EncounterBridge.clipsForStagger("fall").equals(List.of("catch_fall", "stagger")), "a fall throws the hands out");
        h.assertTrue(EncounterBridge.clipsForStagger("stumble").equals(List.of("stagger")), "a stumble staggers");
        h.assertTrue(EncounterBridge.clipForState(EncounterStateEvent.State.PLAY_DEAD).equals("cover_neck"), "play dead covers the neck");
        h.assertTrue(EncounterBridge.clipForState(EncounterStateEvent.State.STRIKE).equals("desperate_strike"), "strike");
        h.assertTrue(EncounterBridge.clipForState(EncounterStateEvent.State.PINNED).equals("push_up"), "pinned pushes up");
        h.succeed();
    }
}
