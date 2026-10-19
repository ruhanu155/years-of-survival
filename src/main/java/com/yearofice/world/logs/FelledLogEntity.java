package com.yearofice.world.logs;

import com.yearofice.build.wood.WoodConfig;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * {@code yearofice:felled_log} (Trees, felling and firewood › Limbing and bucking): a felled tree lying on the ground,
 * butt by its stump, with its crown as limbs until it is limbed. Holding use with a tool limbs it, then bucks it
 * into rounds from the top end ({@code build.wood.WoodJobs}). It dries as a whole log in the open (B4: × 0.25 ×
 * 0.7), and one not worked for 7 game days settles into {@code fallen_log} blocks, so entities never pile up.
 */
public class FelledLogEntity extends Entity {
    static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> BEARING = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> LIMBED = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.BOOLEAN);

    private double moisture = Seasoning.GREEN_PCT;
    private long stampDay;
    private long workedDay;

    public FelledLogEntity(EntityType<? extends FelledLogEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Lays the log: butt half a block out from the stump at {@code base}, along compass bearing {@code bearing}. */
    public void setup(BlockPos base, TreeKind kind, int height, float bearing, int seed, double lengthM, double moisturePct, long day) {
        double[] d = Felling.unit(bearing);
        entityData.set(KIND, (byte) kind.ordinal());
        entityData.set(HEIGHT, height);
        entityData.set(BEARING, bearing);
        entityData.set(SEED, seed);
        entityData.set(LENGTH, (float) lengthM);
        entityData.set(LIMBED, false);
        moisture = moisturePct;
        stampDay = day;
        workedDay = day;
        moveTo(base.getX() + 0.5 + d[0] * 0.5, base.getY(), base.getZ() + 0.5 + d[1] * 0.5, 0, 0);
        setBoundingBox(makeBoundingBox());
    }

    public TreeKind kind() { return TreeKind.byOrdinal(entityData.get(KIND)); }

    public int treeHeight() { return entityData.get(HEIGHT); }

    public float bearing() { return entityData.get(BEARING); }

    public int seed() { return entityData.get(SEED); }

    public double lengthM() { return entityData.get(LENGTH); }

    public boolean limbed() { return entityData.get(LIMBED); }

    public int roundsLeft() { return Bucking.rounds(lengthM()); }

    public void setLimbed() {
        entityData.set(LIMBED, true);
        workedDay = WoodItems.today();
    }

    /** Takes one round's length off the top end; discards the log when no round is left. */
    public void cutRound() {
        float left = (float) Math.max(0, lengthM() - Bucking.ROUND_M);
        entityData.set(LENGTH, left);
        workedDay = WoodItems.today();
        setBoundingBox(makeBoundingBox());
        if (Bucking.rounds(left) <= 0) discard();
    }

    /** The log's moisture today: green when felled, drying as a whole log in the open. */
    public double moistureNow(long day) {
        return Seasoning.dry(WoodConfig.rates(), moisture, stampDay, day, Seasoning.Place.UNCOVERED, Seasoning.Form.LOG);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, (byte) 0);
        b.define(HEIGHT, 1);
        b.define(BEARING, 0f);
        b.define(SEED, 0);
        b.define(LENGTH, 1f);
        b.define(LIMBED, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == LENGTH || key == BEARING) setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        if (entityData == null) return super.makeBoundingBox();
        double[] d = Felling.unit(bearing());
        double len = Math.max(0.5, lengthM());
        double x1 = getX() + d[0] * len, z1 = getZ() + d[1] * len;
        double r = 0.35;
        return new AABB(Math.min(getX(), x1) - r, getY(), Math.min(getZ(), z1) - r, Math.max(getX(), x1) + r, getY() + 0.7, Math.max(getZ(), z1) + r);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || tickCount % 100 != 0) return;
        if (WoodItems.today() - workedDay >= WoodConfig.get("felled_log_settle_days")) settle(sl);
    }

    /** Settles into {@code fallen_log} blocks along its nearest axis, where there is air on ground; then it is gone. */
    void settle(ServerLevel sl) {
        double[] d = Felling.unit(bearing());
        Direction.Axis axis = Math.abs(d[0]) >= Math.abs(d[1]) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState log = WorldBlocks.FALLEN_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        int n = (int) Math.floor(lengthM());
        for (int i = 0; i < n; i++) {
            BlockPos p = BlockPos.containing(getX() + d[0] * (i + 0.5), getY() + 0.2, getZ() + d[1] * (i + 0.5));
            if (sl.getBlockState(p).canBeReplaced() && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), Direction.UP)) {
                sl.setBlock(p, log, Block.UPDATE_CLIENTS);
            }
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d2) { return d2 < 128 * 128; }

    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(limbed() ? 0.5 : 4.0); }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(KIND, t.getByte("kind"));
        entityData.set(HEIGHT, Math.max(1, t.getInt("height")));
        entityData.set(BEARING, t.getFloat("bearing"));
        entityData.set(SEED, t.getInt("seed"));
        entityData.set(LENGTH, t.getFloat("length_m"));
        entityData.set(LIMBED, t.getBoolean("limbed"));
        moisture = t.getDouble("moisture_pct");
        stampDay = t.getLong("stamp_day");
        workedDay = t.getLong("worked_day");
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putByte("kind", entityData.get(KIND));
        t.putInt("height", treeHeight());
        t.putFloat("bearing", bearing());
        t.putInt("seed", seed());
        t.putFloat("length_m", (float) lengthM());
        t.putBoolean("limbed", limbed());
        t.putDouble("moisture_pct", moisture);
        t.putLong("stamp_day", stampDay);
        t.putLong("worked_day", workedDay);
    }
}
