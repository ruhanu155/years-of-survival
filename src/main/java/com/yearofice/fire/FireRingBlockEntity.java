package com.yearofice.fire;

import com.yearofice.api.fire.FireStateEvent;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.Armload;
import com.yearofice.sim.fire.Fire;
import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireTuning;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.sim.fire.LayPart;
import com.yearofice.sim.fire.Wood;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The block entity of {@code yearofice:fire_ring} (Blueprint › Fire, water and light › The fire state machine): one
 * {@link Fire} per ring, stepped by the clock's game minutes ({@code ClockEvent.Minute}, through {@link FireClock}),
 * so the Close, Work and Sleep rates burn it as fast as they move time; the ember's real seconds are counted per
 * server tick. On every change it updates the block state (light and model), the heat index, and posts
 * {@link FireStateEvent}. Interactions come from {@link FireRingBlock}; {@code /yoi fire} and the GameTests call the
 * public methods directly.
 */
public class FireRingBlockEntity extends BlockEntity {
    private Fire fire = new Fire();
    /** The stacks laid into the unlit lay, in order, so a part can be taken back and a broken ring drops them. */
    private final List<ItemStack> layStacks = new ArrayList<>();
    private FireWeather weatherOverride;
    private boolean paused;
    private long lastPuffTick = -100;
    private FireState shownState = FireState.OUT;

    public FireRingBlockEntity(BlockPos pos, BlockState state) {
        super(FireRegistry.FIRE_RING_ENTITY.get(), pos, state);
    }

    public Fire fire() { return fire; }

    /** GameTests pin the weather on one ring; null returns to the world's. */
    public void setWeatherOverride(FireWeather w) { weatherOverride = w; }

    public FireWeather weather() { return level == null ? FireWeather.CALM : FireEnv.sample(level, worldPosition, weatherOverride); }

    /** GameTests hold a ring still: neither the clock's minutes nor the ember's seconds move it. */
    public void pause(boolean p) { paused = p; }

    public boolean paused() { return paused; }

    // ---- ticking -------------------------------------------------------------------------------------------

    /** The server tick: only the ember's real seconds. Game minutes come from the clock ({@link #clockMinutes}). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FireRingBlockEntity be) {
        if (be.paused) return;
        Fire.Change c = be.fire.tick();
        if (c != null) be.changed(c, null);
    }

    /** {@code minutes} game minutes from the clock ({@code ClockEvent.Minute}, or a jump's closed form). */
    void clockMinutes(int minutes) {
        if (!paused && minutes > 0) stepMinutes(minutes);
    }

    /** Steps {@code minutes} game minutes in the current weather (the closed-form catch-up the tests also use). */
    public void stepMinutes(int minutes) {
        FireTuning t = FireConfig.tuning();
        for (int i = 0; i < minutes; i++) {
            Fire.Change c = fire.stepMinute(weather(), t);
            if (c != null) changed(c, null);
        }
        if (minutes > 0) {
            syncLay();
            setChanged();
            refreshIndex();
        }
    }

    // ---- interaction ---------------------------------------------------------------------------------------

    ItemInteractionResult useItem(Player player, InteractionHand hand, ItemStack stack) {
        FireTuning t = FireConfig.tuning();
        if (stack.is(FireRegistry.MATCH.get())) {
            Fire.Result r = strikeMatch(player, stack, level.random.nextDouble());
            return r.ok() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
        }
        if (stack.is(FireRegistry.BOW_DRILL.get())) {
            BowDrillService.start(player, this, stack);
            return ItemInteractionResult.CONSUME;
        }
        LayPart part = FireParts.part(stack);
        if (part == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Fire.Result r = addPart(stack, t);
        tell(player, r.line());
        if (!r.ok()) return ItemInteractionResult.CONSUME;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, worldPosition, part == LayPart.FUEL ? SoundEvents.WOOD_PLACE : SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.6f, 1.0f);
        return ItemInteractionResult.SUCCESS;
    }

