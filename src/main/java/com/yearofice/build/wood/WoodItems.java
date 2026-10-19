package com.yearofice.build.wood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.api.clock.ClockService;
import com.yearofice.fire.FireRegistry;
import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.wood.WoodKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Rounds and splits as stacks: making them with a moisture and settling their seasoning (B4). A piece in a pack or
 * a hand counts as under cover (the rack's rate) and an unsplit round dries at the round's share; stacks are
 * settled on every game day and whenever a job hands one over, so the shared {@code yearofice:moisture} that the
 * fire reads is never more than a day behind.
 */
public final class WoodItems {
    private WoodItems() {}

    /** {@code yearofice:seasoning}: the exact moisture % and the absolute day it was last settled. */
    public record Stamp(double moisture, long day) {
        public static final Codec<Stamp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 100).fieldOf("moisture_pct").forGetter(Stamp::moisture),
                Codec.LONG.fieldOf("day").forGetter(Stamp::day)).apply(i, Stamp::new));
        public static final StreamCodec<ByteBuf, Stamp> STREAM = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Stamp::moisture, ByteBufCodecs.VAR_LONG, Stamp::day, Stamp::new);
    }

    public static long today() { return ClockService.installed() ? ClockService.get().absDay() : 0; }

    public static @Nullable WoodPieceItem piece(ItemStack stack) { return stack.getItem() instanceof WoodPieceItem w ? w : null; }

    public static boolean isRound(ItemStack s) { return piece(s) != null && piece(s).form == Seasoning.Form.ROUND; }

    public static boolean isSplit(ItemStack s) { return piece(s) != null && piece(s).form == Seasoning.Form.SPLIT; }

    public static ItemStack round(WoodKind w, int count, double moisture, long day) {
        return stamp(new ItemStack(WoodRegistry.ROUNDS.get(w).get(), count), moisture, day);
    }

    public static ItemStack split(WoodKind w, int count, double moisture, long day) {
        return stamp(new ItemStack(WoodRegistry.SPLITS.get(w).get(), count), moisture, day);
    }

    /** Sets both components: the exact stamp and the whole-percent moisture every reader uses. */
    public static ItemStack stamp(ItemStack s, double moisture, long day) {
        double m = Math.max(0, Math.min(100, moisture));
        s.set(WoodRegistry.SEASONING.get(), new Stamp(m, day));
        s.set(FireRegistry.MOISTURE.get(), (int) Math.round(m));
        return s;
    }

    /** The exact moisture now, carried under cover, from the stamp (or the whole-percent component). */
    public static double moistureNow(ItemStack s, long day) {
        WoodPieceItem p = piece(s);
        Stamp st = s.get(WoodRegistry.SEASONING.get());
        if (st == null) {
            Integer m = s.get(FireRegistry.MOISTURE.get());
            return m != null ? m : Seasoning.SNAG_PCT;
        }
        if (p == null) return st.moisture();
        return Seasoning.dry(WoodConfig.rates(), st.moisture(), st.day(), day, Seasoning.Place.RACK, p.form);
    }

    /** Whole-percent moisture as shown (the shared component). */
    public static int moisturePct(ItemStack s) {
        Integer m = s.get(FireRegistry.MOISTURE.get());
        if (m != null) return m;
        Stamp st = s.get(WoodRegistry.SEASONING.get());
        return st != null ? (int) Math.round(st.moisture()) : (int) Seasoning.SNAG_PCT;
    }

    /** Brings a wood stack's seasoning up to {@code day}; returns true if it changed. */
    public static boolean settle(ItemStack s, long day) {
        if (piece(s) == null) return false;
        Stamp st = s.get(WoodRegistry.SEASONING.get());
        if (st != null && st.day() >= day) return false;
        stamp(s, moistureNow(s, day), day);
        return true;
    }

    /** Settles every wood stack a player carries. */
    public static void settle(Player p, long day) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) settle(inv.getItem(i), day);
    }

    /** Gives {@code s} to the player, or drops it at their feet when there is no open slot. */
    public static void give(Player p, ItemStack s) {
        if (s.isEmpty()) return;
        if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
    }
}
