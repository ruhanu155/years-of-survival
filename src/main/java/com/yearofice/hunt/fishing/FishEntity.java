package com.yearofice.hunt.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A live fish in a fishing spot (Blueprint › Fish as living mobs). It swims in the spot's water so the player can
 * read the water, and it is not the catch: a catch is drawn from the spot's stock (see {@link FishSpawner}, which
 * keeps the visible fish at a share of that stock, spawns them only near players and takes one away when a fish is
 * landed). Residents (grayling, dolly varden, pike, burbot, whitefish, lake trout) use the resident rig, salmon the
 * school rig. Not saved: the spawner repopulates a spot from its stock.
 */
public class FishEntity extends WaterAnimal implements GeoEntity {
    private static final EntityDataAccessor<String> SPECIES = SynchedEntityData.defineId(FishEntity.class, EntityDataSerializers.STRING);
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.fish.swim");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public FishEntity(EntityType<? extends FishEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02f, 0.1f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder attributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SPECIES, "");
    }

    public String species() { return entityData.get(SPECIES); }

    public void setSpecies(String id) { entityData.set(SPECIES, id); }

    /** Salmon swim as a school; the rest are residents. */
    public static boolean isSchool(String species) { return species.endsWith("_salmon"); }

    public boolean school() { return isSchool(species()); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new RandomSwimmingGoal(this, 1.0, 40));
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return new WaterBoundPathNavigation(this, level); }

    @Override
    public void travel(Vec3 v) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(getSpeed(), v);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            if (getTarget() == null) setDeltaMovement(getDeltaMovement().add(0, -0.005, 0));
        } else {
            super.travel(v);
        }
    }

    /** Spawned and removed by the spawner only; a fish out of the water is a lost fish. */
    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean isPersistenceRequired() { return true; }

    @Override
    public boolean requiresCustomPersistence() { return true; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("species", species());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setSpecies(t.getString("species"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "swim", 4, st -> st.setAndContinue(SWIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    /** The cell a fish at {@code pos} belongs to, for debug lines. */
    public BlockPos cell() { return blockPosition(); }
}
