package com.yearofice.food.traps;

import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.food.FoodConfig;
import com.yearofice.sim.ecosystem.Snare;
import com.yearofice.sim.food.Deadfall;
import com.yearofice.sim.food.DeadfallTuning;
import com.yearofice.sim.food.MiceInStores;
import com.yearofice.sim.time.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A set deadfall's record and its dawn and dusk rolls, run by {@link Deadfall} (Blueprint › Traps). The figure-4
 * catches the snowshoe hare until the marten has a place in the book; the mouse deadfall rolls on winter nights only
 * and its catch is a mouse (marten bait, no item yet). Catch-up on load settles every missed roll silently from the
 * same seeded streams, so reloading never rerolls a catch. The sticks last {@code sticksLife} triggers.
 */
public class DeadfallBlockEntity extends BlockEntity {
    public enum Status { SET, CATCH, SPRUNG, BROKEN }

    private Status status = Status.SET;
    private long lastRoll = Long.MIN_VALUE;
    private long heldMinute;
    private long heldRoll;
    private int triggers;
    private int setNo;
    private boolean slipped;
    private int mice;
    private Long clockOverride;
    private Double animalsOverride;
    private boolean silent;

    public DeadfallBlockEntity(BlockPos pos, BlockState state) { super(TrapRegistry.DEADFALL_ENTITY.get(), pos, state); }

    // ---- setting ----------------------------------------------------------------------------------------------

