package com.yearofice.build.raft;

import com.yearofice.sim.build.RaftModel;
import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * {@code yearofice:raft} (Open world in depth › Traversal): six lashed logs that carry the player and a load, drift
 * with the water's flow at the current's speed and break up in rapids, spilling the logs. The flow is the vanilla
 * water flow vector times {@code build.outpost} current (1.2 m/s on the main river).
 * ponytail: the current is one dial, not the river model's per-reach speed — upgrade: read the hydrology service.
 */
public class RaftEntity extends Entity {
    public static final double DEFAULT_CURRENT_MS = 1.2;
    private double cargoKg;

    public RaftEntity(EntityType<? extends RaftEntity> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(CompoundTag t) { cargoKg = t.getDouble("cargo_kg"); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { t.putDouble("cargo_kg", cargoKg); }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override protected boolean canAddPassenger(Entity e) { return getPassengers().size() < 2; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity e, EntityDimensions d, float scale) { return new Vec3(0, 0.25, getPassengers().indexOf(e) == 0 ? -0.4 : 0.6); }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (p.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide) return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    public double cargoKg() { return cargoKg; }
    public void setCargoKg(double kg) { cargoKg = kg; }

    /** Load carried: the cargo plus 75 kg a rider. */
    public double loadKg() { return cargoKg + 75.0 * getPassengers().size(); }

    /** The raft's drift for one tick along {@code flow} (a unit-ish direction) at {@code currentMs}. Returns false once it has broken up. */
    public boolean step(Vec3 flowDir, double currentMs, boolean poling, RandomGenerator rng) {
        double v = RaftModel.speedMs(currentMs, poling) / 20.0;
        Vec3 dir = flowDir.lengthSqr() < 1e-9 ? Vec3.ZERO : flowDir.normalize();
        setDeltaMovement(dir.x * v, getDeltaMovement().y, dir.z * v);
        move(MoverType.SELF, getDeltaMovement());
        if (rng.nextDouble() < RaftModel.breakPerS(currentMs, loadKg()) / 20.0) {
            breakUp();
            return false;
        }
        return true;
    }

    /** The lashings give: the riders are in the water, the logs drop, cargo stays floating where it was. */
    public void breakUp() {
        if (level() instanceof ServerLevel sl) {
            ejectPassengers();
            for (int i = 0; i < 6; i++) spawnAtLocation(new ItemStack(Items.SPRUCE_LOG));
            sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.WOOD_BREAK, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.8f);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        var fluid = level().getFluidState(blockPosition());
        if (fluid.is(net.minecraft.tags.FluidTags.WATER)) {
            Vec3 flow = fluid.getFlow(level(), blockPosition());
            boolean rapids = flow.length() > 0.55 || flow.y < -0.2;
            double current = flow.length() < 1e-6 ? 0 : DEFAULT_CURRENT_MS * Math.min(2.5, 1 + flow.length());
            if (rapids) current = Math.max(current, RaftModel.RAPIDS_MS + 0.2);
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x, Math.max(m.y, 0.02), m.z);
            step(flow, current, false, new java.util.SplittableRandom(random.nextLong()));
        } else {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.8, 0.98, 0.8));
        }
    }
}
