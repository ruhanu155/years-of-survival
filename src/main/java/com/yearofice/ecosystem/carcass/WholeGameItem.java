package com.yearofice.ecosystem.carcass;

import com.yearofice.api.body.BodyService;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.Fresh;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * {@code yearofice:whole_game}: a whole small carcass carried as an item (Bestiary › In Minecraft: carcasses:
 * "Tiny and small carcasses become the item yearofice:whole_game with the data component yearofice:carcass"). It
 * carries the carcass record (species, weight, cause, death minute) and {@code yearofice:fresh}. Holding use dresses
 * it ({@link Butchering}): a hold-to-work job run as a time-lapse.
 */
public class WholeGameItem extends Item {
    public WholeGameItem(Properties props) { super(props); }

    public static ItemStack of(CarcassRecord r) {
        ItemStack s = new ItemStack(EcosystemRegistry.WHOLE_GAME.get());
        s.set(EcosystemRegistry.CARCASS_COMPONENT.get(), r);
        s.set(EcosystemRegistry.FRESH.get(), r.fresh());
        return s;
    }

    public static CarcassRecord record(ItemStack s) { return s.get(EcosystemRegistry.CARCASS_COMPONENT.get()); }

    @Override
    public Component getName(ItemStack stack) {
        CarcassRecord r = record(stack);
        if (r == null) return super.getName(stack);
        return Component.translatable("item.yearofice.whole_game.of", Component.translatable("entity." + r.species().replace(':', '.')));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        CarcassRecord r = record(stack);
        if (r == null) return;
        lines.add(Component.translatable("item.yearofice.whole_game.weight", String.format("%.2f", r.weightG() / 1000.0)));
        Fresh f = stack.get(EcosystemRegistry.FRESH.get());
        if (f != null && Butchering.clientLevel() != null) {
            double fr = f.freshness(Freshness.dNow(Butchering.clientLevel()));
            int stage = DecayLadder.FOOD.stageOf(f.progress(Freshness.dNow(Butchering.clientLevel())));
            lines.add(Component.translatable("item.yearofice.whole_game.fresh", Math.round(fr / 5.0) * 5,
                    Component.translatable("decay.yearofice.food." + stage)));
        }
        lines.add(Component.translatable("item.yearofice.whole_game.hint"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (record(stack) == null) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp && BodyService.get().energy(sp) < EcoConfig.tuning().minWorkEnergy()) {
            sp.displayClientMessage(Component.translatable("ecosystem.yearofice.too_tired"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        CarcassRecord r = record(stack);
        return r == null ? 0 : Butchering.holdTicks(r);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BRUSH; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer sp) Butchering.workTick(sp, remaining);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) return Butchering.finish(sp, stack);
        return stack;
    }
}
