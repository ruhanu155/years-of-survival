package com.yearofice.world.ice;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.world.IceService;
import com.yearofice.sim.world.IceModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.season.SeasonConfig;
import com.yearofice.world.season.SeasonState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** {@link IceService}: ice thickness and stage at a column, and the ice holes that skin over. */
public final class IceServiceImpl implements IceService {
    private BodyClass classAt(ServerLevel level, BlockPos pos) {
        return BodyClass.values()[IcePass.classOf(WorldHooks.world(level), pos.getX(), pos.getZ())[0]];
    }

    @Override
    public double thicknessCm(ServerLevel level, BlockPos pos) { return SeasonState.get(level).core.ice(classAt(level, pos)).cm; }

    @Override
    public IceModel.Stage stage(ServerLevel level, BlockPos pos) {
        return IceModel.stage(SeasonState.get(level).core.ice(classAt(level, pos)), SeasonConfig.ice());
    }

    @Override
    public boolean digHole(ServerLevel level, BlockPos pos) {
        if (!IceBlocks.isIce(level.getBlockState(pos))) return false;
        level.setBlock(pos, IceBlocks.ICE_HOLE.get().defaultBlockState(), Block.UPDATE_ALL);
        SeasonState st = SeasonState.get(level);
        st.holes.put(pos.asLong(), new long[] {ClockService.get().nowMinute(), 0});
        st.setDirty();
        return true;
    }

    @Override
    public boolean isOpenHole(ServerLevel level, BlockPos pos) { return level.getBlockState(pos).is(IceBlocks.ICE_HOLE.get()); }

    @Override
    public int tickHoles(ServerLevel level) {
        SeasonState st = SeasonState.get(level);
        if (st.holes.isEmpty()) return 0;
        long now = ClockService.get().nowMinute();
        int closed = 0;
        List<Long> gone = new ArrayList<>();
        for (Map.Entry<Long, long[]> e : st.holes.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            if (!level.isLoaded(p)) continue;
            if (!level.getBlockState(p).is(IceBlocks.ICE_HOLE.get())) { gone.add(e.getKey()); continue; }
            long[] h = e.getValue();
            long dt = now - h[0];
            if (dt <= 0) continue;
            double need = IceModel.holeSkinMinutes(EnvService.get().airC(level, p), SeasonConfig.ice());
            h[0] = now;
            h[1] += need == Double.POSITIVE_INFINITY ? 0 : Math.round(dt / need * 1000.0);
            if (h[1] >= 1000) {
                level.setBlock(p, IceBlocks.SKIM_ICE.get().defaultBlockState(), Block.UPDATE_ALL);
                gone.add(e.getKey());
                closed++;
            }
        }
        for (Long g : gone) st.holes.remove(g);
        st.setDirty();
        return closed;
    }
}
