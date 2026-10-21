package com.yearofice.client.work;

import com.yearofice.build.wood.ChoppingBlockEntity;
import com.yearofice.build.wood.FellingService;
import com.yearofice.build.wood.ToolData;
import com.yearofice.build.wood.WoodBlocks;
import com.yearofice.build.wood.WoodConfig;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.build.wood.WoodSystem;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.client.hud.ClientHud;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.ToolStats;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.world.logs.FelledLogEntity;
import com.yearofice.worldgen.block.YoiLogBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The client half of hold-to-work for wood (Player systems and client › Hold-to-work and the use key): a use press
 * under 0.25 s stays a tap; at 0.25 s on a wood target with the right thing in hand the client sends
 * {@code work_start}, beats it every 5 ticks while held, and cancels vanilla's repeat-use. Before the hold it shows
 * the job's preview in the prompt row; while the ring fills it predicts between the server's {@code work_progress}.
 * The server decides everything; this only shows.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class ClientWork {
    private ClientWork() {}

    static final int HOLD_TICKS = 5, BEAT_TICKS = 5;

    private static int held;
    private static long sentKey = Long.MIN_VALUE;
    private static int sinceBeat;
    private static volatile WorkProgressPayload last = WorkProgressPayload.idle();
    private static volatile long lastAt;

    /** What the ring shows: share (predicted), phase and counters; null when no job runs. */
    public record Ring(float share, WorkProgressPayload.Phase phase, int done, int of) {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        WoodSystem.WorkSink.sink = p -> {
            last = p;
            lastAt = System.currentTimeMillis();
        };
    }

    public static @Nullable Ring ring() {
        WorkProgressPayload p = last;
        if (!p.active() || System.currentTimeMillis() - lastAt > 1500) return null;
        float ticks = (System.currentTimeMillis() - lastAt) / 50f;
        float share = Math.min(1f, p.share() + p.perTick() * ticks);
        WorkProgressPayload.Phase[] ph = WorkProgressPayload.Phase.values();
        return new Ring(share, ph[Math.max(0, Math.min(ph.length - 1, p.phase()))], p.done(), p.of());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || p.isSpectator()) { release(); held = 0; return; }
        WorkStartPayload target = target(mc, p);
        boolean down = mc.options.keyUse.isDown();
        if (target == null || !down) {
            release();
            held = down ? held + 1 : 0;
            if (target != null && !down) preview(mc, p);
            return;
        }
        held++;
        if (held < HOLD_TICKS) return;
        sinceBeat++;
        if (target.targetKey() != sentKey || sinceBeat >= BEAT_TICKS) {
            PacketDistributor.sendToServer(target);
            sentKey = target.targetKey();
            sinceBeat = 0;
        }
    }

    static void release() {
        if (sentKey != Long.MIN_VALUE) {
            PacketDistributor.sendToServer(WorkStartPayload.release());
            sentKey = Long.MIN_VALUE;
        }
        sinceBeat = 0;
    }

    /** Vanilla's use is cancelled on a wood target, so the hold is ours. */
    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isUseItem() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && com.yearofice.build.pieces.PlaceJob.row(mc.player.getMainHandItem()) == null && target(mc, mc.player) != null) {
            e.setSwingHand(false);
            e.setCanceled(true);
        }
    }

    /** The heartbeat for what the crosshair is on, or null when it is not a wood job with what is in hand. */
    static @Nullable WorkStartPayload target(Minecraft mc, LocalPlayer p) {
        HitResult h = mc.hitResult;
        ItemStack hand = p.getMainHandItem();
        ToolStats tool = ToolData.of(hand);
        if (h instanceof EntityHitResult eh && eh.getEntity() instanceof FelledLogEntity log && tool != null) {
            return new WorkStartPayload(true, BlockPos.ZERO, log.getId(), (byte) 0, (float) h.getLocation().x, (float) h.getLocation().y, (float) h.getLocation().z);
        }
        if (!(h instanceof BlockHitResult bh) || h.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = bh.getBlockPos();
        BlockState s = p.level().getBlockState(pos);
        if (boxHold(p, s, hand)) {
            return new WorkStartPayload(true, pos, -1, (byte) bh.getDirection().get3DDataValue(),
                    (float) bh.getLocation().x, (float) bh.getLocation().y, (float) bh.getLocation().z);
        }
        boolean ok;
        if (com.yearofice.build.pieces.PlaceJob.row(hand) != null) {
            ok = true; // a building piece in hand: the server holds it as a job unless [pieces] instant_place
        } else if (WoodItems.isRound(hand) && p.isShiftKeyDown() && bh.getDirection() == Direction.UP) {
            ok = p.level().getBlockState(pos.above()).canBeReplaced() && s.isFaceSturdy(p.level(), pos, Direction.UP);
        } else if (com.yearofice.food.pan.PanTargets.isPan(hand) && com.yearofice.food.pan.PanTargets.isGravel(s)) {
            ok = true;
        } else if (tool == null) {
            ok = false;
        } else if (s.getBlock() instanceof YoiLogBlock) {
            ok = true;
        } else {
            ok = p.level().getBlockEntity(pos) instanceof ChoppingBlockEntity be && be.hasRound();
        }
        if (!ok) return null;
        return new WorkStartPayload(true, pos, -1, (byte) bh.getDirection().get3DDataValue(),
                (float) bh.getLocation().x, (float) bh.getLocation().y, (float) bh.getLocation().z);
    }

    /** A Shift + use hold that lifts, chips or sets down a box (A7): the server decides which, and whether it can. */
    static boolean boxHold(LocalPlayer p, BlockState s, ItemStack hand) {
        if (!p.isShiftKeyDown()) return false;
        if (com.yearofice.item.inventory.BoxCarry.carrying(p)) return true;
        return s.is(com.yearofice.item.inventory.BoxCarry.LIFTABLE)
                && (hand.isEmpty() || hand.is(com.yearofice.item.inventory.BoxCarry.CHIP_TOOLS));
    }

    /** The prompt row before the hold: "Hold RMB  Fell white spruce: 24 cm, 6 swings, 24 min, 180 kcal". */
    static void preview(Minecraft mc, LocalPlayer p) {
        Component key = mc.options.keyUse.getTranslatedKeyMessage();
        ToolStats tool = ToolData.of(p.getMainHandItem());
        HitResult h = mc.hitResult;
        double scale = WoodConfig.get("work_scale");
        if (h instanceof EntityHitResult eh && eh.getEntity() instanceof FelledLogEntity log && tool != null) {
            if (!log.limbed()) ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.limb", key, fmt(tool.limbMin() * scale)));
            else ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.buck", key, log.roundsLeft(),
                    fmt(Bucking.roundMinutes(tool.buckMin()) * scale)));
            return;
        }
        if (!(h instanceof BlockHitResult bh)) return;
        BlockPos pos = bh.getBlockPos();
        BlockState s = p.level().getBlockState(pos);
        if (tool == null) {
            if (WoodItems.isRound(p.getMainHandItem())) ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.set_block", key));
            return;
        }
        if (p.level().getBlockEntity(pos) instanceof ChoppingBlockEntity be && be.hasRound()) {
            ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.split", key, tool.splitStrikes(), tool.splitCrackStrikes()));
            return;
        }
        if (!(s.getBlock() instanceof YoiLogBlock)) return;
        if (WoodBlocks.placedState(s)) {
            ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.cut_placed", key, fmt(Bucking.placedBlockMinutes(tool.buckMin()) * scale)));
            return;
        }
        TrunkSearch.Trunk t = TrunkSearch.find(WoodBlocks.probe(p.level()), pos.getX(), pos.getY(), pos.getZ());
        if (t == null) {
            ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.cut_placed", key, fmt(tool.buckMin() * scale)));
            return;
        }
        if (p.isShiftKeyDown()) {
            ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.cut_top", key,
                    fmt(Bucking.sneakTopMinutes(tool.buckMin(), WoodConfig.get("sneak_cut_time_mult")) * scale)));
            return;
        }
        TreeKind k = FellingService.kind(t);
        double d = k.diameterCm(t.height());
        if (!Felling.canFell(tool, d)) {
            ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.too_thick", Math.round(d), Math.round(tool.fellMaxCm())));
            return;
        }
        int n = Felling.swings(tool.swings25(), d);
        double min = n * tool.swingMin() * WoodConfig.get("swing_scale");
        ClientHud.localPrompt("wood", Component.translatable("prompt.yearofice.wood.fell", key,
                Component.translatable("tree.yearofice." + k.id()), Math.round(d), n, fmt(min), Math.round(Felling.kcal(min))));
    }

    static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v); }
}
