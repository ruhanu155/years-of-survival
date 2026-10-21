package com.yearofice.build.wood;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.build.craft.Wear;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload.Phase;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.fire.FireRegistry;
import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.FellingGame;
import com.yearofice.sim.wood.Splitting;
import com.yearofice.sim.wood.ToolStats;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.world.logs.FelledLogEntity;
import com.yearofice.worldgen.block.StumpBlock;
import com.yearofice.worldgen.block.YoiLogBlock;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The wood jobs of hold-to-work (Trees, felling and firewood; video 2, A1), and the resolver that picks one for a held
 * target:
 * <ul>
 *   <li>a natural trunk with an axe: {@link Fell} (the notch, then the back cut); with Sneak, {@link CutTop};</li>
 *   <li>a placed log (or a lone log not rooted in soil): {@link CutBlock}, that one block only;</li>
 *   <li>a felled log: {@link Limb} while its crown is on, then {@link Buck} into rounds;</li>
 *   <li>a chopping block with a round on it: {@link Split};</li>
 *   <li>Sneak with a round in hand on the top of a block: {@link SetBlock}, a new chopping block.</li>
 * </ul>
 * Every time is game minutes, filled at the Work rate; the tool's numbers come from {@code data_maps/item/tool.json}.
 */
public final class WoodJobs {
    private WoodJobs() {}

    static final long DS_PER_MIN = 600;

