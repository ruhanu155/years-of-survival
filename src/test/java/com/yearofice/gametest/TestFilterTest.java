package com.yearofice.gametest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TestFilterTest {
    @Test
    void prefixes() {
        assertTrue(TestFilter.keep("yearofice:hud/x", List.of()));
        assertTrue(TestFilter.keep("yearofice:hud/x", TestFilter.parse("hud/, fire/")));
        assertTrue(TestFilter.keep("yearofice:fire/y", TestFilter.parse("yearofice:fire/")));
        assertFalse(TestFilter.keep("yearofice:body/z", TestFilter.parse("hud/,fire/")));
    }
}
