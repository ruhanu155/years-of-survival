package com.yearofice.item.inventory;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.build.craft.CraftConfig;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.BoxLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Lift a full box (video 2, A7; RECONCILE 170; Minecraft development › Hand-carried loads and carcasses). A chest,
 * barrel or crate of 30 kg or less, contents included, in {@code #yearofice:liftable_containers}, is lifted whole by
 * holding Shift + use for 1 s with an empty main hand, carried in the arms (the {@code yearofice:carry} attachment,
 * counted in the load), and set down with Shift + use held for 1 s on a block, its contents and facing kept. A box
 * that has stood 24 game hours on frozen ground in air under 0 C is frozen down: hold Shift + use 2 s with a hatchet or
 * knife to chip it free first. No Carry On dependency. While the arms are full tool, bow and placement inputs are off.
 *
 * <p>Not yet (see the PR): Q to set down at once, the camera drop and the carry slot's art (look), the Strength
 * scaling of the cap (M4), warm rooms for the frozen rule (M8), and the supply crate block (start site lane).
 */
public final class BoxCarry {
    private BoxCarry() {}

    public static final TagKey<Block> LIFTABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "liftable_containers"));
    /** Tools that chip a frozen box free: the hatchet and the knives. */
    public static final TagKey<net.minecraft.world.item.Item> CHIP_TOOLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "chip_tools"));

    public enum Problem { NONE, TOO_HEAVY, FROZEN, NOT_SINGLE, NOT_A_BOX, HANDS_FULL, CARRYING }

    /** What lifting {@code pos} would be: the problem, the total weight and the weight over the cap. */
    public record Check(Problem problem, double totalKg, double overKg, int stacks) {}

    // ---- reading ----------------------------------------------------------------------------------------------

    public static CarryData carried(net.minecraft.world.entity.player.Player p) {
        // hasData first: reading a default would attach it and send a sync for every player who never lifted a box.
        return p.hasData(CarryRegistry.CARRY.get()) ? p.getData(CarryRegistry.CARRY.get()) : CarryData.NONE;
    }

    /** Sets the load and syncs it to the owner. A detached test bot has no client to sync to; the value is set before the sync throws. */
    static void setCarry(ServerPlayer p, CarryData d) {
        try {
            p.setData(CarryRegistry.CARRY.get(), d);
        } catch (UnsupportedOperationException detachedBot) {
            // only a GameTest bot on a serverbound connection gets here
        }
    }

    public static boolean carrying(net.minecraft.world.entity.player.Player p) { return !carried(p).isEmpty(); }

    /** Kilograms in the arms, counted in the load. */
    public static double kg(net.minecraft.world.entity.player.Player p) { return carried(p).kg(); }

    static @Nullable Container containerAt(ServerLevel l, BlockPos pos) {
        return l.getBlockEntity(pos) instanceof Container c ? c : null;
    }

    /** The empty weight of the box block, from the item weight map of its item (chest 12 kg, barrel 8 kg). */
    public static double emptyKg(BlockState s) { return LoadCalculator.unitG(new ItemStack(s.getBlock().asItem())) / 1000.0; }

    public static double contentsKg(Container c) {
        double kg = 0;
        for (int i = 0; i < c.getContainerSize(); i++) kg += LoadCalculator.stackKg(c.getItem(i), true);
        return kg;
    }

    public static int stackCount(Container c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) n++;
        return n;
    }

    /** Whether lifting {@code pos} works for a player with an empty hand: the weight, a single box, and the frost. */
    public static Check check(ServerLevel l, BlockPos pos) {
        BlockState s = l.getBlockState(pos);
        Container c = containerAt(l, pos);
        if (!s.is(LIFTABLE) || c == null) return new Check(Problem.NOT_A_BOX, 0, 0, 0);
        if (s.getBlock() instanceof ChestBlock && s.hasProperty(ChestBlock.TYPE) && s.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return new Check(Problem.NOT_SINGLE, 0, 0, 0);
        }
        BoxLift.Verdict v = BoxLift.check(emptyKg(s), contentsKg(c), CraftConfig.get("box_cap_kg"));
        int stacks = stackCount(c);
        if (!v.ok()) return new Check(Problem.TOO_HEAVY, v.totalKg(), v.overKg(), stacks);
        if (frozenDown(l, pos)) return new Check(Problem.FROZEN, v.totalKg(), 0, stacks);
        return new Check(Problem.NONE, v.totalKg(), 0, stacks);
    }

    /** The Clock minute the box was last touched; a box with no stamp has just been found, so it stamps now. */
    static long stamp(ServerLevel l, BlockPos pos) {
        BlockEntity be = l.getBlockEntity(pos);
        if (be == null) return now();
        if (!be.hasData(CarryRegistry.BOX_STAMP.get())) touch(l, pos);
        return be.getData(CarryRegistry.BOX_STAMP.get());
    }

    static long now() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    /** Marks the box as touched now (opened, lifted, set down, chipped): it has not stood still. */
    public static void touch(ServerLevel l, BlockPos pos) {
        BlockEntity be = l.getBlockEntity(pos);
        if (be != null) be.setData(CarryRegistry.BOX_STAMP.get(), now());
    }

    /** Where the frost rule reads the air and the ground; tests swap it so they never move the shared weather. */
    public interface Frost {
        double airC(ServerLevel l, BlockPos pos);

        boolean groundFrozen(ServerLevel l, BlockPos pos);
    }

    /** The Env service's air at the box and its ground temperature at 10 cm. */
    public static final Frost ENV_FROST = new Frost() {
        @Override public double airC(ServerLevel l, BlockPos pos) { return EnvService.installed() ? EnvService.get().airC(l, pos) : 0; }

        @Override public boolean groundFrozen(ServerLevel l, BlockPos pos) { return EnvService.installed() && EnvService.get().groundC(l, pos.below(), 0.1) < 0; }
    };
    public static volatile Frost frost = ENV_FROST;

    /** Stood {@code box_frozen_h} game hours on frozen ground in air under 0 C. */
    public static boolean frozenDown(ServerLevel l, BlockPos pos) {
        double hours = (now() - stamp(l, pos)) / 60.0;
        // Warm rooms arrive with the building pieces (M8): until then no room thaws a box.
        return BoxLift.frozenDown(hours, CraftConfig.get("box_frozen_h"), frost.airC(l, pos), frost.groundFrozen(l, pos), false);
    }

    // ---- lifting and setting down -----------------------------------------------------------------------------

    /** Lifts the box at {@code pos} into the player's arms; returns false (saying why) if it cannot. */
    public static boolean lift(ServerPlayer p, BlockPos pos) {
        ServerLevel l = p.serverLevel();
        Problem pre = precheck(p);
        if (pre != Problem.NONE) {
            say(p, pre);
            return false;
        }
        Check c = check(l, pos);
        if (c.problem() != Problem.NONE) {
            say(p, c.problem(), c.totalKg(), c.overKg());
            return false;
        }
        BlockState s = l.getBlockState(pos);
        BlockEntity be = l.getBlockEntity(pos);
        Container box = (Container) be;
        CompoundTag tag = be.saveWithFullMetadata(l.registryAccess());
        box.clearContent();
        l.removeBlockEntity(pos);
        l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        setCarry(p, new CarryData(s, tag, c.totalKg(), c.stacks()));
        l.playSound(null, pos, CarryRegistry.BOX_LIFT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    /** What stops a player lifting at all: arms already full, or something in the main hand. */
    static Problem precheck(ServerPlayer p) {
        if (carrying(p)) return Problem.CARRYING;
        if (!p.getMainHandItem().isEmpty()) return Problem.HANDS_FULL;
        return Problem.NONE;
    }

    /** Whether the carried box fits on {@code at}: a clear block on firm, dry ground. */
    public static boolean fits(ServerLevel l, BlockPos at) {
        BlockState here = l.getBlockState(at);
        if (!here.canBeReplaced() || !l.getFluidState(at).isEmpty()) return false;
        BlockPos below = at.below();
        return l.getBlockState(below).isFaceSturdy(l, below, Direction.UP) && l.getFluidState(below).isEmpty();
    }

    /** Sets the carried box down at {@code at} with its contents and facing; false if there is no room. */
    public static boolean setDown(ServerPlayer p, BlockPos at, boolean sayWhy) {
        CarryData d = carried(p);
        if (d.isEmpty()) return false;
        ServerLevel l = p.serverLevel();
        if (!fits(l, at)) {
            if (sayWhy) say(p, "box_no_room");
            return false;
        }
        l.setBlock(at, d.state(), 3);
        BlockEntity be = l.getBlockEntity(at);
        if (be != null) {
            be.loadWithComponents(d.blockEntity(), l.registryAccess());
            be.setChanged();
        }
        touch(l, at);
        setCarry(p, CarryData.NONE);
        l.playSound(null, at, CarryRegistry.BOX_SET.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    /** Collapse and death set the box down where the player falls, never spilled. */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !carrying(p)) return;
        ServerLevel l = p.serverLevel();
        BlockPos base = p.blockPosition();
        for (int up = 0; up <= 3; up++) {
            for (Direction d : new Direction[] {null, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos at = (d == null ? base : base.relative(d)).above(up);
                if (setDown(p, at, false)) return;
            }
        }
    }

    // ---- jobs -------------------------------------------------------------------------------------------------

    /** The job for a held Shift + use on a block, or null (posting why, where the player should know). */
    public static @Nullable WorkJob resolve(ServerPlayer p, WorkStartPayload w) {
        if (w.entity() >= 0 || !p.isShiftKeyDown()) return null;
        ServerLevel l = p.serverLevel();
        BlockPos pos = w.pos();
        if (carrying(p)) {
            Direction face = Direction.from3DDataValue(w.face());
            BlockPos at = l.getBlockState(pos).canBeReplaced() ? pos : pos.relative(face);
            if (!fits(l, at)) {
                say(p, "box_no_room");
                return null;
            }
            return new Job(Kind.SET, pos, at, CraftConfig.get("box_lift_s"));
        }
        if (!l.getBlockState(pos).is(LIFTABLE)) return null;
        ItemStack hand = p.getMainHandItem();
        if (hand.is(CHIP_TOOLS)) {
            if (!frozenDown(l, pos)) return null;
            return new Job(Kind.CHIP, pos, pos, CraftConfig.get("box_chip_s"));
        }
        Problem pre = precheck(p);
        if (pre != Problem.NONE) {
            say(p, pre);
            return null;
        }
        Check c = check(l, pos);
        if (c.problem() == Problem.FROZEN) {
            say(p, Problem.FROZEN);
            return null;
        }
        if (c.problem() != Problem.NONE) {
            say(p, c.problem(), c.totalKg(), c.overKg());
            return null;
        }
        return new Job(Kind.LIFT, pos, pos, CraftConfig.get("box_lift_s"));
    }

    enum Kind { LIFT, SET, CHIP }

    /** Lift, set-down and chip are all a timed hold in real seconds (the box does not wait on the Work rate). */
    public static final class Job implements WorkJob {
        final Kind kind;
        final BlockPos target, at;
        final int needTicks;
        int ticks;
        boolean finished;

        Job(Kind kind, BlockPos target, BlockPos at, double seconds) {
            this.kind = kind;
            this.target = target.immutable();
            this.at = at.immutable();
            this.needTicks = Math.max(1, (int) Math.round(seconds * 20));
        }

        @Override
        public boolean valid(ServerPlayer p) {
            if (finished) return true;
            if (p.getEyePosition().distanceTo(Vec3.atCenterOf(target)) > CraftConfig.get("box_reach_m") + 1.0) return false;
            if (!p.isShiftKeyDown()) return false;
            ServerLevel l = p.serverLevel();
            return switch (kind) {
                case SET -> carrying(p) && fits(l, at);
                case LIFT -> !carrying(p) && l.getBlockState(target).is(LIFTABLE) && p.getMainHandItem().isEmpty();
                case CHIP -> l.getBlockState(target).is(LIFTABLE) && p.getMainHandItem().is(CHIP_TOOLS);
            };
        }

        @Override
        public void work(ServerPlayer p, long ds) {
            if (finished || ++ticks < needTicks) return;
            finished = true;
            ServerLevel l = p.serverLevel();
            switch (kind) {
                case LIFT -> lift(p, target);
                case SET -> setDown(p, at, true);
                case CHIP -> {
                    touch(l, target);
                    l.playSound(null, target, CarryRegistry.BOX_LIFT.get(), SoundSource.BLOCKS, 0.8f, 1.4f);
                    com.yearofice.build.craft.Wear.use(p, p.getMainHandItem(), 1);
                }
            }
        }

        @Override public boolean finished() { return finished; }
        @Override public float share() { return Math.min(1f, (float) ticks / needTicks); }
        @Override public float sharePerDs() { return 1f / needTicks / Math.max(1, com.yearofice.build.work.WorkService.dsPerTick()); }

        @Override
        public WorkProgressPayload.Phase phase() {
            return kind == Kind.CHIP ? WorkProgressPayload.Phase.CHIP : WorkProgressPayload.Phase.LIFT;
        }

        @Override public int done() { return 0; }
        @Override public int of() { return 1; }
    }

    // ---- lines ------------------------------------------------------------------------------------------------

    static void say(ServerPlayer p, Problem pr) { say(p, pr, 0, 0); }

    static void say(ServerPlayer p, Problem pr, double total, double over) {
        switch (pr) {
            case TOO_HEAVY -> post(p, "box_too_heavy", HudArg.fixed(total, "kg"), HudArg.fixed(over, "kg"));
            case FROZEN -> post(p, "box_frozen");
            case NOT_SINGLE -> post(p, "box_not_single");
            case HANDS_FULL -> post(p, "box_hands_full");
            case CARRYING -> post(p, "box_carrying");
            default -> { }
        }
    }

    static void say(ServerPlayer p, String id) { post(p, id); }

    static void post(ServerPlayer p, String id, HudArg... args) {
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.PROMPT, id, "pack", args));
    }

    // ---- inputs while carrying ---------------------------------------------------------------------------------

    static void wire() {
        NeoForge.EVENT_BUS.addListener(BoxCarry::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) deny(e);
            else if (e.getEntity() instanceof ServerPlayer p && !e.getLevel().isClientSide() && e.getLevel() instanceof ServerLevel l
                    && l.getBlockState(e.getPos()).is(LIFTABLE)) touch(l, e.getPos()); // opening a box moves it: it has not frozen down
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) deny(e);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) deny(e);
        });
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && carrying(p)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> {
            if (carrying(e.getPlayer())) e.setCanceled(true);
        });
    }

    private static void deny(PlayerInteractEvent.RightClickBlock e) {
        e.setCancellationResult(InteractionResult.FAIL);
        e.setCanceled(true);
    }

    private static void deny(PlayerInteractEvent.RightClickItem e) {
        e.setCancellationResult(InteractionResult.FAIL);
        e.setCanceled(true);
    }

    private static void deny(PlayerInteractEvent.EntityInteract e) {
        e.setCancellationResult(InteractionResult.FAIL);
        e.setCanceled(true);
    }

    /** The prompt row while carrying: what is in the arms and how to put it down. */
    public static void tickPrompt(ServerPlayer p) {
        if (!HudBus.installed() || p.tickCount % 20 != 3) return;
        CarryData d = carried(p);
        if (d.isEmpty()) {
            HudBus.get().clearPrompt(p, "box_carry");
            return;
        }
        HudBus.get().offerPrompt(p, "box_carry", d.slotLine() + ". Shift + hold use on a block to set it down.", 1);
    }
}
