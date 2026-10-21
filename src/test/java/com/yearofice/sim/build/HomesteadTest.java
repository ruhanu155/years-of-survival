package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import org.junit.jupiter.api.Test;

class HomesteadTest {
    @Test
    void needsBothBreakupAndAWarmRoom() {
        int april = Calendar.dayOfYear(4, 27);
        assertTrue(Homestead.qualifies(april, true));
        assertFalse(Homestead.qualifies(april, false));
        assertFalse(Homestead.qualifies(Calendar.dayOfYear(1, 15), true));
        assertFalse(Homestead.qualifies(Calendar.dayOfYear(7, 1), true));
    }
}
