package com.yearofice.food.traps;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.sim.ecosystem.Density;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import com.yearofice.sim.ecosystem.Snare;
import com.yearofice.sim.ecosystem.TrapTuning;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The set snare's record (Blueprint › Traps: "Each trap keeps a small record"; Minecraft engine › Traps and nets)
 * and its rolls, run by {@link Snare}.
 *
 * <ul>
 *   <li>Live: the clock's {@code ClockEvent.Dawn} and {@code Dusk} settle the due roll ({@link TrapClock}); rolls fall
 *       at the sun's dawn and dusk ({@link #rolls}). Each tick only looks for a catch whose minute has come: then the
 *       loop springs and a player within 40 m hears the hare (the world's tell; the Trail Messenger never reports a
 *       catch).</li>
 *   <li>Unloaded: nothing ticks; {@link #onLoad} settles every missed roll in one call with the same seeded
 *       streams, so the result equals the live one. Catch-up rolls are silent.</li>
 *   <li>A held catch is drawn as a dead hare in the loop (the GeckoLib block renderer), and taken as
 *       {@code whole_game}, stamped at the minute it was caught.</li>
 *   <li>Hares in range: density × the home range's circle × the habitat share read at setting × the hare cycle's
 *       year; a catch also empties the nearest hare slot.</li>
 * </ul>
 */
public class SnareBlockEntity extends BlockEntity implements GeoBlockEntity {
    private Snare.State state;
    private Snare.SetQuality quality = Snare.SetQuality.PLAIN;
    private double habitatShare;
    private boolean announced;
    private long harvestedRoll = Long.MIN_VALUE;
    private Long clockOverride;
    private Double haresOverride;
    private boolean silent;
    // client
    private boolean clientCatch;
    private int clientCoat;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SnareBlockEntity(BlockPos pos, BlockState state) { super(TrapRegistry.SNARE_ENTITY.get(), pos, state); }

    // ---- setting ----------------------------------------------------------------------------------------------

    /** When snares roll: the sun's dawn and dusk at −6°, the minutes {@code ClockEvent.Dawn} and {@code Dusk} carry. */
    public static Snare.Rolls rolls() { return Snare.Rolls.of(ClockService.get().sun()); }

    public void set(ServerLevel level) {
        SnareSite.Reading r = SnareSite.read(level, worldPosition, getBlockState().getValue(SnareBlock.FACING));
        quality = r.quality();
        habitatShare = r.habitatShare();
        state = Snare.State.set(now(), rolls());
        announced = false;
        setChanged();
    }

    /** GameTests: set the snare's quality and habitat directly, and pin its clock and hares in range. */
    public void setForTest(Snare.SetQuality q, double share, long minute, Double hares) {
        clockOverride = minute;
        quality = q;
        habitatShare = share;
        haresOverride = hares;
        state = Snare.State.set(minute, rolls());
        announced = false;
        setChanged();
    }

    public void setClock(long minute) { clockOverride = minute; }

    /** GameTests: no sound tells. */
    public void setSilent(boolean s) { silent = s; }

    public long now() { return clockOverride != null ? clockOverride : level == null ? 0 : SliceClock.minute(level); }

    public Snare.State state() { return state; }

    public Snare.SetQuality quality() { return quality; }

    public double habitatShare() { return habitatShare; }

    public boolean broken() { return state != null && state.status() == Snare.Status.BROKEN; }

    // ---- the world at each roll ---------------------------------------------------------------------------------

    public Snare.Env env() {
        ServerLevel sl = (ServerLevel) level;
        TrapTuning t = TrapConfig.tuning();
        SpeciesDef hare = SpeciesBook.get(SnowshoeHareEntity.ID);
        double density = EcoConfig.tuning().hareDensityPerKm2();
        Snare.Rolls rolls = rolls();
        return new Snare.Env() {
            public long seed() { return sl.getSeed(); }
            public int x() { return worldPosition.getX(); }
            public int y() { return worldPosition.getY(); }
            public int z() { return worldPosition.getZ(); }
            public double hares(long roll) {
                if (haresOverride != null) return haresOverride;
                long minute = rolls.minute(roll);
                return Density.coveringPoint(density, hare.homeRangeM(), habitatShare, SlotSpawner.hareMultiplier(sl, minute));
            }
            public Snare.SetQuality quality() { return quality; }
            public TrapTuning tuning() { return t; }
            public Snare.Rolls rolls() { return rolls; }
        };
    }

    // ---- ticking ------------------------------------------------------------------------------------------------

    /** The tick rolls nothing: it only shows a held catch once its minute has come. */
    public static void serverTick(Level level, BlockPos pos, BlockState bs, SnareBlockEntity be) {
        if (be.state == null) be.set((ServerLevel) level);
        be.reveal(true);
    }

    /**
     * Settles every roll due by now (the clock's dawn or dusk, a check, a command), then shows a catch whose minute
     * has come. Live settles tell; catch-up does not.
     */
    public void settle(boolean live) {
        if (!(level instanceof ServerLevel sl) || state == null) return;
        long now = now();
        Snare.State before = state;
        state = Snare.advance(state, now, env());
        if (state != before) setChanged();
        Snare.Catch held = state.held();
        if (held != null && held.roll() != harvestedRoll && level.isLoaded(worldPosition) && live) {
            harvestedRoll = held.roll();
            SlotSpawner.harvestNear(sl, worldPosition, held.minute(), TrapConfig.tuning().respawnDays());
        }
        reveal(live);
    }

    /** A held catch whose minute has come springs the loop, and live, a player in range hears it. */
    private void reveal(boolean live) {
        if (!(level instanceof ServerLevel sl) || state == null) return;
        long now = now();
        Snare.Catch v = state.visible(now);
        if (v != null && !announced) {
            announced = true;
            if (live && !silent && now - v.minute() <= 2) tell(sl);
            setChanged();
        }
        if (live) syncBlock(sl);
    }

    private void tell(ServerLevel sl) {
        double r = TrapConfig.tuning().tellRangeM();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(worldPosition.getCenter()) <= r * r) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(TrapRegistry.SNARE_HARE,
                        SoundSource.NEUTRAL, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                        (float) (r / 16.0), 0.9f + sl.random.nextFloat() * 0.2f, sl.random.nextLong()));
            }
        }
    }

    private void syncBlock(ServerLevel sl) {
        boolean sprung = state.status() == Snare.Status.TAKEN || state.visible(now()) != null;
        BlockState bs = getBlockState();
        if (bs.getValue(SnareBlock.SPRUNG) != sprung) {
            sl.setBlock(worldPosition, bs.setValue(SnareBlock.SPRUNG, sprung), 3);
            sl.sendBlockUpdated(worldPosition, bs, getBlockState(), 3);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel) TrapClock.remove(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel) TrapClock.remove(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) TrapClock.add(level, worldPosition);
        if (level instanceof ServerLevel && state != null) {
            // Chunk-load catch-up: every missed dawn and dusk in one closed-form call, silent.
            Snare.State before = state;
            state = Snare.advance(state, now(), env());
            if (state.visible(now()) != null) announced = true;
            if (state != before) setChanged();
        }
    }

    // ---- checking -----------------------------------------------------------------------------------------------

    /** The visible catch as whole game, stamped at its minute: a wire snare kills at once. */
    public static CarcassRecord record(Snare.Catch c) {
        SpeciesDef d = SpeciesBook.get(c.species());
        int coat = SeasonalLooks.hare(Calendar.dayOfYearOfMinute(c.minute())).ordinal();
        return new CarcassRecord(c.species(), 0, "snare", c.minute(), (int) Math.round(d.wholeKg() * 1000), coat,
                Freshness.wholeCarcass(DeathPipeline.meatItem(d), c.minute()));
    }

    public InteractionResult use(Player player) {
        if (!(level instanceof ServerLevel sl) || state == null) return InteractionResult.PASS;
        settle(true);
        long now = now();
        Snare.Catch v = state.visible(now);
        if (v != null) {
            ItemStack whole = WholeGameItem.of(record(v));
            if (!player.getInventory().add(whole)) player.drop(whole, false);
            state = Snare.check(state, now, TrapConfig.tuning());
            announced = false;
            afterCheck(sl, player, "snare_taken_hare");
            return InteractionResult.CONSUME;
        }
        if (state.status() == Snare.Status.TAKEN) {
            state = Snare.check(state, now, TrapConfig.tuning());
            afterCheck(sl, player, "snare_taken");
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            sl.removeBlock(worldPosition, false);
            ItemStack wire = new ItemStack(TrapRegistry.SNARE_WIRE.get());
            if (!player.getInventory().add(wire)) player.drop(wire, false);
            return InteractionResult.CONSUME;
        }
        checkLine(player, state.status() == Snare.Status.BROKEN ? "snare_broken" : "snare_empty");
        return InteractionResult.CONSUME;
    }

    /**
     * The snare's answer to the player's own hand on it, on the pickup line (Layer 8). These are read in person at
     * the snare, so the source is {@code food.snare_check}, never the trap: the bus refuses trap sources, and a roll
     * or a catch never posts anything (the Trail Messenger never reports a catch).
     */
    static void checkLine(Player player, String id) {
        if (player instanceof ServerPlayer sp) HudBus.get().post(sp, HudLine.of(HudLineType.PICKUP, id, CHECK_SOURCE));
    }

    static final String CHECK_SOURCE = "food.snare_check";

    private void afterCheck(ServerLevel sl, Player player, String line) {
        checkLine(player, line);
        setChanged();
        if (state.status() == Snare.Status.BROKEN) {
            checkLine(player, "snare_broken");
            sl.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        syncBlock(sl);
    }

    /** GameTests and /yoi: take the visible catch, if any, as the player's check would. */
    public CarcassRecord collect() {
        Snare.Catch v = state == null ? null : state.visible(now());
        if (state != null) state = Snare.check(state, now(), TrapConfig.tuning());
        announced = false;
        setChanged();
        return v == null ? null : record(v);
    }

    // ---- saving and sync --------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (state == null) return;
        CompoundTag s = new CompoundTag();
        s.putLong("set_minute", state.setMinute());
        s.putLong("last_roll", state.lastRoll());
        s.putString("status", state.status().name());
        s.putInt("catches", state.catches());
        s.putLongArray("recent", state.recent().stream().mapToLong(Long::longValue).toArray());
        if (state.held() != null) {
            CompoundTag h = new CompoundTag();
            h.putString("species", state.held().species());
            h.putLong("roll", state.held().roll());
            h.putLong("minute", state.held().minute());
            s.put("held", h);
        }
        tag.put("snare", s);
        tag.putBoolean("on_trail", quality.onTrail());
        tag.putBoolean("beside_cover", quality.besideCover());
        tag.putBoolean("funnel", quality.funnel());
        tag.putBoolean("baited", quality.baited());
        tag.putDouble("habitat_share", habitatShare);
        tag.putBoolean("announced", announced);
        tag.putLong("harvested_roll", harvestedRoll);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("snare")) {
            CompoundTag s = tag.getCompound("snare");
            Snare.Catch held = null;
            if (s.contains("held")) {
                CompoundTag h = s.getCompound("held");
                held = new Snare.Catch(h.getString("species"), h.getLong("roll"), h.getLong("minute"));
            }
            List<Long> recent = new ArrayList<>();
            for (long m : s.getLongArray("recent")) recent.add(m);
            state = new Snare.State(s.getLong("set_minute"), s.getLong("last_roll"), Snare.Status.valueOf(s.getString("status")),
                    held, s.getInt("catches"), recent);
            quality = new Snare.SetQuality(tag.getBoolean("on_trail"), tag.getBoolean("beside_cover"), tag.getBoolean("funnel"),
                    tag.getBoolean("baited"), 1, 1);
            habitatShare = tag.getDouble("habitat_share");
            announced = tag.getBoolean("announced");
            harvestedRoll = tag.contains("harvested_roll") ? tag.getLong("harvested_roll") : Long.MIN_VALUE;
        }
        if (tag.contains("client_catch")) {
            clientCatch = tag.getBoolean("client_catch");
            clientCoat = tag.getInt("client_coat");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        Snare.Catch v = state == null ? null : state.visible(now());
        t.putBoolean("client_catch", v != null);
        t.putInt("client_coat", v == null ? 0 : SeasonalLooks.hare(Calendar.dayOfYearOfMinute(v.minute())).ordinal());
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public boolean clientCatch() { return clientCatch; }

    public int clientCoat() { return clientCoat; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "pose", 0,
                st -> st.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.snowshoe_hare.death"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