    /** Sets the trap now. At practice 0 the rock slips 15% of the time: a cut finger and 2 Condition (the slip is logged). */
    public void set() {
        long now = now();
        lastRoll = SnareBlockEntity.rolls().atOrBefore(now);
        status = Status.SET;
        setNo++;
        slipped = level instanceof ServerLevel sl && Deadfall.slips(sl.getSeed(), worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), setNo, FoodConfig.deadfall().slipP(), 0);
        setChanged();
    }

    public void setClock(long minute) { clockOverride = minute; }

    /** GameTests: pin the animals in range of the trap. */
    public void setAnimals(Double n) { animalsOverride = n; }

    public void setSilent(boolean s) { silent = s; }

    public long now() { return clockOverride != null ? clockOverride : level == null ? 0 : SliceClock.minute(level); }

    public Status status() { return status; }

    public boolean mouse() { return getBlockState().getBlock() instanceof DeadfallBlock d && d.mouse(); }

    public int triggers() { return triggers; }

    public int mice() { return mice; }

    public boolean slipped() { return slipped; }

    public long lastRoll() { return lastRoll; }

    // ---- rolling ------------------------------------------------------------------------------------------------

    double animals(long rollMinute) {
        if (animalsOverride != null) return animalsOverride;
        DeadfallTuning t = FoodConfig.deadfall();
        return mouse() ? t.mouseN() : t.bigN();
    }

    /** Settles every roll due by now; live settles tell, catch-up does not. Returns the number of rolls that caught. */
    public int settle(boolean live) {
        if (!(level instanceof ServerLevel sl) || status != Status.SET) return 0;
        long now = now();
        Snare.Rolls rolls = SnareBlockEntity.rolls();
        long to = rolls.atOrBefore(now);
        if (lastRoll == Long.MIN_VALUE) lastRoll = to;
        DeadfallTuning tune = FoodConfig.deadfall();
        int caught = 0;
        for (long k = lastRoll + 1; k <= to && status == Status.SET; k++) {
            long minute = rolls.minute(k);
            if (mouse() && !MiceInStores.mouseSeason(Calendar.month(Calendar.dayOfYearOfMinute(minute)))) continue;
            Deadfall.Outcome o = Deadfall.roll(sl.getSeed(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), k,
                    animals(minute), 1.0, false, mouse() ? tune.mouse() : tune.big());
            if (o == Deadfall.Outcome.EMPTY) continue;
            triggers++;
            heldMinute = minute;
            heldRoll = k;
            if (o == Deadfall.Outcome.KILL) {
                status = Status.CATCH;
                caught++;
            } else {
                status = Status.SPRUNG;
            }
            if (triggers >= tune.sticksLife() && status == Status.SPRUNG) status = Status.BROKEN;
            if (live && !silent && now - minute <= 2) tell(sl, tune);
        }
        lastRoll = to;
        sync(sl);
        setChanged();
        return caught;
    }

    private void tell(ServerLevel sl, DeadfallTuning tune) {
        double r = mouse() ? tune.mouseSoundRangeM() : tune.soundRangeM();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(worldPosition.getCenter()) <= r * r) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(TrapRegistry.DEADFALL_SOUND.get()), SoundSource.NEUTRAL,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                        (float) (r / 16.0), mouse() ? 1.4f : 0.8f, sl.random.nextLong()));
            }
        }
    }

    private void sync(ServerLevel sl) {
        boolean sprung = status != Status.SET;
        BlockState bs = getBlockState();
        if (bs.hasProperty(DeadfallBlock.SPRUNG) && bs.getValue(DeadfallBlock.SPRUNG) != sprung) {
            sl.setBlock(worldPosition, bs.setValue(DeadfallBlock.SPRUNG, sprung), 3);
        }
    }

    // ---- checking -----------------------------------------------------------------------------------------------

    public InteractionResult use(Player player) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        settle(true);
        if (status == Status.CATCH) {
            takeCatch(player);
            return InteractionResult.CONSUME;
        }
        if (status == Status.SPRUNG) {
            status = Status.SET;
            lastRoll = SnareBlockEntity.rolls().atOrBefore(now());
            sync(sl);
            setChanged();
            line(player, "deadfall_reset");
            return InteractionResult.CONSUME;
        }
        if (status == Status.BROKEN) {
            line(player, "deadfall_broken");
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            sl.removeBlock(worldPosition, false);
            ItemStack back = new ItemStack(getBlockState().getBlock().asItem());
            if (!player.getInventory().add(back)) player.drop(back, false);
            return InteractionResult.CONSUME;
        }
        line(player, slipped ? "deadfall_slipped" : "deadfall_set");
        return InteractionResult.CONSUME;
    }

    /** Takes the held catch: a hare as whole game, or a mouse counted. The trap is left sprung, to reset. */
    public CarcassRecord takeCatch(Player player) {
        CarcassRecord rec = null;
        if (mouse()) {
            mice++;
            if (player != null) line(player, "deadfall_mouse");
        } else {
            rec = SnareBlockEntity.record(new Snare.Catch(SnowshoeHareEntity.ID, heldRoll, heldMinute));
            if (player != null) {
                ItemStack whole = WholeGameItem.of(rec);
                if (!player.getInventory().add(whole)) player.drop(whole, false);
                line(player, "deadfall_taken_hare");
            }
        }
        status = triggers >= FoodConfig.deadfall().sticksLife() ? Status.BROKEN : Status.SPRUNG;
        if (level instanceof ServerLevel sl) sync(sl);
        setChanged();
        return rec;
    }

    static void line(Player player, String id) {
        if (player instanceof ServerPlayer sp) HudBus.get().post(sp, HudLine.of(HudLineType.PICKUP, id, SnareBlockEntity.CHECK_SOURCE));
    }

    // ---- saving -------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putString("status", status.name());
        tag.putLong("last_roll", lastRoll);
        tag.putLong("held_minute", heldMinute);
        tag.putLong("held_roll", heldRoll);
        tag.putInt("triggers", triggers);
        tag.putInt("set_no", setNo);
        tag.putBoolean("slipped", slipped);
        tag.putInt("mice", mice);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (!tag.contains("status")) return;
        status = Status.valueOf(tag.getString("status"));
        lastRoll = tag.getLong("last_roll");
        heldMinute = tag.getLong("held_minute");
        heldRoll = tag.getLong("held_roll");
        triggers = tag.getInt("triggers");
        setNo = tag.getInt("set_no");
        slipped = tag.getBoolean("slipped");
        mice = tag.getInt("mice");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            TrapClock.add(level, worldPosition);
            if (lastRoll != Long.MIN_VALUE) settle(false);
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
}
