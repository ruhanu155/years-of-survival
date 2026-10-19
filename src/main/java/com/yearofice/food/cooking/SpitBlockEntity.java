package com.yearofice.food.cooking;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.rules.YoiConfig;
import com.yearofice.food.FoodConfig;
import com.yearofice.food.Meals;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.RoastTuning;
import com.yearofice.sim.food.Roasting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A spit over a fire ring holding one load (Blueprint › Cooking: spit or stick roast). The server decides: each game
 * minute the load's {@link Roasting.Spit} advances by the fire below as {@code api.fire} reports it, and when the ring
 * reaches Rare the stack turns cooked (its freshness keeps its progress on the cooked life) and carries
 * {@code yearofice:doneness}. The client shows: the load stack and its ring come down in the block-entity update and
 * {@code client.food.SpitRenderer} draws it on the crossbar.
 *
 * <p>Minutes accrue at the World rate the fire ring itself steps at (1,440 game minutes over the configured day
 * length), so doneness stays in step with the ring's fuel and states; a STAND-IN until ClockService lands. GameTests
 * {@link #pause} the tick and drive {@link #stepMinutes} in step with the ring's own catch-up.
 */
public class SpitBlockEntity extends BlockEntity {
    private ItemStack load = ItemStack.EMPTY;
    private @Nullable Roasting.Spit spit;
    private Doneness.Ring shown = Doneness.Ring.RAW;
    private double minuteCarry;
    private boolean paused;

    public SpitBlockEntity(BlockPos pos, BlockState state) { super(CookingRegistry.SPIT_ENTITY.get(), pos, state); }

    public ItemStack load() { return load; }

    public @Nullable Roasting.Spit spit() { return spit; }

    /** The ring as last settled (server) or as last sent (client). */
    public Doneness.Ring ring() { return shown; }

    public boolean empty() { return load.isEmpty(); }

    /** GameTests stop the real tick and step minutes themselves. */
    public void pause(boolean p) { paused = p; }

    static double minutesPerTick() {
        int dayMin = YoiConfig.SPEC.isLoaded() ? YoiConfig.DAY_LENGTH_MIN.get() : 24;
        return 1440.0 / (dayMin * 60.0 * 20.0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpitBlockEntity be) {
        if (be.paused || be.load.isEmpty()) return;
        be.minuteCarry += minutesPerTick();
        if (be.minuteCarry >= 1) {
            int whole = (int) Math.floor(be.minuteCarry);
            be.minuteCarry -= whole;
            be.stepMinutes(whole);
        }
    }

    /** The fire under the spit now. */
    public Doneness.Fire fire() { return level == null ? Doneness.Fire.OUT : RingHeat.at(level, worldPosition.below()); }

    /** Runs the load forward {@code minutes} game minutes over the fire as it is now. */
    public void stepMinutes(int minutes) {
        if (level == null || level.isClientSide || spit == null || minutes <= 0) return;
        RoastTuning t = FoodConfig.tuning();
        Doneness.Fire f = fire();
        for (int i = 0; i < minutes; i++) spit = spit.after(1, f, t);
        settleRing(f);
        setChanged();
    }

    /** Applies the ring to the load: cooked from Rare on, and the doneness component. */
    private void settleRing(Doneness.Fire f) {
        Doneness.Ring r = spit.ring(f);
        if (r == shown) return;
        shown = r;
        if (Roasting.cooked(r)) {
            DecayService.get().changeState(level, load, FoodState.COOKED);
            load.set(YoiComponents.DONENESS.get(), r);
        }
        sync();
    }

    /**
     * Puts food on the spit from {@code held} (moved off it, as many items as fit). Returns the number taken, 0 if the
     * spit is full or the food does not roast.
     */
    public int putOn(ItemStack held) {
        if (level == null || !load.isEmpty() || held.isEmpty()) return 0;
        CookingData.Food food = CookingData.of(held.getItem());
        FoodMap.Row row = FoodMap.row(held);
        if (food == null || row == null || !row.spoilDaysAt5c().containsKey(FoodState.COOKED.key())) return 0;
        RoastTuning t = FoodConfig.tuning();
        double g = Meals.grams(held);
        if (!(g > 0)) return 0;
        int n = (int) Math.max(1, Math.min(held.getCount(), Math.floor(t.spitCapacityG() / g)));
        ItemStack taken = held.split(n);
        DecayService decay = DecayService.get();
        if (decay.settle(level, taken) == null) decay.stamp(level, taken, FoodState.RAW);
        load = taken;
        Doneness.Ring already = taken.get(YoiComponents.DONENESS.get());
        spit = Roasting.Spit.load(Roasting.spitMinutes(food.spit(), g * n, t), already);
        shown = already == null ? Doneness.Ring.RAW : already;
        minuteCarry = 0;
        sync();
        return n;
    }

    /** Takes the whole load off, cooked as far as it got. */
    public ItemStack takeOff() {
        if (load.isEmpty()) return ItemStack.EMPTY;
        if (spit != null && level != null && !level.isClientSide) settleRing(fire());
        ItemStack out = load;
        load = ItemStack.EMPTY;
        spit = null;
        shown = Doneness.Ring.RAW;
        sync();
        return out;
    }

    /** The spit was broken or lost its ring: the load drops. */
    void dropLoad() {
        if (level == null || level.isClientSide || load.isEmpty()) return;
        ItemStack out = takeOff();
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5, out);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---- saving and sync -----------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (!load.isEmpty()) tag.put("load", load.save(regs));
        if (spit != null) {
            tag.putDouble("cook_min", spit.cookMinutes());
            tag.putDouble("progress", spit.progress());
            tag.putBoolean("burnt", spit.burnt());
        }
        tag.putString("ring", shown.name().toLowerCase(java.util.Locale.ROOT));
        tag.putDouble("minute_carry", minuteCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        load = tag.contains("load") ? ItemStack.parse(regs, tag.getCompound("load")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        spit = tag.contains("cook_min") && !load.isEmpty()
                ? new Roasting.Spit(tag.getDouble("cook_min"), tag.getDouble("progress"), tag.getBoolean("burnt")) : null;
        shown = tag.contains("ring") ? Doneness.Ring.valueOf(tag.getString("ring").toUpperCase(java.util.Locale.ROOT)) : Doneness.Ring.RAW;
        minuteCarry = tag.getDouble("minute_carry");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        if (!load.isEmpty()) t.put("load", load.save(regs));
        t.putString("ring", shown.name().toLowerCase(java.util.Locale.ROOT));
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
