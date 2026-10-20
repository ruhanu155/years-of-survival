package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.StrikeJudge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * {@code yearofice:bobber}: the cast bobber, which replaces vanilla's fishing hook (Minecraft engine › Fishing ›
 * Rod and bobber). The server flies it along the cast's arc, floats it at the water's surface, and holds the
 * angler's session: the bait in the water, the bite cue, the hooked fish. The client draws it and the line, and
 * dips or shakes it from the synced cue. It is never saved: a chunk unload or a restart reels it in.
 */
public final class BobberEntity extends Entity {
    public enum State { FLYING, FLOATING, LANDED, HOOKED }

    static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> CUE = SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);

    // Server-side session.
    @Nullable ServerPlayer owner;
    Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    int flightTicks, flightT;
    double arcM;
    long lastMinute = Long.MIN_VALUE;
    int minutesInWater;
    int cueTicks;
    long dipTick;
    double castM;
    /** Tests hold the per-minute rolls so only the bites they force happen. */
    boolean holdRolls;
    @Nullable FightSession fight;
    /** The species whose bite is on (set by the bite roll; null off a bite). */
    public @Nullable String biter;

    public BobberEntity(EntityType<? extends BobberEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(STATE, (byte) State.FLYING.ordinal());
        b.define(CUE, (byte) StrikeJudge.Cue.NONE.ordinal());
        b.define(OWNER, -1);
    }

    public State state() { return State.values()[Math.floorMod(entityData.get(STATE), State.values().length)]; }

    void setState(State s) { entityData.set(STATE, (byte) s.ordinal()); }

    public StrikeJudge.Cue cue() { return StrikeJudge.Cue.values()[Math.floorMod(entityData.get(CUE), StrikeJudge.Cue.values().length)]; }

    void setCue(StrikeJudge.Cue c, int ticks) {
        entityData.set(CUE, (byte) c.ordinal());
        cueTicks = ticks;
    }

    public boolean hooked() { return state() == State.HOOKED; }

    /** The hooked fish's session (server), or null. */
    public @Nullable FightSession fight() { return fight; }

    /** The angler, for the line renderer (client) or the session (server). */
    public @Nullable Player ownerPlayer() {
        if (owner != null) return owner;
        Entity e = level().getEntity(entityData.get(OWNER));
        return e instanceof Player p ? p : null;
    }

    void bind(ServerPlayer p) {
        owner = p;
        entityData.set(OWNER, p.getId());
    }

    /** Client: does this player have a bobber out (so right mouse reels in rather than charging a cast). */
    public static boolean clientHasBobber(Player p) {
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(48), e -> e instanceof BobberEntity)) {
            if (((BobberEntity) e).ownerPlayer() == p) return true;
        }
        return false;
    }

    /** Starts the flight from {@code start} to {@code target} over {@code ticks}, rising {@code arc} metres mid-way. */
    void launch(Vec3 start, Vec3 target, int ticks, double arc) {
        from = start;
        to = target;
        flightTicks = Math.max(1, ticks);
        flightT = 0;
        arcM = arc;
        setPos(start);
        setState(State.FLYING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!FishingService.ownerStillFishing(this)) {
            FishingService.drop(this, "The line went slack.");
            return;
        }
        switch (state()) {
            case FLYING -> fly();
            case FLOATING -> {
                floatAtSurface();
                FishingService.floatTick(this);
            }
            case HOOKED -> FishingService.fightTick(this);
            case LANDED -> { }
        }
    }

    private void fly() {
        Vec3 old = position();
        flightT++;
        double f = Math.min(1.0, flightT / (double) flightTicks);
        Vec3 next = from.lerp(to, f).add(0, arcM * 4 * f * (1 - f), 0);
        BlockHitResult hit = level().clip(new ClipContext(old, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            setPos(hit.getLocation());
            BlockPos at = hit.getBlockPos();
            if (level().getFluidState(at).is(FluidTags.WATER)) {
                FishingService.landedInWater(this, at);
            } else {
                setState(State.LANDED);
                FishingService.landedOnGround(this);
            }
            return;
        }
        setPos(next);
        if (flightT >= flightTicks) {
            BlockPos at = BlockPos.containing(next.x, next.y - 0.05, next.z);
            if (level().getFluidState(at).is(FluidTags.WATER)) {
                FishingService.landedInWater(this, at);
            } else {
                setState(State.LANDED);
                FishingService.landedOnGround(this);
            }
        }
    }

    /** The water's surface above the bobber's column, or NaN when it is not over water. */
    double surfaceY() {
        BlockPos p = BlockPos.containing(getX(), getY() - 0.05, getZ());
        FluidState f = level().getFluidState(p);
        if (!f.is(FluidTags.WATER)) {
            p = p.below();
            f = level().getFluidState(p);
            if (!f.is(FluidTags.WATER)) return Double.NaN;
        }
        for (int i = 0; i < 8 && level().getFluidState(p.above()).is(FluidTags.WATER); i++) p = p.above();
        return p.getY() + level().getFluidState(p).getHeight(level(), p);
    }

    /** The water block the bobber floats on. */
    BlockPos waterBlock() { return BlockPos.containing(getX(), surfaceY() - 0.05, getZ()); }

    private void floatAtSurface() {
        double s = surfaceY();
        if (Double.isNaN(s)) {
            setState(State.LANDED);
            return;
        }
        Vec3 flow = level().getFluidState(waterBlock()).getFlow(level(), waterBlock());
        setPos(getX() + flow.x * 0.01, s - 0.1, getZ() + flow.z * 0.01);
    }

    HookBait bait() {
        return owner == null ? HookBait.NONE : RodItem.bait(FishingService.heldRod(owner));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 64 * 64; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) FishingService.removed(this);
        super.remove(reason);
    }
}
