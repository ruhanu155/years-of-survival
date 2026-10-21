package com.yearofice.fire.stove;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.build.multiblock.MultiblockData;
import com.yearofice.fire.FireRegistry;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.Armload;
import com.yearofice.sim.fire.Stove;
import com.yearofice.sim.fire.StoveBurn;
import com.yearofice.sim.fire.Wood;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The stove's firebox on the game clock. It catches up from the minutes the clock has moved since it last ran
 * (capped at 3 game days a catch-up), burns fuel at a rate set by the chimney's draft, builds creosote, posts its
 * output to {@link StoveIndex}, smokes outside at the chimney's top, and (unflued) fills the room with bad air.
 */
public class StoveBlockEntity extends BlockEntity {
    public static final int SWEEP_BOUGHS = 5;
    private static final double MAX_CATCHUP_MIN = 3 * 24 * 60;
    private static final String BAD_AIR = "coal_air";

    private StoveBurn burn;
    private long lastMinute = Long.MIN_VALUE;
    private double pinChimneyM = Double.NaN;
    private boolean paused;
    private int chimneyFires;
    private long lastHarmMinute = Long.MIN_VALUE;
    private long rollCounter;

    public StoveBlockEntity(BlockPos pos, BlockState state) { super(StoveRegistry.STOVE_ENTITY.get(), pos, state); }

    public String kindId() { return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }

    public Stove.Kind kind() {
        String id = kindId();
        return new Stove.Kind(id, MultiblockData.num(id, "armload_min"), MultiblockData.num(id, "efficiency_pct") / 100.0,
                (int) MultiblockData.num(id, "max_armloads"), MultiblockData.num(id, "min_chimney_m"), MultiblockData.num(id, "ideal_chimney_m"));
    }

    private StoveBurn burn() {
        if (burn == null) burn = new StoveBurn(kind());
        return burn;
    }

    // ---- chimney -------------------------------------------------------------------------------------------------------

    /** GameTests pin the chimney height; real play counts the pipes. NaN unpins. */
    public void pinChimney(double metres) { pinChimneyM = metres; }

    /** GameTests stop the live clock and step minutes themselves. */
    public void pause(boolean p) { paused = p; }

    /** Metres of pipe stacked straight above the stove, up to the kind's maximum. */
    public double chimneyM() {
        if (!Double.isNaN(pinChimneyM)) return pinChimneyM;
        if (level == null) return 0;
        int max = (int) MultiblockData.num(kindId(), "max_chimney_m"), n = 0;
        BlockPos p = worldPosition.above();
        while (n < max && level.getBlockState(p).is(StoveRegistry.CHIMNEY_PIPE.get())) { n++; p = p.above(); }
        return n;
    }

    /** True when the block above the top pipe lets smoke out. */
    public boolean topOpen() {
        if (!Double.isNaN(pinChimneyM)) return true;
        if (level == null) return false;
        BlockPos top = worldPosition.above((int) chimneyM() + 1);
        return level.getBlockState(top).getCollisionShape(level, top).isEmpty();
    }

    public boolean flued() { return Stove.flued(chimneyM(), topOpen()); }

    public double draft() { return flued() ? Stove.draft(kind(), chimneyM()) : Stove.UNFLUED_DRAFT; }

    // ---- the box -------------------------------------------------------------------------------------------------------

    public double loadFuel(Wood w, int moisturePct, double share) {
        double took = burn().load(w, moisturePct, share);
        if (took > 0) changed();
        return took;
    }

    public boolean light() {
        boolean ok = burn().light();
        if (ok) { lastMinute = nowMinute(); sync(); changed(); }
        return ok;
    }

    public void douse() { burn().douse(); sync(); changed(); }

    public FireSetting setting() { return burn().setting(); }
    public void setting(FireSetting s) { burn().setting(s); sync(); changed(); }
    public boolean lit() { return burn().lit(); }
    public double armloads() { return burn().armloads(); }
    public double creosote() { return burn().creosote(); }
    public void creosote(double c) { burn().creosote(c); changed(); }
    public double smokePct() { return burn().smokePct(); }
    public double burnedArmloads() { return burn().burnedArmloads(); }
    public int chimneyFires() { return chimneyFires; }

    /** Watts the room gets now. */
    public double outputW() { return burn().outputW(draft()); }

    /** Game minutes the head armload lasts at the present setting and draft (for the readout and tests). */
    public double minutesPerArmload() {
        Armload a = burn().queue().isEmpty() ? null : burn().queue().get(0);
        return Stove.minutesPerArmload(kind(), setting(), draft(), a == null ? Wood.BIRCH : a.wood);
    }

    /** Sweeping with a spruce-bough brush clears the chimney (Smoke indoors: resets creosote). */
    public void sweep() { burn().creosote(0); changed(); }

