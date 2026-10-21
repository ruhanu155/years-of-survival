package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DrivesTest {
    @Test
    void mooseJulyDay() {
        DailySchedule s = DailySchedule.run(Drives.Rates.MOOSE, Drives.Weights.MOOSE, 30, 3);
        System.out.println("moose: bed " + s.bedMiddayShare() + " water " + s.duskWaterDays() + " maxSw " + s.maxSwitchesHour() + " total " + s.totalSwitches());
        assertTrue(s.bedMiddayShare() >= 0.55 && s.bedMiddayShare() <= 0.70, "bedded " + s.bedMiddayShare());
        assertTrue(s.duskWaterDays() >= 24, "dusk water days " + s.duskWaterDays());
        assertTrue(s.maxSwitchesHour() <= 6);
        assertTrue(s.totalSwitches() / 30.0 < 24, "it dithers: " + s.totalSwitches());
    }

    @Test
    void safetyOverrides() {
        Drives.State st = new Drives.State();
        st.safety = 0.8;
        Drives.step(st, Drives.Rates.MOOSE, Drives.Weights.MOOSE, 12, 0.1);
        assertEquals(Drives.Act.SAFETY, st.act);
    }
}
