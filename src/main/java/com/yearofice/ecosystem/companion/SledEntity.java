package com.yearofice.ecosystem.companion;

import com.yearofice.sim.companion.CompanionTuning;
import com.yearofice.sim.companion.SledMath;
import com.yearofice.sim.travel.Travel;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * {@code yearofice:sled}: a dog sled pulled by two to six harnessed Companion wolves over snow and ice (Minecraft
 * development › The wolf: pack and sled). The server moves it: forward input of the rider sets the team going, the
 * rider's look sets the heading, speed and load follow the surface ({@link SledMath}). A pass over snow cover wears the
 * trail one step (a pass over a set trail makes the {@code sled_lane}). Sneak-use with an empty hand hitches the
 * Companion wolves within 8 m; sneak-hitting it takes it back.
 */
public class SledEntity extends Entity {
    private final List<UUID> team = new ArrayList<>();
    private double cargoKg;
    private BlockPos lastCell;
    private Float driveYaw, driveThrust;
    private double speedMps;
    private long msgTick;

    public SledEntity(EntityType<? extends SledEntity> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

    public int teamSize() { return team.size(); }

    public double speedMps() { return speedMps; }

    public double cargoKg() { return cargoKg; }

    public void setCargoKg(double kg) { cargoKg = kg; }

    /** GameTests and {@code /yoi sled drive}: steer without a rider (thrust 0..1). */
    public void drive(float yaw, float thrust) {
        driveYaw = yaw;
        driveThrust = thrust;
    }

    public void stopDriving() {
        driveYaw = null;
        driveThrust = null;
    }

    public boolean hitch(WolfEntity w) {
        if (!w.canPull() || w.harnessed() || team.size() >= CompanionConfig.tuning().sledMaxTeam()) return false;
        team.add(w.getUUID());
        w.setSled(getUUID());
        return true;
    }

    public int hitchNear(double range) { return hitchNear(range, null); }

    /** Hitches the grown Companion wolves within range (of one test group in GameTests, else none). */
    public int hitchNear(double range, String group) {
        int n = 0;
        for (WolfEntity w : level().getEntitiesOfClass(WolfEntity.class, getBoundingBox().inflate(range, 4, range),
                w -> w.isAlive() && w.canPull() && !w.harnessed() && java.util.Objects.equals(w.testGroup(), group))) {
            if (hitch(w)) n++;
        }
        return n;
    }

    public void unhitchAll() {
        if (level() instanceof ServerLevel sl) for (UUID id : team) if (sl.getEntity(id) instanceof WolfEntity w) w.setSled(null);
        team.clear();
    }

    List<WolfEntity> wolves() {
        List<WolfEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel sl)) return out;
        for (UUID id : new ArrayList<>(team)) {
            if (sl.getEntity(id) instanceof WolfEntity w && w.isAlive()) out.add(w);
        }
        return out;
    }

    public SledMath.Surface surface() {
        BlockPos p = blockPosition();
        BlockState here = level().getBlockState(p);
        if (here.is(WorldBlocks.SNOW_COVER.get())) return here.getValue(SnowCoverBlock.TRAIL_WEAR) >= 3 ? SledMath.Surface.LANE : SledMath.Surface.SNOW;
        if (IceBlocks.isIce(level().getBlockState(p.below())) || level().getBlockState(p.below()).is(net.minecraft.tags.BlockTags.ICE)) return SledMath.Surface.ICE;
        return SledMath.Surface.BARE;
    }

    @Override
    public float maxUpStep() { return 0.6f; }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            int n = hitchNear(8);
            player.displayClientMessage(Component.literal(n > 0 ? n + " wolves hitched; the team is " + team.size() + "." : "No Companion wolf within 8 m ready to pull."), true);
            return InteractionResult.CONSUME;
        }
        if (getPassengers().isEmpty()) player.startRiding(this);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player p && p.isShiftKeyDown() && !isRemoved()) {
            unhitchAll();
            level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.3, getZ(), new ItemStack(CompanionRegistry.DOG_SLED.get())));
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        CompanionTuning t = CompanionConfig.tuning();
        List<WolfEntity> wolves = wolves();
        Entity rider = getFirstPassenger();
        float thrust = driveThrust != null ? driveThrust : rider instanceof Player p ? p.zza : 0;
        float yaw = driveYaw != null ? driveYaw : rider != null ? rider.getYRot() : getYRot();
        double load = cargoKg + getPassengers().stream().mapToDouble(e -> e instanceof Player ? 70 : 40).sum();
        SledMath.Surface s = surface();
        double target = thrust > 0 ? SledMath.speedMps(t, wolves.size(), s, load) : 0;
        if (thrust > 0 && target == 0 && level.getGameTime() - msgTick > 100 && rider instanceof ServerPlayer sp) {
            msgTick = level.getGameTime();
            sp.displayClientMessage(Component.literal(wolves.size() < 2 ? "The sled needs a team of at least two wolves."
                    : "The team balks: " + Math.round(load) + " kg is more than it can pull here."), true);
        }
        speedMps += (target - speedMps) * 0.15;
        if (speedMps < 0.02) speedMps = 0;
        setYRot(Mth.rotLerp(0.15f, getYRot(), yaw));
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        Vec3 v = new Vec3(dir.x * speedMps / 20.0, onGround() ? -0.02 : getDeltaMovement().y - 0.08, dir.z * speedMps / 20.0);
        move(MoverType.SELF, v);
        setDeltaMovement(v);
        wearLane(level);
        // the rider works the sled too: steering and braking cost a little every 10 s of real driving
        if (speedMps > 0.3 && tickCount % EFFORT_TICKS == 0 && rider instanceof ServerPlayer rp)
            com.yearofice.api.body.BodyService.get().effort(rp, "haul", EFFORT_KCAL, EFFORT_GAME_MIN);
        placeTeam(wolves, dir);
    }

    private static final int EFFORT_TICKS = 200;
    private static final double EFFORT_KCAL = 6, EFFORT_GAME_MIN = 4;

    private void wearLane(ServerLevel level) {
        BlockPos cell = blockPosition();
        if (cell.equals(lastCell) || speedMps < 0.2) return;
        lastCell = cell;
        BlockState s = level.getBlockState(cell);
        if (!s.is(WorldBlocks.SNOW_COVER.get())) return;
        int wear = s.getValue(SnowCoverBlock.TRAIL_WEAR);
        int next = wear == Travel.Trail.SET.ordinal() ? Travel.nextTrail(Travel.Trail.SET, 0, 0, true).ordinal() : Math.min(wear + 1, Travel.Trail.SET.ordinal());
        if (next != wear) level.setBlock(cell, s.setValue(SnowCoverBlock.TRAIL_WEAR, next), 2);
    }

    private void placeTeam(List<WolfEntity> wolves, Vec3 dir) {
        Vec3 right = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i < wolves.size(); i++) {
            int row = i / 2;
            double side = (i % 2 == 0 ? -0.6 : 0.6);
            Vec3 at = position().add(dir.scale(2.2 + 1.4 * row)).add(right.scale(side));
            wolves.get(i).harnessPose(at, getYRot(), speedMps > 0.3);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        team.clear();
        ListTag l = tag.getList("team", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < l.size(); i++) team.add(NbtUtils.loadUUID(l.get(i)));
        cargoKg = tag.getDouble("cargo_kg");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag l = new ListTag();
        for (UUID id : team) l.add(NbtUtils.createUUID(id));
        tag.put("team", l);
        tag.putDouble("cargo_kg", cargoKg);
    }
}
