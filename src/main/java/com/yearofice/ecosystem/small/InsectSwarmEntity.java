package com.yearofice.ecosystem.small;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.InjurySpec;
import com.yearofice.body.injury.InjuryData;
import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.life.Bugs;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One biting-insect swarm (mosquito, black fly or no-see-um; Land animals in depth: the four insect entities exist
 * only within 8 blocks). It drifts to the nearest player, draws specks as particles and bites exposed skin through
 * {@link BodyService#applyInjury} at a rate that follows the bug pressure. A head net stops mosquitoes and black flies,
 * not no-see-ums; a fire within 6 m keeps every swarm off. Not saved: {@link InsectService} places and removes swarms.
 */
public class InsectSwarmEntity extends Entity {
    private Bugs.Kind kind = Bugs.Kind.MOSQUITO;
    private double nextBiteS = 4;
    /** Bites delivered since this swarm formed, for the GameTests and {@code /yoi insects}. */
    public int bites;

    public InsectSwarmEntity(EntityType<? extends InsectSwarmEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setKind(Bugs.Kind k) { kind = k; }

    public Bugs.Kind kind() { return kind; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    /** A head net stops mosquitoes and black flies; nothing but smoke stops no-see-ums. */
    public static boolean netted(ServerPlayer p, Bugs.Kind k) {
        return k != Bugs.Kind.NO_SEE_UM && p.getItemBySlot(EquipmentSlot.HEAD).is(com.yearofice.item.inventory.KitItems.MOSQUITO_HEAD_NET.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        double range = LifeConfig.tuning().insectRangeM();
        ServerPlayer p = (ServerPlayer) level.getNearestPlayer(this, range);
        if (p == null || p.isSpectator() || InsectService.smoked(level, p.position())) {
            discard();
            return;
        }
        Vec3 target = p.position().add(0, p.getBbHeight() * 0.8, 0);
        Vec3 to = target.subtract(position());
        Vec3 jitter = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(0.25);
        double d = to.length();
        setPos(position().add(d > 0.7 ? to.scale(0.12 / Math.max(0.5, d) * 2).add(jitter) : jitter));
        if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 3, 0.25, 0.25, 0.25, 0.0);
        if (d > 1.6) return;
        nextBiteS -= 0.05; // a tick of game time at the default clock is a twentieth of a second
        if (nextBiteS > 0) return;
        double b = InsectService.pressureAt(level, p.blockPosition(), kind);
        nextBiteS = Math.min(120, Bugs.biteIntervalS(Math.max(b, 0.3), LifeConfig.tuning().biteIntervalS()));
        if (!Bugs.exposed(b, netted(p, kind))) return;
        bite(p);
    }

    /** One bite: a tiny Condition loss on the head, arms or legs from the {@code injury_source/<kind>_bite.json} row. */
    public void bite(ServerPlayer p) {
        InjurySource src = InjuryData.source(kind.path() + "_bite", InjurySource.simple(kind.path() + "_bite", 0.15,
                java.util.Map.of("head", 40.0, "arms", 30.0, "legs", 30.0), List.of()));
        BodyService.get().applyInjury(p, InjurySpec.of(src).withLoss(src.conditionLoss() * LifeConfig.tuning().biteScale()), "yearofice:" + kind.path());
        bites++;
        InsectService.bitesTotal++;
    }
}
