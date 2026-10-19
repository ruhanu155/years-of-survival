package com.yearofice.build.camp;

import com.yearofice.build.BuildConfig;
import com.yearofice.build.BuildRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Building a bough bed: one hold-to-work job on the ground (Blueprint › Making camp: 40 spruce boughs, 1 game hour).
 * {@link SpruceBoughsItem#useOn} checks the spot and starts it; each held tick here keeps the Work rate and checks the
 * worker is still by the spot and the spot still clear; the finished hold lays the bed and takes the boughs. Letting
 * go cancels it with nothing spent.
 *
 * <p>The spot is the block above the one clicked (or the clicked block itself if it is replaceable, like grass), the
 * foot; the head is one block on in the direction the player faces. Both need a sturdy top below them.
 */
public final class BedBuilding {
    private BedBuilding() {}

    /** One job: the foot, the facing (foot to head) and the ticks held. */
    public static final class Job {
        public final BlockPos foot;
        public final Direction facing;
        int ticks;

        Job(BlockPos foot, Direction facing) {
            this.foot = foot;
            this.facing = facing;
        }

        public BlockPos head() { return foot.relative(facing); }

        public int ticks() { return ticks; }
    }

    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();

    /** Why a spot will not take a bed, as a lang key suffix, or null when it will. */
    public static @Nullable String problem(Level level, BlockPos foot, Direction facing, @Nullable Player p) {
        BlockPos head = foot.relative(facing);
        for (BlockPos q : new BlockPos[] {foot, head}) {
            if (!level.getBlockState(q).canBeReplaced()) return "bed_blocked";
            if (!level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP)) return "bed_no_ground";
        }
        if (p != null && !p.getAbilities().instabuild && SpruceBoughsItem.held(p) < BuildConfig.bedBoughs()) return "bed_few_boughs";
        return null;
    }

    /** The foot a click on {@code clicked}'s {@code face} would build on. */
    public static BlockPos footFor(Level level, BlockPos clicked, Direction face) {
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
    }

    public static void begin(ServerPlayer p, BlockPos foot, Direction facing) { JOBS.put(p.getUUID(), new Job(foot.immutable(), facing)); }

    public static @Nullable Job job(ServerPlayer p) { return JOBS.get(p.getUUID()); }

    /** One held tick; false (and the job dropped) when the worker walked off or the spot filled. */
    public static boolean tick(ServerPlayer p) {
        Job j = JOBS.get(p.getUUID());
        if (j == null) return false;
        double r = BuildConfig.reachM();
        if (p.distanceToSqr(j.foot.getCenter()) > r * r || problem(p.level(), j.foot, j.facing, null) != null) {
            JOBS.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("building.yearofice.bed_stopped"), true);
            return false;
        }
        j.ticks++;
        Work.tick(p, j.ticks, "building a bough bed");
        return true;
    }

    /** The held job is done: lay the bed and take the boughs from {@code stack} (the held stack). */
    public static boolean finish(ServerPlayer p, ItemStack stack) {
        Job j = JOBS.remove(p.getUUID());
        if (j == null || j.ticks < Work.ticks(BuildConfig.bedMin()) - 1) return false;
        Level level = p.level();
        String why = problem(level, j.foot, j.facing, p);
        if (why != null) {
            p.displayClientMessage(Component.translatable("building.yearofice." + why, BuildConfig.bedBoughs(), SpruceBoughsItem.held(p)), true);
            return false;
        }
        BoughBedBlock bed = BuildRegistry.BOUGH_BED.get();
        BlockState foot = bed.foot(j.facing), head = bed.head(j.facing);
        level.setBlock(j.foot, foot, 3);
        level.setBlock(j.head(), head, 3);
        if (!p.getAbilities().instabuild) SpruceBoughsItem.take(p, stack, BuildConfig.bedBoughs());
        level.playSound(null, j.foot, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("building.yearofice.bed_built"), true);
        return true;
    }

    public static void cancel(ServerPlayer p) { JOBS.remove(p.getUUID()); }
}
