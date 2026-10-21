package com.yearofice.hunt;

import com.yearofice.api.body.BodyService;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Weapon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A bow (data {@code weapon/<id>.json}, Blueprint › Weapons and arrowheads). Hold right mouse to draw; the draw takes
 * the bow's ready time (x1.5 under 3 bolts), is capped at 60% when Strength is under the bow's requirement, and past
 * the hold limit the aim shakes at triple sway. Release looses the first {@code yearofice:arrow} in the pack.
 * Holding costs the body a little each second (BodyService.effort).
 */
public class BowItem extends Item {
    public static final int USE_TICKS = 72_000;
    /** One bolt of energy as kcal for the hold cost, until BodyService.effort exists. */
    static final double KCAL_PER_BOLT = 40;

    private final String weaponId;

    public BowItem(Properties p, String weaponId) {
        super(p);
        this.weaponId = weaponId;
    }

    public Weapon weapon() { return HuntData.weapon(weaponId); }

    @Override
    public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }

    @Override
    public int getUseDuration(ItemStack s, LivingEntity e) { return USE_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack bow = p.getItemInHand(hand);
        if (findArrow(p).isEmpty() && !p.getAbilities().instabuild) return InteractionResultHolder.fail(bow);
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    static ItemStack findArrow(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(HuntRegistry.ARROW.get())) return s;
        }
        return ItemStack.EMPTY;
    }

    static double strength(Player p) {
        try { return p instanceof ServerPlayer sp ? BodyService.get().strength(sp) : 100; } catch (IllegalStateException e) { return 100; }
    }

    static double energyBolts(Player p) {
        try { return p instanceof ServerPlayer sp ? BodyService.get().energy(sp) : 10; } catch (IllegalStateException e) { return 10; }
    }

    /** The draw fraction after {@code heldTicks}. */
    public double drawAfter(Player p, int heldTicks) {
        Weapon w = weapon();
        double sec = w.drawSeconds(energyBolts(p), HuntConfig.tuning());
        double cap = w.maxDraw(strength(p), HuntConfig.tuning().cappedDrawPct());
        return Math.min(cap, sec <= 0 ? cap : heldTicks / 20.0 / sec);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity who, int timeLeft) {
        if (level.isClientSide || !(who instanceof ServerPlayer p)) return;
        int held = USE_TICKS - timeLeft;
        double heldS = held / 20.0;
        double draw = drawAfter(p, held);
        if (draw < 0.15) return; // a tap lets down
        ItemStack ammo = findArrow(p);
        String head = ammo.isEmpty() ? HuntItems.DEFAULT_HEAD : HuntItems.headOf(ammo);
        if (!p.getAbilities().instabuild && !ammo.isEmpty()) ammo.shrink(1);
        Weapon w = weapon();
        Loose.fire(p, w, head, draw, heldS, bow);
        int kcal = (int) Math.round(Math.max(0, heldS - w.drawTimeS()) * HuntConfig.tuning().holdBoltsPerS() * KCAL_PER_BOLT);
        if (kcal > 0) com.yearofice.api.body.BodyService.get().effort(p, "bow_hold", kcal, 0);
        bow.hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /** Looses an arrow: speed from the draw, aim wandered by sway. */
    static final class Loose {
        static ArrowEntity fire(ServerPlayer p, Weapon w, String head, double draw, double heldS, ItemStack bow) {
            var t = HuntConfig.tuning();
            double swayCm = w.swayCm(20, 0, 0, heldS, t);
            double rad = swayCm / 100.0 / 20.0; // angle at 20 m
            Vec3 look = p.getLookAngle();
            var r = p.getRandom();
            double a = r.nextDouble() * Math.PI * 2, m = Math.sqrt(r.nextDouble()) * rad;
            Vec3 up = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = look.cross(up).normalize(), upv = right.cross(look).normalize();
            Vec3 dir = look.add(right.scale(Math.cos(a) * m)).add(upv.scale(Math.sin(a) * m)).normalize();
            ArrowEntity e = new ArrowEntity(HuntRegistry.ARROW_ENTITY.get(), p.level());
            e.launch(p, p.getEyePosition().add(dir.scale(0.5)), dir, w.speedAt(draw), w.energyAt(draw), w, head);
            p.level().addFreshEntity(e);
            return e;
        }
    }
}
