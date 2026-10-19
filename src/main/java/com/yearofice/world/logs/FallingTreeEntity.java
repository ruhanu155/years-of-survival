package com.yearofice.world.logs;

import com.yearofice.build.wood.WoodConfig;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.build.wood.WoodRegistry;
import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.TreeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * {@code yearofice:falling_tree} (Trees, felling and firewood › Felling, step by step, 4): a felled tree turning about
 * its base for 1.5–3.0 s along its fall bearing; then it lands as one {@code felled_log} with its crown still on
 * and plays {@code tree.fall}. Only the client draws it ({@code client.render.wood}); the blocks are already gone.
 * A tree caught mid-fall by a save lands as soon as it loads.
 */
public class FallingTreeEntity extends Entity {
    static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(FallingTreeEntity.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(FallingTreeEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> BEARING = SynchedEntityData.defineId(FallingTreeEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FallingTreeEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(FallingTreeEntity.class, EntityDataSerializers.INT);

    private boolean landNow;

    public FallingTreeEntity(EntityType<? extends FallingTreeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setup(BlockPos base, TreeKind kind, int height, float bearing, int seed, int durationTicks) {
        moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        entityData.set(KIND, (byte) kind.ordinal());
        entityData.set(HEIGHT, height);
        entityData.set(BEARING, bearing);
        entityData.set(SEED, seed);
        entityData.set(DURATION, Math.max(1, durationTicks));
    }

    public TreeKind kind() { return TreeKind.byOrdinal(entityData.get(KIND)); }

    public int height() { return entityData.get(HEIGHT); }

    public float bearing() { return entityData.get(BEARING); }

    public int seed() { return entityData.get(SEED); }

    public int duration() { return entityData.get(DURATION); }

    /** Degrees from upright at {@code partial} ticks: it starts slow and speeds up, 90 on landing. */
    public float angle(float partial) {
        float u = Math.min(1f, (tickCount + partial) / duration());
        return 90f * u * u;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, (byte) 0);
        b.define(HEIGHT, 1);
        b.define(BEARING, 0f);
        b.define(SEED, 0);
        b.define(DURATION, 40);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (landNow || tickCount >= duration()) land();
    }

    /** Lands: one felled log, butt by the stump, lying along the bearing. */
    void land() {
        if (!(level() instanceof ServerLevel sl) || isRemoved()) return;
        TreeKind k = kind();
        FelledLogEntity log = new FelledLogEntity(WoodRegistry.FELLED_LOG.get(), sl);
        BlockPos base = blockPosition();
        log.setup(base, k, height(), bearing(), seed(), Bucking.usableM(height()), WoodConfig.get("green_moisture_pct"), WoodItems.today());
        sl.addFreshEntity(log);
        sl.playSound(null, log.getX(), log.getY(), log.getZ(), WoodRegistry.TREE_FALL.get(), SoundSource.BLOCKS, 1.4f, 0.9f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() {
        double r = height() + 3;
        return new AABB(getX() - r, getY(), getZ() - r, getX() + r, getY() + r, getZ() + r);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d2) { return d2 < 160 * 160; }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(KIND, t.getByte("kind"));
        entityData.set(HEIGHT, Math.max(1, t.getInt("height")));
        entityData.set(BEARING, t.getFloat("bearing"));
        entityData.set(SEED, t.getInt("seed"));
        entityData.set(DURATION, Math.max(1, t.getInt("duration")));
        landNow = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putByte("kind", entityData.get(KIND));
        t.putInt("height", height());
        t.putFloat("bearing", bearing());
        t.putInt("seed", seed());
        t.putInt("duration", duration());
    }

    /** The unit (x, z) of the fall. */
    public double[] dir() { return Felling.unit(bearing()); }
}
