package com.yearofice.fire;

import com.yearofice.api.fire.FireEffortEvent;
import com.yearofice.fire.net.BowDrillOpenPayload;
import com.yearofice.fire.net.MinigameResultPayload;
import com.yearofice.sim.fire.BowDrill;
import com.yearofice.sim.fire.BowDrillJudge;
import com.yearofice.sim.fire.Fire;
import com.yearofice.sim.fire.FireTuning;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server half of the bow drill: opens the client's screen at a laid ring, remembers when the attempt started,
 * and judges the result the client sends back ({@link BowDrillJudge}). The judge's verdict lights the ring, wears
 * the drill, counts practice and reports the 60 kcal and the close-clock minutes as a {@link FireEffortEvent}.
 */
public final class BowDrillService {
    private BowDrillService() {}

    private record Session(BlockPos pos, long startTick, boolean sheltered) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    static void clear() { SESSIONS.clear(); }

    /** Right mouse with the bow drill on a ring. */
    static void start(Player player, FireRingBlockEntity be, ItemStack drill) {
        FireTuning t = FireConfig.tuning();
        String why = be.fire().cannotLight(t);
        if (why != null) {
            player.displayClientMessage(Component.literal(why), true);
            return;
        }
        if (!(player instanceof ServerPlayer sp)) return;
        boolean sheltered = player.isShiftKeyDown();
        int emberTicks = Fire.emberLifeTicks(be.weather().windMs(), sheltered, t);
        boolean boardWet = FireParts.moisture(drill) > t.tinderMaxMoisture();
        boolean practiced = BowDrill.practiced(FireSkill.reps(player), t);
        SESSIONS.put(player.getUUID(), new Session(be.getBlockPos().immutable(), player.level().getGameTime(), sheltered));
        PacketDistributor.sendToPlayer(sp, new BowDrillOpenPayload(be.getBlockPos(), boardWet, emberTicks, practiced));
    }

    /** The client's result for an open attempt. */
    public static void onResult(ServerPlayer player, MinigameResultPayload p) {
        if (!MinigameResultPayload.BOW_DRILL.equals(p.minigame())) return;
        Session s = SESSIONS.remove(player.getUUID());
        if (s == null || !s.pos().equals(p.pos())) return;
        Level level = player.level();
        if (!(level.getBlockEntity(s.pos()) instanceof FireRingBlockEntity be)) return;
        if (player.distanceToSqr(s.pos().getCenter()) > 6 * 6) return;
        ItemStack drill = heldDrill(player);
        FireTuning t = FireConfig.tuning();
        double elapsedS = (level.getGameTime() - s.startTick()) / 20.0;
        BowDrillJudge.Facts facts = new BowDrillJudge.Facts(elapsedS, FireSkill.reps(player),
                drill.isEmpty() ? 100 : FireParts.moisture(drill), be.fire().tinderMoisture(), FireSkill.energyBolts(player));
        BowDrillJudge.Summary summary = drill.isEmpty()
                ? new BowDrillJudge.Summary(BowDrillJudge.Claim.ABORTED, p.strokes(), p.onBeat(), p.peakHeat(), p.goodPuffs(), p.held())
                : p.summary();
        apply(player, be, drill, BowDrillJudge.judge(summary, facts, t), t);
    }

    /** Applies a verdict: the effort, the wear, the practice, and the flame. Public for /yoi fire drill and tests. */
    public static void apply(Player player, FireRingBlockEntity be, ItemStack drill, BowDrillJudge.Verdict v, FireTuning t) {
        if (v.kcal() > 0) NeoForge.EVENT_BUS.post(new FireEffortEvent(player, "bow_drill", v.kcal(), v.gameMinutes()));
        if (v.reps() > 0) {
            FireSkill.addReps(player, v.reps(), t);
            if (!drill.isEmpty() && !player.getAbilities().instabuild) {
                EquipmentSlot slot = player.getItemInHand(InteractionHand.MAIN_HAND) == drill ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                drill.hurtAndBreak(1, player, slot);
            }
        }
        if (v.lit()) {
            Fire.Result r = be.emberCaught(player);
            player.displayClientMessage(Component.literal(r.ok() ? v.line() : r.line()), true);
        } else if (v.line() != null) {
            player.displayClientMessage(Component.literal(v.line()), true);
        }
    }

    private static ItemStack heldDrill(Player p) {
        if (p.getMainHandItem().is(FireRegistry.BOW_DRILL.get())) return p.getMainHandItem();
        if (p.getOffhandItem().is(FireRegistry.BOW_DRILL.get())) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }
}
