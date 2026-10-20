package com.yearofice.item.inventory;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.pack.LoadBands;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The server half of the worn pack (Minecraft engine › Inventory and packs, Taking off a pack) and of the load
 * modifier.
 *
 * <ul>
 *   <li><b>The pack holds its rows.</b> While a pack is worn, rows A–C are written through to its
 *       {@code minecraft:container} component whenever a row differs from the last write, so the pack always carries what is in
 *       it. Taken off, its rows close and their slots empty: what was written travels with the pack item. A stack
 *       that reached a row after the last write (the same tick) is given back to the belt or dropped, never lost.
 *       Put on, the pack's contents fill its rows.</li>
 *   <li><b>Load.</b> On every inventory or pack change, and every 10 ticks for direct slot writes and Strength, the band sets the
 *       {@code yearofice:load} movement-speed modifier, and no running from 30 kg (at Strength multiplier 1).</li>
 * </ul>
 */
public final class PackSync {
    private PackSync() {}

    public static final ResourceLocation LOAD = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "load");
    static final int SLOTS = PackRules.ROW_END - PackRules.ROW_START;

    static final class State {
        ItemStack worn = ItemStack.EMPTY;
        int rows;
        final ItemStack[] written = new ItemStack[SLOTS];
        int changed = Integer.MIN_VALUE;
        int sinceLoad;
        LoadBands.Band band = LoadBands.Band.LIGHT;
        com.yearofice.net.payload.PackReadoutPayload readout;

        State() { java.util.Arrays.fill(written, ItemStack.EMPTY); }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new State()); }

    static void forget(UUID id) { STATES.remove(id); }

    static void clear() { STATES.clear(); }

    /** One player's tick: pack on, off or changed, then the load. */
    public static void tick(ServerPlayer p) {
        State s = state(p);
        Inventory inv = p.getInventory();
        ItemStack back = PackSlots.back(p);
        boolean isPack = PackRules.isPack(back);
        boolean packChanged = false;
        if (!s.worn.isEmpty() && (back != s.worn || !isPack)) {
            takeOff(p, s);
            packChanged = true;
        }
        if (isPack && s.worn.isEmpty()) {
            putOn(p, s, back);
            packChanged = true;
        }
        boolean rowsChanged = !s.worn.isEmpty() && (packChanged || rowsDiffer(inv, s));
        if (rowsChanged) write(p, s);
        if (packChanged || rowsChanged || inv.getTimesChanged() != s.changed || ++s.sinceLoad >= 10) {
            s.changed = inv.getTimesChanged();
            s.sinceLoad = 0;
            applyLoad(p, s);
        }
        if (!s.band.canRun() && p.isSprinting()) p.setSprinting(false);
    }

    /** Forces the write-through and the load check now (commands and tests that set slots directly). */
    public static void refresh(ServerPlayer p) {
        tick(p);
        State s = state(p);
        if (!s.worn.isEmpty()) write(p, s);
        applyLoad(p, s);
    }

    static boolean rowsDiffer(Inventory inv, State s) {
        for (int i = 0; i < SLOTS; i++) if (!ItemStack.matches(inv.getItem(PackRules.ROW_START + i), s.written[i])) return true;
        return false;
    }

    static void putOn(ServerPlayer p, State s, ItemStack pack) {
        s.worn = pack;
        s.rows = PackRules.openRows(p);
        Inventory inv = p.getInventory();
        ItemContainerContents c = pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> in = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        c.copyInto(in);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = in.get(i);
            if (st.isEmpty()) continue;
            int slot = PackRules.ROW_START + i;
            if (PackRules.isOpen(p, slot) && inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, st.copy());
            } else if (!inv.add(st.copy())) {
                p.drop(st.copy(), false);
            }
        }
        java.util.Arrays.fill(s.written, ItemStack.EMPTY);
    }

    /** Rows into the worn pack's container component. */
    static void write(ServerPlayer p, State s) {
        Inventory inv = p.getInventory();
        NonNullList<ItemStack> out = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = inv.getItem(PackRules.ROW_START + i);
            out.set(i, st.copy());
            s.written[i] = st.copy();
        }
        ItemContainerContents next = ItemContainerContents.fromItems(out);
        if (!next.equals(s.worn.get(DataComponents.CONTAINER))) s.worn.set(DataComponents.CONTAINER, next);
    }

    static void takeOff(ServerPlayer p, State s) {
        Inventory inv = p.getInventory();
        java.util.List<ItemStack> giveBack = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            int slot = PackRules.ROW_START + i;
            ItemStack cur = inv.getItem(slot);
            if (cur.isEmpty()) continue;
            if (!ItemStack.matches(cur, s.written[i])) giveBack.add(cur.copy());
            inv.setItem(slot, ItemStack.EMPTY);
        }
        s.worn = ItemStack.EMPTY;
        s.rows = 0;
        java.util.Arrays.fill(s.written, ItemStack.EMPTY);
        for (ItemStack st : giveBack) if (!inv.add(st)) p.drop(st, false);
    }

    static void applyLoad(ServerPlayer p, State s) {
        LoadBands bands = PackConfig.bands();
        s.band = LoadCalculator.bandOf(p, LoadCalculator.kgOf(p));
        sendReadout(p, s, bands);
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double f = bands.speed(s.band) - 1;
        AttributeModifier cur = speed.getModifier(LOAD);
        if (cur != null && cur.amount() == f) return;
        speed.removeModifier(LOAD);
        if (f != 0) speed.addTransientModifier(new AttributeModifier(LOAD, f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Strength-scaled band edges and the cap, sent to the pack screen when they change. */
    static void sendReadout(ServerPlayer p, State s, LoadBands bands) {
        double strength = LoadCalculator.strength(p), reps = PackSkill.reps(p);
        double[] e = bands.edgesKg(strength, reps);
        var r = new com.yearofice.net.payload.PackReadoutPayload((float) strength, (float) reps, (float) e[0],
                (float) e[1], (float) e[2], (float) LoadCalculator.maxKgOf(p), s.band.ordinal());
        if (r.equals(s.readout)) return;
        s.readout = r;
        com.yearofice.net.YoiNet.send(p, r);
    }

    /** What the pack screen was last told (tests). */
    public static com.yearofice.net.payload.PackReadoutPayload readout(ServerPlayer p) { return state(p).readout; }

    /** The band the last load check found (tests and {@code /yoi pack}). */
    public static LoadBands.Band band(ServerPlayer p) { return state(p).band; }
}
