package com.yearofice.ecosystem.wound;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.species.SpeciesDef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * Death is a pipeline, and nothing drops (Minecraft development › Carcass hand-off), the slice's steps:
 *
 * <ol>
 *   <li>Cause: a player's hit, a command or the void.</li>
 *   <li>Event: NeoForge's {@code LivingDeathEvent}, as any death posts it.</li>
 *   <li>Record: species, sex, cause, minute, the whole weight and the coat; {@code fresh} stamped as an unopened
 *       small carcass (k 0.6, s 1.0).</li>
 *   <li>Body: the living entity is discarded with no drops, experience or death smoke, and a
 *       {@code carcass_small} spawns in the same spot and facing; the collapse plays on it.</li>
 *   <li>Counts: the animal's slot waits out its respawn days.</li>
 * </ol>
 * Vanilla's {@code die} never runs, so loot tables, {@code doMobLoot} and experience never come into it. Medium to
 * huge carcasses (M6) are the same entity in two more sizes. Tiny, small and medium
 * carcasses (M5) are one entity class in three sizes; a class "none" (an insect) leaves nothing.
 */
public final class DeathPipeline {
    private DeathPipeline() {}

    public static void kill(YoiAnimal animal, DamageSource source, String cause) {
        if (!(animal.level() instanceof ServerLevel level) || animal.isRemoved()) return;
        if (CommonHooks.onLivingDeath(animal, source)) return;
        SpeciesDef d = animal.species();
        double minute = SliceClock.minuteD(level);
        net.minecraft.world.entity.EntityType<CarcassEntity> carcassType = carcassType(d.carcassClass());
        if (carcassType != null) {
            CarcassRecord r = record(animal, d, cause, minute);
            CarcassEntity c = carcassType.create(level);
            if (c != null) {
                c.moveTo(animal.getX(), animal.getY(), animal.getZ(), animal.getYRot(), 0);
                c.setRecord(r);
                level.addFreshEntity(c);
            }
        } else {
            YearOfIce.LOG.info("{} died ({}): a {} carcass ({} arrive with big game in M6)", d.id(), cause, d.carcassClass(), "large and huge");
        }
        Entity killer = source.getEntity();
        if (d.id().endsWith("caribou") && (animal.isInWater() || level.getFluidState(animal.blockPosition()).is(net.minecraft.tags.FluidTags.WATER))) {
            if (killer instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.yearofice.story.ambition.Ambitions.fire(sp, "hunt.caribou_crossing", (long) minute);
            } else if (level.getNearestPlayer(animal, 64) instanceof net.minecraft.server.level.ServerPlayer near) {
                com.yearofice.story.ambition.Ambitions.fire(near, "hunt.caribou_crossing", (long) minute);
            }
        }
        SlotSpawner.reportDeath(level, animal, killer != null);
        animal.remove(Entity.RemovalReason.KILLED);
    }

    /** The carcass entity for a size class (Bestiary › Carcass classes); null for a class with no body yet. */
    public static net.minecraft.world.entity.EntityType<CarcassEntity> carcassType(String cls) {
        return switch (cls) {
            case "tiny" -> EcosystemRegistry.CARCASS_TINY.get();
            case "small" -> EcosystemRegistry.CARCASS_SMALL.get();
            case "medium" -> EcosystemRegistry.CARCASS_MEDIUM.get();
            case "large" -> EcosystemRegistry.CARCASS_LARGE.get();
            case "huge" -> EcosystemRegistry.CARCASS_HUGE.get();
            default -> null;
        };
    }

    public static CarcassRecord record(YoiAnimal a, SpeciesDef d, String cause, double minute) {
        double kg = d.wholeKg() * (a.male() ? 1.0 : 0.95);
        return new CarcassRecord(d.id(), a.male() ? 1 : 0, cause, (long) Math.floor(minute), (int) Math.round(kg * 1000), a.coat(),
                Freshness.wholeCarcass(meatItem(d), minute));
    }

    public static Item meatItem(SpeciesDef d) {
        String m = d.meatItem();
        if (m == null) return Items.AIR;
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(m));
    }
}