    /** Adds one item as a lay part (no stack shrink): the shared path of right mouse and the tests. */
    public Fire.Result addPart(ItemStack stack, FireTuning t) {
        LayPart part = FireParts.part(stack);
        if (part == null) return new Fire.Result(false, "That will not burn.");
        FuelData fuel = part == LayPart.FUEL ? FireParts.fuel(stack) : null;
        Wood wood = fuel == null ? null : fuel.woodType();
        double amount = fuel == null ? 1 : fuel.armloadFraction();
        int before = fire.lay().size();
        Fire.Result r = fire.add(part, wood, FireParts.moisture(stack), amount, t);
        if (r.ok() && fire.lay().size() > before) layStacks.add(stack.copyWithCount(1));
        if (r.ok()) {
            changed(null, null);
            refreshIndex();
        }
        return r;
    }

    InteractionResult useEmptyHand(Player player) {
        FireTuning t = FireConfig.tuning();
        FireState s = fire.state();
        if (player.isShiftKeyDown()) {
            if (s == FireState.COALS) {
                Fire.Result r = fire.bank();
                tell(player, r.line());
                changed(new Fire.Change(s, fire.state(), null), player);
                return InteractionResult.SUCCESS;
            }
            ItemStack back = takeBack();
            if (!back.isEmpty()) {
                if (!player.getInventory().add(back)) player.drop(back, false);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (s == FireState.EMBER || s == FireState.BANKED) {
            if (level.getGameTime() - lastPuffTick < 15) return InteractionResult.CONSUME;
            lastPuffTick = level.getGameTime();
            Fire.Result r = puff();
            tell(player, r.line());
            return r.ok() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (s == FireState.FIRE) {
            FireSetting next = switch (fire.setting()) {
                case LOW -> FireSetting.STEADY;
                case STEADY -> FireSetting.HOT;
                case HOT -> FireSetting.LOW;
            };
            Fire.Result r = setSetting(next);
            tell(player, r.line());
            return InteractionResult.SUCCESS;
        }
        tell(player, readout(t));
        return InteractionResult.SUCCESS;
    }

    /** Strikes one match on the lay; the match is spent unless the lay could not take a flame at all. */
    public Fire.Result strikeMatch(Player player, ItemStack match, double roll) {
        FireTuning t = FireConfig.tuning();
        String why = fire.cannotLight(t);
        if (why != null) {
            tell(player, why);
            return new Fire.Result(false, why);
        }
        if (player != null && !player.getAbilities().instabuild) match.shrink(1);
        FireState from = fire.state();
        Fire.Result r = fire.strikeMatch(roll, FireParts.moisture(match), weather(), player != null && player.isShiftKeyDown(), t);
        level.playSound(null, worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
        tell(player, r.line());
        changed(new Fire.Change(from, fire.state(), r.line()), player);
        return r;
    }

    /** The bow drill's verdict was a flame: the tinder catches. */
    public Fire.Result emberCaught(Player player) {
        FireState from = fire.state();
        Fire.Result r = fire.emberCaught(FireConfig.tuning());
        changed(new Fire.Change(from, fire.state(), r.line()), player);
        return r;
    }

    public Fire.Result puff() {
        FireState from = fire.state();
        Fire.Result r = fire.puff(FireConfig.tuning());
        if (r.ok()) changed(new Fire.Change(from, fire.state(), null), null);
        return r;
    }

    public Fire.Result bank() {
        FireState from = fire.state();
        Fire.Result r = fire.bank();
        changed(new Fire.Change(from, fire.state(), null), null);
        return r;
    }

    public Fire.Result setSetting(FireSetting s) {
        Fire.Result r = fire.setSetting(s);
        changed(null, null);
        refreshIndex();
        return r;
    }

    /** /yoi fire state. */
    public void forceState(FireState s, FireSetting setting) {
        FireState from = fire.state();
        fire.force(s, setting, FireConfig.tuning());
        layStacks.clear();
        changed(new Fire.Change(from, fire.state(), null), null);
    }

    /** /yoi fire fuel: the queue becomes {@code armloads} of dead standing spruce at 18%. */
    public void setFuel(double armloads) {
        FireState from = fire.state();
        fire.setFuel(armloads, Wood.DEAD_SPRUCE, 18);
        changed(new Fire.Change(from, fire.state(), null), null);
    }

    /** Takes the last part back off an unlit lay, as the item that went in. */
    public ItemStack takeBack() {
        Fire.LayEntry e = fire.takeBack();
        if (e == null) return ItemStack.EMPTY;
        ItemStack back = layStacks.isEmpty() ? ItemStack.EMPTY : layStacks.remove(layStacks.size() - 1);
        changed(null, null);
        return back;
    }

    /** One line: "Fire, Low. Fuel 52 min." (Every concept from the video › Fire time and the fire roster). */
    public String readout(FireTuning t) {
        int m = (int) Math.round(fire.minutesLeft(t));
        String left = m < 60 ? m + " min" : (m / 60) + " h " + (m % 60) + " min";
        return switch (fire.state()) {
            case OUT -> fire.hasTinder() ? (fire.layInOrder() ? "Laid: ready to light." : "Laid out of order.") : "Fire ring.";
            case EMBER -> "Ember. Blow on it.";
            case TINDER -> "Tinder flame.";
            case SMALL -> "Small fire. Kindling " + left + ".";
            case FIRE -> "Fire, " + Fire.word(fire.setting()) + ". Fuel " + left + "."
                    + (fire.burning() != null && fire.burning().moisturePct > com.yearofice.sim.fire.FireRules.SMOKY_MOISTURE ? " Smoky: wet wood." : "");
            case COALS -> "Coals. Out in " + left + ".";
            case BANKED -> "Banked. Relights for " + left + ".";
        };
    }

    // ---- state plumbing ------------------------------------------------------------------------------------

    private void changed(Fire.Change c, Player actor) {
        if (level == null || level.isClientSide) return;
        syncLay();
        BlockState now = level.getBlockState(worldPosition);
        if (now.getBlock() instanceof FireRingBlock) {
            BlockState want = now.setValue(FireRingBlock.BURN, FireBurn.of(fire.state(), fire.setting()))
                    .setValue(FireRingBlock.LAY, layShown());
            if (want != now) level.setBlock(worldPosition, want, Block.UPDATE_ALL);
        }
        setChanged();
        if (shownState != fire.state()) {
            FireState from = shownState;
            shownState = fire.state();
            refreshIndex();
            NeoForge.EVENT_BUS.post(new FireStateEvent(level, worldPosition, from, fire.state(), heat(), c == null ? null : c.line()));
            if (c != null && c.line() != null && actor == null) tellNearby(c.line());
        }
    }

    /** The lay as the model shows it: the top layer present. */
    private FireLay layShown() {
        if (!fire.fuel().isEmpty() && fire.state() != FireState.COALS && fire.state() != FireState.BANKED) return FireLay.FUEL;
        if (fire.kindling() > 0) return FireLay.KINDLING;
        if (fire.hasTinder()) return FireLay.TINDER;
        return FireLay.NONE;
    }

    /** Parts the fire consumed are no longer there to take back or drop. */
    private void syncLay() {
        while (layStacks.size() > fire.lay().size()) layStacks.remove(layStacks.size() - 1);
    }

    public FireHeat heat() {
        return FireHeat.of(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fire, FireConfig.tuning());
    }

    private void refreshIndex() {
        if (level != null && !level.isClientSide) FireIndex.INSTANCE.put(level, worldPosition, heat());
    }

    private void tell(Player p, String line) {
        if (p instanceof net.minecraft.server.level.ServerPlayer sp && line != null) {
            com.yearofice.api.hud.HudBus.get().offerPrompt(sp, "fire", line, 2);
        }
    }

    /** A change nobody caused right now (rain, a timer): the players within 16 blocks hear about it. */
    private void tellNearby(String line) {
        for (Player p : level.players()) {
            if (p.distanceToSqr(worldPosition.getCenter()) < 16 * 16) tell(p, line);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        shownState = fire.state();
        refreshIndex();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) FireIndex.INSTANCE.remove(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) FireIndex.INSTANCE.remove(level, worldPosition);
    }

    /** The ring was broken: an unlit lay's parts drop. */
    void removed() {
        if (level == null || level.isClientSide) return;
        if (fire.state() == FireState.OUT) {
            for (ItemStack s : layStacks) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
        }
        layStacks.clear();
        FireIndex.INSTANCE.remove(level, worldPosition);
    }

    // ---- saving --------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        Fire.Snapshot s = fire.snapshot();
        CompoundTag f = new CompoundTag();
        f.putString("state", s.state().name().toLowerCase(Locale.ROOT));
        f.putString("setting", s.setting().name().toLowerCase(Locale.ROOT));
        f.putInt("minutes_in_state", s.minutesInState());
        f.putInt("catch_minutes", s.catchMinutes());
        f.putDouble("rain_minutes", s.rainMinutes());
        f.putInt("snow_minutes", s.snowMinutes());
        f.putInt("puffs_needed", s.puffsNeeded());
        f.putInt("ember_ticks", s.emberTicks());
        f.putDouble("burned_armloads", s.burnedArmloads());
        f.putBoolean("packed_warned", s.packedWarned());
        f.putBoolean("coal_bed", s.coalBed());
        f.putDouble("tinder_moisture", s.tinderMoisture());
        f.putInt("kindling", s.kindling());
        f.putDouble("kindling_moisture", s.kindlingMoisture());
        ListTag lay = new ListTag();
        for (Fire.LayEntry e : s.lay()) {
            CompoundTag t = new CompoundTag();
            t.putString("part", e.part().name().toLowerCase(Locale.ROOT));
            if (e.wood() != null) t.putString("wood", e.wood().id());
            t.putInt("moisture", e.moisturePct());
            t.putDouble("amount", e.amount());
            lay.add(t);
        }
        f.put("lay", lay);
        ListTag fuel = new ListTag();
        for (Armload a : s.fuel()) {
            CompoundTag t = new CompoundTag();
            t.putString("wood", a.wood.id());
            t.putInt("moisture", a.moisturePct);
            t.putDouble("amount", a.amount());
            fuel.add(t);
        }
        f.put("fuel", fuel);
        tag.put("fire", f);
        ListTag stacks = new ListTag();
        for (ItemStack st : layStacks) if (!st.isEmpty()) stacks.add(st.save(regs));
        tag.put("lay_stacks", stacks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("fire")) {
            CompoundTag f = tag.getCompound("fire");
            List<Fire.LayEntry> lay = new ArrayList<>();
            for (Tag el : f.getList("lay", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) el;
                lay.add(new Fire.LayEntry(LayPart.valueOf(t.getString("part").toUpperCase(Locale.ROOT)),
                        t.contains("wood") ? Wood.byId(t.getString("wood")) : null, t.getInt("moisture"), t.getDouble("amount")));
            }
            List<Armload> fuel = new ArrayList<>();
            for (Tag el : f.getList("fuel", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) el;
                if (t.getDouble("amount") > 0) fuel.add(new Armload(Wood.byId(t.getString("wood")), t.getInt("moisture"), t.getDouble("amount")));
            }
            fire = Fire.restore(new Fire.Snapshot(
                    FireState.valueOf(f.getString("state").toUpperCase(Locale.ROOT)),
                    FireSetting.valueOf(f.getString("setting").toUpperCase(Locale.ROOT)),
                    f.getInt("minutes_in_state"), f.getInt("catch_minutes"), f.getDouble("rain_minutes"), f.getInt("snow_minutes"),
                    f.getInt("puffs_needed"), f.getInt("ember_ticks"), f.getDouble("burned_armloads"), f.getBoolean("packed_warned"),
                    f.getBoolean("coal_bed"), f.getDouble("tinder_moisture"), f.getInt("kindling"), f.getDouble("kindling_moisture"), lay, fuel));
        }
        layStacks.clear();
        for (Tag el : tag.getList("lay_stacks", Tag.TAG_COMPOUND)) ItemStack.parse(regs, el).ifPresent(layStacks::add);
        shownState = fire.state();
    }
}
