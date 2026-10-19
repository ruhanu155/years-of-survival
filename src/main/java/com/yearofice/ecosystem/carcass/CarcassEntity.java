package com.yearofice.ecosystem.carcass;

import com.yearofice.ecosystem.EcosystemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * {@code yearofice:carcass_small} (Bestiary › The carcass entity, In Minecraft: carcasses): a whole small body
 * where the animal fell. Not a living entity, so it has no health, hurt time or AI by construction. Its record is
 * the {@code yearofice:carcass} attachment; the client gets only the species and coat. It settles to the ground,
 * then ticks no movement. Use (empty hand, or crouched) takes it up as {@code yearofice:whole_game}, the small-game
 * item that carries the same record. Saved with its chunk; it never despawns by distance.
 */
public class CarcassEntity extends Entity implements GeoEntity {
    static final EntityDataAccessor<String> SPECIES = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.STRING);
    static final EntityDataAccessor<Byte> COAT = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean settled;

    public CarcassEntity(EntityType<? extends CarcassEntity> type, Level level) { super(type, level); }

    public CarcassRecord record() { return getData(EcosystemRegistry.CARCASS); }

    public void setRecord(CarcassRecord r) {
        setData(EcosystemRegistry.CARCASS, r);
        entityData.set(SPECIES, r.species());
        entityData.set(COAT, (byte) r.coat());
    }

    public String speciesId() { return entityData.get(SPECIES); }

    public String speciesPath() {
        String s = speciesId();
        return s.substring(s.indexOf(':') + 1);
    }

    public int coat() { return entityData.get(COAT); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SPECIES, "yearofice:snowshoe_hare").define(COAT, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (settled && onGround()) return;
        Vec3 v = getDeltaMovement();
        if (!onGround()) v = v.add(0, -0.04, 0);
        move(MoverType.SELF, v);
        setDeltaMovement(v.multiply(0.6, 0.98, 0.6));
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            settled = true;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        ItemStack whole = WholeGameItem.of(record());
        if (!player.getInventory().add(whole)) player.drop(whole, false);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        settled = tag.getBoolean("settled");
        CarcassRecord r = record();
        entityData.set(SPECIES, r.species());
        entityData.set(COAT, (byte) r.coat());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { tag.putBoolean("settled", settled); }

    // The collapse plays once and holds on its last frame: the death pose.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "pose", 0,
                st -> st.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation." + speciesPath() + ".death"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean shouldBeSaved() { return !isRemoved(); }

    @Override
    public void setDeltaMovement(Vec3 v) { super.setDeltaMovement(v); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void push(Entity e) {}
}