    static void say(ServerPlayer p, String id, HudArg... args) {
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.PROMPT, "wood_" + id, "building", args));
    }

    static double reach() { return WoodConfig.get("work_reach_m"); }

    static boolean inReach(ServerPlayer p, Vec3 at) { return p.getEyePosition().distanceTo(at) <= reach() + 1.0; }

    static @Nullable ToolStats tool(ServerPlayer p) { return ToolData.of(p.getMainHandItem()); }

    static long minutesDs(double minutes) { return Math.max(1, Math.round(minutes * DS_PER_MIN)); }

    static void chips(ServerLevel l, BlockPos at, BlockState s) {
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
    }

    // ---- the resolver -----------------------------------------------------------------------------------------

    /** The job for a held target, or null (posting why, where the player should know). */
    public static @Nullable WorkJob resolve(ServerPlayer p, WorkStartPayload w) {
        ServerLevel level = p.serverLevel();
        ItemStack held = p.getMainHandItem();
        ToolStats tool = ToolData.of(held);
        if (w.entity() >= 0) {
            Entity e = level.getEntity(w.entity());
            if (!(e instanceof FelledLogEntity log) || tool == null) return null;
            if (!log.limbed()) return tool.limbMin() > 0 ? new Limb(log, tool) : null;
            if (!tool.bucks()) { say(p, "cannot_buck"); return null; }
            return new Buck(log, tool);
        }
        BlockPos pos = w.pos();
        BlockState s = level.getBlockState(pos);
        if (WoodItems.isRound(held) && p.isShiftKeyDown() && w.face() == Direction.UP.get3DDataValue()) {
            return SetBlock.at(level, pos);
        }
        if (tool == null) return null;
        if (s.getBlock() instanceof ChoppingBlock && level.getBlockEntity(pos) instanceof ChoppingBlockEntity be && be.hasRound()) {
            if (!tool.splits()) { say(p, "cannot_split"); return null; }
            return new Split(pos, tool);
        }
        if (!(s.getBlock() instanceof YoiLogBlock)) return null;
        if (!tool.bucks()) { say(p, "cannot_buck"); return null; }
        if (WoodBlocks.isStop(level, pos, s)) return new CutBlock(pos, tool, false);
        TrunkSearch.Trunk t = FellingService.trunk(level, pos);
        if (t == null) return new CutBlock(pos, tool, false);
        if (p.isShiftKeyDown()) return new CutTop(t, tool);
        double d = FellingService.diameterCm(t);
        if (!Felling.canFell(tool, d)) {
            say(p, "too_thick", HudArg.of(Math.round(d)), HudArg.of(Math.round(tool.fellMaxCm())));
            return null;
        }
        return new Fell(level, t, tool);
    }

    // ---- felling ----------------------------------------------------------------------------------------------

    /** The felling game on one tree: swings at the tool's minutes each, scored by where the feller stands. */
    public static final class Fell implements WorkJob {
        final TrunkSearch.Trunk trunk;
        final BlockPos base;
        final ToolStats tool;
        final FellingGame game;
        final long swingDs;
        long into;
        boolean creaked, felled, notchFirst, wrongSide;
        Double forcedBearing;

        public Fell(ServerLevel level, TrunkSearch.Trunk t, ToolStats tool) {
            this.trunk = t;
            this.base = FellingService.base(t);
            this.tool = tool;
            this.game = FellingService.game(level, base, tool, FellingService.diameterCm(t));
            this.swingDs = minutesDs(tool.swingMin() * WoodConfig.get("swing_scale"));
        }

        public FellingGame game() { return game; }

        /** Swing from this bearing whatever the player's position (the debug command). */
        public Fell from(double bearing) { forcedBearing = bearing; return this; }

        @Override
        public boolean valid(ServerPlayer p) {
            if (felled) return true;
            BlockState s = p.serverLevel().getBlockState(base);
            return s.getBlock() instanceof YoiLogBlock && tool(p) != null && inReach(p, Vec3.atCenterOf(base).add(0, 1, 0));
        }

        double bearingOf(ServerPlayer p) {
            if (forcedBearing != null) return forcedBearing;
            return Felling.bearing(p.getX() - (base.getX() + 0.5), p.getZ() - (base.getZ() + 0.5));
        }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (felled) return;
            ServerLevel l = p.serverLevel();
            if (into == 0 && game.lastSwingNext() && !creaked && game.arrowLocked()) {
                creaked = true;
                l.playSound(null, base.above(2), WoodRegistry.TREE_CREAK.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            into += ds;
            if (into < swingDs) return;
            into = 0;
            Wear.use(p, p.getMainHandItem(), 1);
            boolean wasNotch = game.notchDone();
            FellingGame.Strike s = game.strike(bearingOf(p));
            l.playSound(null, base.above(), WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 1.0f, 0.9f + 0.2f * l.random.nextFloat());
            chips(l, base.above(), l.getBlockState(base));
            switch (s) {
                case NOTCH_FIRST -> { notchFirst = true; say(p, "notch_first"); }
                case WRONG_SIDE -> { wrongSide = true; say(p, "back_cut_far_side"); }
                default -> { }
            }
            if (!wasNotch && game.notchDone() && !game.done()) {
                say(p, "notch_done", HudArg.text(Felling.compass(game.fallBearing())));
            }
            if (game.done()) {
                TrunkSearch.Trunk now = FellingService.trunk(l, base);
                var felledTrunk = now != null ? now : trunk;
                FellingService.fell(l, felledTrunk, game.fallBearing(), p);
                felled = true;
                Mishaps.roll(l, p, base, game.fallBearing(), felledTrunk.height(), notchFirst, wrongSide);
            }
        }

        @Override public boolean finished() { return felled; }

        @Override public float share() { return (float) Math.min(1, game.share() + (double) into / swingDs / game.swings()); }

        @Override public float sharePerDs() { return 1f / (swingDs * game.swings()); }

        @Override public Phase phase() { return game.notchDone() ? Phase.BACK_CUT : Phase.NOTCH; }

        @Override public int done() { return (int) Math.floor(game.notchDone() ? game.back() : game.notch()); }

        @Override public int of() { return game.notchDone() ? game.backNeeded() : game.notchNeeded(); }
    }

    /** Sneak + use on a standing trunk: its top block only, at twice the buck time (A1). */
    public static final class CutTop extends Timed {
        final TrunkSearch.Trunk trunk;

        public CutTop(TrunkSearch.Trunk t, ToolStats tool) {
            super(FellingService.base(t), Bucking.sneakTopMinutes(tool.buckMin(), WoodConfig.get("sneak_cut_time_mult")) * WoodConfig.get("work_scale"),
                    Phase.CUT_TOP);
            this.trunk = t;
        }

        @Override
        boolean still(ServerPlayer p) {
            int[] top = trunk.top();
            BlockPos tp = new BlockPos(top[0], top[1], top[2]);
            return p.serverLevel().getBlockState(tp).getBlock() instanceof YoiLogBlock && tool(p) != null && inReach(p, Vec3.atCenterOf(target));
        }

        @Override
        void complete(ServerPlayer p) {
            TrunkSearch.Trunk now = FellingService.trunk(p.serverLevel(), target);
            FellingService.cutTop(p.serverLevel(), now != null ? now : trunk, p);
        }
    }

    /** A placed log (or a lone, unrooted one): that block only, in the tool's buck time, as 2 rounds (A1). */
    public static final class CutBlock extends Timed {
        public CutBlock(BlockPos pos, ToolStats tool, boolean unused) {
            super(pos, Bucking.placedBlockMinutes(tool.buckMin()) * WoodConfig.get("work_scale"), Phase.CUT_BLOCK);
        }

        @Override
        boolean still(ServerPlayer p) {
            return p.serverLevel().getBlockState(target).getBlock() instanceof YoiLogBlock && tool(p) != null && inReach(p, Vec3.atCenterOf(target));
        }

        @Override
        void complete(ServerPlayer p) { FellingService.cutBlock(p.serverLevel(), target, p); }
    }

    /** A job of one span of game minutes on a block. */
    abstract static class Timed implements WorkJob {
        final BlockPos target;
        final long needDs;
        final Phase phase;
        long doneDs;
        boolean finished;

        Timed(BlockPos target, double minutes, Phase phase) {
            this.target = target.immutable();
            this.needDs = minutesDs(minutes);
            this.phase = phase;
        }

        abstract boolean still(ServerPlayer p);

        abstract void complete(ServerPlayer p);

        @Override public boolean valid(ServerPlayer p) { return finished || still(p); }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (finished) return;
            long before = doneDs;
            doneDs += ds;
            Wear.use(p, p.getMainHandItem(), (int) (doneDs / DS_PER_MIN - before / DS_PER_MIN));
            if (before / (DS_PER_MIN * 2) != doneDs / (DS_PER_MIN * 2)) {
                p.serverLevel().playSound(null, target, WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
                chips(p.serverLevel(), target, p.serverLevel().getBlockState(target));
            }
            if (doneDs >= needDs) {
                finished = true;
                complete(p);
            }
        }

        @Override public boolean finished() { return finished; }

        @Override public float share() { return Math.min(1f, (float) doneDs / needDs); }

        @Override public float sharePerDs() { return 1f / needDs; }

        @Override public Phase phase() { return phase; }

        @Override public int done() { return finished ? 1 : 0; }

        @Override public int of() { return 1; }
    }

    // ---- limbing and bucking ----------------------------------------------------------------------------------

    static boolean nearLog(ServerPlayer p, FelledLogEntity log) {
        if (log.isRemoved()) return false;
        Vec3 eye = p.getEyePosition();
        return log.getBoundingBox().inflate(reach() + 1.0).contains(eye);
    }

    /** Limbing a felled log: the tool's limb minutes; spruce gives boughs and sticks, birch sticks (as kindling). */
    public static final class Limb implements WorkJob {
        final FelledLogEntity log;
        final long needDs;
        long doneDs;
        boolean finished;

        public Limb(FelledLogEntity log, ToolStats tool) {
            this.log = log;
            this.needDs = minutesDs(tool.limbMin() * WoodConfig.get("work_scale"));
        }

        @Override public boolean valid(ServerPlayer p) { return finished || (nearLog(p, log) && tool(p) != null && !log.limbed()); }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (finished) return;
            long before = doneDs;
            doneDs += ds;
            Wear.use(p, p.getMainHandItem(), (int) (doneDs / DS_PER_MIN - before / DS_PER_MIN));
            if (doneDs < needDs) return;
            finished = true;
            log.setLimbed();
            TreeKind k = log.kind();
            WoodItems.give(p, new ItemStack(FireRegistry.KINDLING.get(), Bucking.sticks(k)));
            Item bough = boughItem();
            if (bough != null && Bucking.boughs(k) > 0) {
                int n = Bucking.boughs(k);
                while (n > 0) {
                    int c = Math.min(n, bough.getDefaultMaxStackSize());
                    WoodItems.give(p, new ItemStack(bough, c));
                    n -= c;
                }
            }
            p.serverLevel().playSound(null, log.getX(), log.getY(), log.getZ(), WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
        }

        @Override public boolean finished() { return finished; }

        @Override public float share() { return Math.min(1f, (float) doneDs / needDs); }

        @Override public float sharePerDs() { return 1f / needDs; }

        @Override public Phase phase() { return Phase.LIMB; }

        @Override public int done() { return finished ? 1 : 0; }

        @Override public int of() { return 1; }
    }

    /** Boughs come from the bough-bed task's item when it is registered ({@code yearofice:spruce_bough}); none before. */
    static @Nullable Item boughItem() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("yearofice", "spruce_bough")).orElse(null);
    }

    /** Bucking a limbed log into rounds from its top end: a quarter of the tool's armload minutes a round. */
    public static final class Buck implements WorkJob {
        final FelledLogEntity log;
        final long roundDs;
        final int total;
        long into;
        long wearDs;
        int cut;

        public Buck(FelledLogEntity log, ToolStats tool) {
            this.log = log;
            this.roundDs = minutesDs(Bucking.roundMinutes(tool.buckMin()) * WoodConfig.get("work_scale"));
            this.total = log.roundsLeft();
        }

        @Override public boolean valid(ServerPlayer p) { return finished() || (nearLog(p, log) && tool(p) != null && log.limbed()); }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (finished()) return;
            into += ds;
            wearDs += ds;
            Wear.use(p, p.getMainHandItem(), (int) (wearDs / DS_PER_MIN));
            wearDs %= DS_PER_MIN;
            while (into >= roundDs && !finished()) {
                into -= roundDs;
                long day = WoodItems.today();
                WoodItems.give(p, WoodItems.round(log.kind().wood, 1, log.moistureNow(day), day));
                log.cutRound();
                cut++;
                p.serverLevel().playSound(null, log.getX(), log.getY(), log.getZ(), WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
            }
        }

        public int cut() { return cut; }

        @Override public boolean finished() { return log.isRemoved() || log.roundsLeft() <= 0; }

        @Override public float share() { return total == 0 ? 1 : Math.min(1f, (cut + (float) into / roundDs) / total); }

        @Override public float sharePerDs() { return total == 0 ? 0 : 1f / (roundDs * total); }

        @Override public Phase phase() { return Phase.BUCK; }

        @Override public int done() { return cut; }

        @Override public int of() { return total; }
    }

    // ---- splitting --------------------------------------------------------------------------------------------

    /** Splitting the round on a chopping block: 1 game minute a strike; on the crack it splits at once. */
    public static final class Split implements WorkJob {
        final BlockPos pos;
        final ToolStats tool;
        final long strikeDs;
        long into;
        boolean finished;
        /** A fixed aim (u, v on the top face) for tests and the debug path; null reads the player's crosshair. */
        public double[] aim;

        public Split(BlockPos pos, ToolStats tool) {
            this.pos = pos.immutable();
            this.tool = tool;
            this.strikeDs = minutesDs(Splitting.STRIKE_MIN * WoodConfig.get("work_scale"));
        }

        public Split aim(double u, double v) { aim = new double[] {u, v}; return this; }

        ChoppingBlockEntity be(ServerLevel l) { return l.getBlockEntity(pos) instanceof ChoppingBlockEntity b ? b : null; }

        @Override
        public boolean valid(ServerPlayer p) {
            if (finished) return true;
            ChoppingBlockEntity b = be(p.serverLevel());
            return b != null && b.hasRound() && tool(p) != null && inReach(p, Vec3.atCenterOf(pos));
        }

        double[] aimOf(ServerPlayer p) {
            if (aim != null) return aim;
            HitResult h = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(reach() + 1)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
            if (h instanceof BlockHitResult b && b.getBlockPos().equals(pos)) {
                return new double[] {b.getLocation().x - pos.getX(), b.getLocation().z - pos.getZ()};
            }
            return new double[] {0, 0};
        }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (finished) return;
            into += ds;
            if (into < strikeDs) return;
            into = 0;
            Wear.use(p, p.getMainHandItem(), 1);
            ServerLevel l = p.serverLevel();
            ChoppingBlockEntity b = be(l);
            double[] a = aimOf(p);
            boolean crack = Splitting.onCrack(a[0], a[1], b.crack());
            b.addStrike();
            l.playSound(null, pos, WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
            if ((crack && b.strikes() >= tool.splitCrackStrikes()) || b.strikes() >= tool.splitStrikes()) {
                ItemStack round = b.takeRound();
                long day = WoodItems.today();
                WoodPieceItem piece = WoodItems.piece(round);
                WoodItems.give(p, WoodItems.split(piece.wood, 1, WoodItems.moistureNow(round, day), day));
                l.playSound(null, pos, WoodRegistry.TREE_SPLIT.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                finished = true;
            }
        }

        @Override public boolean finished() { return finished; }

        @Override public float share() { return finished ? 1 : Math.min(1f, (float) into / strikeDs); }

        @Override public float sharePerDs() { return 1f / strikeDs; }

        @Override public Phase phase() { return Phase.SPLIT; }

        @Override public int done() { return 0; }

        @Override public int of() { return tool.splitStrikes(); }
    }

    /** Sneak + use with a round on the top of a block: sets it on end as a chopping block (2 game minutes). */
    public static final class SetBlock extends Timed {
        SetBlock(BlockPos on, double minutes) { super(on, minutes, Phase.SET_BLOCK); }

        static @Nullable SetBlock at(ServerLevel l, BlockPos on) {
            if (!l.getBlockState(on.above()).canBeReplaced() || !l.getBlockState(on).isFaceSturdy(l, on, Direction.UP)) return null;
            return new SetBlock(on, WoodConfig.get("chopping_block_set_min"));
        }

        @Override
        boolean still(ServerPlayer p) {
            return WoodItems.isRound(p.getMainHandItem()) && p.serverLevel().getBlockState(target.above()).canBeReplaced()
                    && inReach(p, Vec3.atCenterOf(target));
        }

        @Override
        void complete(ServerPlayer p) {
            p.serverLevel().setBlock(target.above(), WoodRegistry.CHOPPING_BLOCK.get().defaultBlockState(), 3);
            if (!p.getAbilities().instabuild) p.getMainHandItem().shrink(1);
        }
    }

    /** A round set on a stump makes the stump a chopping block (Splitting: "Any stump is a chopping block"). */
    public static boolean roundOnStump(ServerLevel l, BlockPos pos, ItemStack held, ServerPlayer p) {
        if (!(l.getBlockState(pos).getBlock() instanceof StumpBlock) || !WoodItems.isRound(held)) return false;
        l.setBlock(pos, WoodRegistry.CHOPPING_BLOCK.get().defaultBlockState().setValue(ChoppingBlock.STUMP, true), 3);
        if (l.getBlockEntity(pos) instanceof ChoppingBlockEntity be) {
            be.setRound(held, l.random.nextInt(4));
            if (!p.getAbilities().instabuild) held.shrink(1);
        }
        return true;
    }

    static String lower(Enum<?> e) { return e.name().toLowerCase(Locale.ROOT); }
}
