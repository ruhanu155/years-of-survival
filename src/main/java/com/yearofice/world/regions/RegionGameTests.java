package com.yearofice.world.regions;

import com.yearofice.build.outpost.OutpostDef;
import com.yearofice.core.YearOfIce;
import com.yearofice.world.WorldHooks;
import com.yearofice.worldgen.regions.Regions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M9 lane D world GameTests: the 16 regions as data agree with the valley's own grid, and the dev border holds and lifts. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class RegionGameTests {
    private RegionGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:world/region_lookup", 100, RegionGameTests::regionLookup));
        out.add(test("yearofice:world/dev_border_holds", 100, RegionGameTests::devBorderHolds));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9d", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static void regionLookup(GameTestHelper h) {
        h.assertTrue(Regions.all().size() == 16, "sixteen regions");
        Set<String> kinds = new HashSet<>();
        var w = WorldHooks.world(h.getLevel());
        for (Regions.Region r : Regions.all()) {
            h.assertTrue(Regions.at(r.centreX(), r.centreZ()) == r, r.cell() + " is found at its own centre");
            if (w != null && w.p.grid().worldSizeM() == 8192)
                h.assertTrue(w.regionCode(r.centreX(), r.centreZ()).equals(r.cell()), "the valley's grid agrees on " + r.cell());
            for (String o : r.outposts()) {
                OutpostDef.get(o);
                kinds.add(o);
            }
        }
        h.assertTrue(kinds.size() == OutpostDef.IDS.size(), "every outpost kind is listed by some region: " + kinds);
        h.assertTrue(Regions.byId("salmon_bend").cell().equals("C3") && Regions.byId("D1").path().equals("windgap_pass"), "ids and cells resolve");
        h.assertTrue(!Regions.allowsOutpost(Regions.byId("A1").centreX(), Regions.byId("A1").centreZ(), "yearofice:fish_camp"), "no fish camp in the Sheep Crags");
        h.assertTrue(Regions.allowsOutpost(Regions.byId("C3").centreX(), Regions.byId("C3").centreZ(), "fish_camp"), "a fish camp at Salmon Bend");
        h.succeed();
    }

    static void devBorderHolds(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var border = server.overworld().getWorldBorder();
        double size = border.getSize(), cx = border.getCenterX(), cz = border.getCenterZ();
        try {
            h.assertTrue(Regions.defaultRing() == 1 && Regions.borderSize(1) == 8192, "the default border is the home valley's 8,192 blocks");
            DevBorder.apply(server, 1);
            h.assertTrue(border.getSize() == 8192, "ring 1 sets 8,192");
            h.assertTrue(border.isWithinBounds(4095, 0) && !border.isWithinBounds(4096, 0), "the border stands at the valley's edge");
            DevBorder.apply(server, 2);
            h.assertTrue(border.isWithinBounds(4096, 0) && !border.isWithinBounds(32768, 0), "ring 2 lifts it to the Interior");
            DevBorder.apply(server, 3);
            h.assertTrue(border.getSize() == 59_999_968, "ring 3 is the vanilla limit");
        } finally {
            border.setCenter(cx, cz);
            border.setSize(size);
        }
        h.succeed();
    }
}
