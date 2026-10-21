package com.yearofice.food.sourdough;

import com.yearofice.api.build.RoomService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.story.AmbitionTracker;
import com.yearofice.food.FoodItems;
import com.yearofice.sim.food.Starter;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A crock of sourdough starter (B6). It steps while it is carried, by the air it sits in: a warm kitchen keeps it
 * Active, a cold cache lets it sleep, a freeze can kill it. Feed it flour from the other hand; two feeds revive a
 * hungry one. The ambition {@code sourdough} opens when you first hold one and closes when one has lived through a winter.
 */
public class StarterItem extends Item {
    public static final String AMBITION = "sourdough";
    /** Nov 1 and Apr 30 as days of the year. */
    public static final int WINTER_FROM_DOY = 184, WINTER_TO_DOY = 364;

    public StarterItem(Properties p) { super(p); }

    public static StarterComp of(ItemStack s, long minute) {
        StarterComp c = s.get(SourdoughRegistry.STARTER.get());
        if (c == null) { c = StarterComp.fresh(minute); s.set(SourdoughRegistry.STARTER.get(), c); }
        return c;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(level instanceof ServerLevel sl) || holder.tickCount % 200 != 3) return;
        long now = ClockService.get().nowMinute();
        StarterComp c = of(stack, now);
        double dt = now - c.lastTickMinute();
        if (dt >= 1) {
            double temp = tempAt(sl, holder);
            Starter.Data d = Starter.advance(c.data(), dt, temp, new java.util.Random(sl.random.nextLong()));
            stack.set(SourdoughRegistry.STARTER.get(), c.with(d, now));
            c = stack.get(SourdoughRegistry.STARTER.get());
        }
        if (holder instanceof ServerPlayer p) {
            AmbitionTracker.open(p, AMBITION);
            long day = now / 1440;
            if (c.data().state() != Starter.State.DEAD && Calendar.dayOfYearOfDay(day) == WINTER_TO_DOY
                    && day - c.bornMinute() / 1440 >= 150 && c.bornMinute() / 1440 < day) AmbitionTracker.complete(p, AMBITION);
        }
    }

    static double tempAt(ServerLevel sl, Entity holder) {
        var room = RoomService.get().roomAt(sl, holder.blockPosition());
        return room != null ? room.feelsLikeC() : EnvService.get().airC(sl, holder.blockPosition());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide || !other.is(FoodItems.BARLEY_FLOUR.get())) return InteractionResultHolder.pass(s);
        long now = ClockService.get().nowMinute();
        StarterComp c = of(s, now);
        if (c.data().state() == Starter.State.DEAD) return InteractionResultHolder.fail(s);
        s.set(SourdoughRegistry.STARTER.get(), c.with(Starter.feed(c.data(), now), c.lastTickMinute()));
        if (!player.getAbilities().instabuild) other.shrink(1);
        return InteractionResultHolder.success(s);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        StarterComp c = stack.get(SourdoughRegistry.STARTER.get());
        if (c == null) return;
        tip.add(Component.literal(c.data().state().name().charAt(0) + c.data().state().name().substring(1).toLowerCase(java.util.Locale.ROOT)));
    }
}
