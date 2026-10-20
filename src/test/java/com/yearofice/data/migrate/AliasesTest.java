package com.yearofice.data.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class AliasesTest {
    @Test
    void aChainResolvesToTheLiveId() {
        Aliases a = new Aliases().add("item", "yearofice:a", "yearofice:b").add("item", "yearofice:b", "yearofice:c");
        assertEquals("yearofice:c", a.resolve("item", "yearofice:a"));
        assertEquals("yearofice:c", a.resolve("item", "yearofice:c"));
        assertEquals("yearofice:a", a.resolve("block", "yearofice:a"), "kinds are separate");
    }

    @Test
    void aCycleIsFoundNotFollowedForever() {
        Aliases a = new Aliases().add("item", "yearofice:a", "yearofice:b").add("item", "yearofice:b", "yearofice:a");
        assertEquals(2, a.problems().size());
        assertThrows(IllegalStateException.class, () -> a.resolve("item", "yearofice:a"));
    }

    @Test
    void anAliasCannotPointAtItselfOrBeListedTwice() {
        assertThrows(IllegalArgumentException.class, () -> new Aliases().add("item", "yearofice:a", "yearofice:a"));
        Aliases a = new Aliases().add("item", "yearofice:a", "yearofice:b");
        assertThrows(IllegalArgumentException.class, () -> a.add("item", "yearofice:a", "yearofice:c"));
    }

    @Test
    void retiredFilesLoadByKind() {
        Aliases a = new Aliases().addJson(JsonParser.parseString(
                "{ \"kind\": \"block\", \"aliases\": { \"yearofice:old_fire_ring\": \"yearofice:fire_ring\" } }").getAsJsonObject());
        assertEquals("yearofice:fire_ring", a.resolve("block", "yearofice:old_fire_ring"));
        assertEquals(1, a.count());
    }

    @Test
    void theShippedAliasFilesAreSound() {
        Aliases a = Aliases.shared();
        assertTrue(a.problems().isEmpty(), a.problems().toString());
    }
}