    private long nowMinute() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    /** Burns {@code minutes} of game time now (the closed-form catch-up the clock and tests both use). */
    public void stepMinutes(double minutes) {
        if (level == null || level.isClientSide || minutes <= 0) return;
        StoveBurn b = burn();
        boolean wasLit = b.lit();
        SplittableRandom rng = new SplittableRandom(worldPosition.asLong() * 31 + (rollCounter++)); // not java.util.Random: neighbouring seeds give neighbouring first draws
        StoveBurn.Result r = b.step(Math.min(minutes, MAX_CATCHUP_MIN), draft(), flued(), rng);
        if (r.chimneyFire()) chimneyFire();
        if (wasLit != b.lit() || r.minutesBurned() > 0) sync();
        harm(minutes);
        changed();
    }

    private void chimneyFire() {
        chimneyFires++;
        BlockPos p = worldPosition.above();
        int n = 0;
        while (n++ < 16 && level.getBlockState(p).is(StoveRegistry.CHIMNEY_PIPE.get())) {
            level.setBlock(p, FireRegistry.FLAME.get().defaultBlockState(), 3);
            p = p.above();
        }
    }

    /** Unflued smoke past 50% gives bad air to anyone within 6 blocks, at most once per game 5 minutes. */
    private void harm(double minutes) {
        if (!(level instanceof ServerLevel sl) || burn().smokePct() <= 50) return;
        long now = nowMinute();
        if (now - lastHarmMinute < 5 && lastHarmMinute != Long.MIN_VALUE) return;
        lastHarmMinute = now;
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(worldPosition) <= 36) BodyService.get().beginIllness(p, BAD_AIR);
        }
    }

    private void sync() {
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        boolean lit = burn().lit();
        if (s.getValue(StoveBlock.LIT) != lit) level.setBlock(worldPosition, s.setValue(StoveBlock.LIT, lit), 3);
        com.yearofice.api.fire.HeatSources.postDuty(level, worldPosition, lit ? burn().duty(draft()) : 1.0);
        if (lit) StoveIndex.INSTANCE.set(level, worldPosition, outputW(), flued());
        else StoveIndex.INSTANCE.remove(level, worldPosition);
    }

    private void changed() { setChanged(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StoveBlockEntity be) {
        if (level.getGameTime() % 20 != 0) return;
        long now = be.nowMinute();
        if (be.lastMinute == Long.MIN_VALUE) be.lastMinute = now;
        if (!be.paused && be.burn().lit() && now > be.lastMinute) be.stepMinutes(now - be.lastMinute);
        else if (!be.paused && !be.burn().lit() && now > be.lastMinute) be.burn().idle(now - be.lastMinute);
        be.lastMinute = now;
        if (be.burn().lit()) {
            be.sync();
            if (level instanceof ServerLevel sl) {
                BlockPos top = be.flued() ? pos.above((int) be.chimneyM() + 1) : pos.above();
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            if (lastMinute == Long.MIN_VALUE) lastMinute = nowMinute(); // a saved stove catches up the minutes it was away
            if (burn != null && burn.lit()) sync();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) { StoveIndex.INSTANCE.remove(level, worldPosition); com.yearofice.api.fire.HeatSources.postDuty(level, worldPosition, 1.0); }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (burn == null) return;
        tag.putBoolean("lit", burn.lit());
        tag.putString("setting", burn.setting().name());
        tag.putDouble("creosote", burn.creosote());
        tag.putDouble("smoke_pct", burn.smokePct());
        tag.putInt("chimney_fires", chimneyFires);
        tag.putDouble("fired_min_24", burn.firedMin24());
        tag.putDouble("observed_min_24", burn.observedMin24());
        if (lastMinute != Long.MIN_VALUE) tag.putLong("last_minute", lastMinute);
        ListTag l = new ListTag();
        for (Armload a : burn.queue()) {
            CompoundTag c = new CompoundTag();
            c.putString("wood", a.wood.id());
            c.putInt("moisture", a.moisturePct);
            c.putDouble("amount", a.amount());
            l.add(c);
        }
        tag.put("armloads", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (!tag.contains("setting")) return;
        StoveBurn b = new StoveBurn(kind());
        for (Tag t : tag.getList("armloads", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            b.load(Wood.byId(c.getString("wood")), c.getInt("moisture"), c.getDouble("amount"));
        }
        b.setting(FireSetting.valueOf(tag.getString("setting")));
        b.creosote(tag.getDouble("creosote"));
        b.smokePct(tag.getDouble("smoke_pct"));
        b.lit(tag.getBoolean("lit"));
        chimneyFires = tag.getInt("chimney_fires");
        b.firedMin24(tag.getDouble("fired_min_24"));
        b.observedMin24(tag.getDouble("observed_min_24"));
        if (tag.contains("last_minute")) lastMinute = tag.getLong("last_minute");
        burn = b;
    }
}
