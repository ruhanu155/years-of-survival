package com.yearofice.hunt.fishing;

import com.yearofice.api.decay.DecayService;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.fishing.Draws;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.SizeDraw;
import com.yearofice.sim.fishing.SliceBite;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A rod session without the hands: the same bite roll the bobber runs each game minute ({@link FishingService#candidates}),
 * the same bait wear, and a fixed share of bites struck and landed (the strike window and the fight are the player's
 * skill). Used by the month proxy and {@code /yoi fish session}; never by play.
 */
public final class RodSession {
    private RodSession() {}

    /** Seconds a landed fish costs on top of the wait: strike, fight, unhook, rebait. */
    public static final int MINUTES_PER_BITE = 3;

    public record Result(List<ItemStack> fish, int bites, int landed, int baitUsed, int minutes, boolean ranOutOfBait) {}

    /**
     * Fishes from {@code start} for up to {@code minutes} game minutes at {@code water}. Bait comes from the player's pack
     * (worm, guts, fish chunk, in that order); each landed fish goes into the pack, or drops at the feet if it is full.
     */
    public static Result run(ServerLevel level, ServerPlayer p, BlockPos water, long start, int minutes, double landShare) {
        FishingTuning t = FishingConfig.tuning();
        long seed = level.getSeed() ^ Draws.name("bite");
        long key = SliceSpots.key(water);
        long sizeSeed = level.getSeed() ^ Draws.name("rod_size");
        List<ItemStack> fish = new ArrayList<>();
        HookBait bait = HookBait.NONE;
        int bites = 0, landed = 0, used = 0, spent = 0;
        boolean dry = false;
        for (int i = 0; i < minutes; i++) {
            long minute = start + i;
            spent = i + 1;
            if (!bait.present()) {
                bait = fitFromPack(p, t);
                if (!bait.present()) { dry = true; break; }
                used++;
            }
            List<SliceBite.Inputs> cands = FishingService.candidates(level, water, bait, minute);
            String biter = null;
            SliceBite.Inputs hit = null;
            for (int c = 0; c < cands.size(); c++) {
                SliceBite.Inputs in = cands.get(c);
                if (Draws.u(seed, key, minute, c == 0 ? SliceBite.SALT_BITE : 100 + c) < SliceBite.pBite(in)) { biter = in.species().id(); hit = in; break; }
            }
            bait = bait.afterMinutes(1);
            if (biter == null) continue;
            bites++;
            bait = bait.afterBite();
            i += MINUTES_PER_BITE - 1;
            if (Draws.u(sizeSeed, key, minute, 7) >= landShare) continue;
            double u = Draws.u(sizeSeed, key, minute, SliceBite.SALT_SIZE);
            double frac = hit.stockK() <= 0 ? 1.0 : Math.min(1.0, hit.stockN() / hit.stockK());
            double kg = SizeDraw.kg(FishingSpecies.byId(biter), 1.0, frac, hit.spotQ(), u, 1.0, 0.5);
            ItemStack f = FishingService.landedFish(biter, (int) Math.round(kg * 1000));
            DecayService.get().stamp(level, f, FoodState.RAW);
            fish.add(f);
            landed++;
            long day = FishingClock.day(minute);
            for (SpotFish.Entry e : SpotFish.at(level, water, FishSeason.dayOfYear(day), t).fish()) {
                if (e.id().equals(biter)) {
                    SliceSpots.stocks(level).harvest(SliceSpots.speciesKey(key, e.id()), day, e.k(), e.r(), 1, minute);
                    break;
                }
            }
        }
        for (ItemStack f : fish) if (!p.getInventory().add(f.copy())) p.drop(f.copy(), false);
        return new Result(fish, bites, landed, used, spent, dry);
    }

    /** Takes one bait from the pack and returns it fresh on the hook, or NONE. */
    static HookBait fitFromPack(ServerPlayer p, FishingTuning t) {
        for (var want : List.of(FishingRegistry.WORM.get(), FishingRegistry.FISH_GUTS.get(), FishingRegistry.FISH_CHUNK.get())) {
            for (int s = 0; s < p.getInventory().getContainerSize(); s++) {
                ItemStack st = p.getInventory().getItem(s);
                if (st.is(want)) {
                    var key = FishBaits.key(st);
                    st.shrink(1);
                    return FishBaits.fresh(key.orElse("worm"), t);
                }
            }
        }
        return HookBait.NONE;
    }
}
