package com.yearofice.hunt;

import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.hunt.wound.WoundTracker;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Shot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The spear (data {@code weapon/spear.json}): a thrust at a downed or snared animal's vitals counts as a heart-and-lung
 * hit; at a bedded wounded animal it finishes it. The throw (fish and hares) is not built yet.
 */
public class SpearItem extends Item {
    public SpearItem(Properties p) { super(p); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player p, LivingEntity target, InteractionHand hand) {
        if (!(p.level() instanceof ServerLevel sl) || p.distanceTo(target) > HuntData.weapon("spear").thrustReachM() + 1) return InteractionResult.PASS;
        var st = WoundTracker.of(target);
        boolean downed = st != null && st.bedded || target.getHealth() < target.getMaxHealth() * 0.25 || target.isSleeping();
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        double threshold;
        try { threshold = HuntData.zones(species).killThresholdJ(); } catch (RuntimeException e) { return InteractionResult.PASS; }
        Shot.Outcome o = Shot.spear(true, downed, threshold);
        if (o.result() == Shot.Result.KILL) {
            if (target instanceof YoiAnimal a) DeathPipeline.kill(a, sl.damageSources().playerAttack(p), "spear");
            else target.kill();
            stack.hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        } else {
            target.setLastHurtByMob(p);
        }
        return InteractionResult.sidedSuccess(sl.isClientSide);
    }
}
