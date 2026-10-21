package com.yearofice.ecosystem.livestock;

import com.yearofice.ecosystem.species.bird.GrouseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A kept hen: the grouse rig, aliased to the spruce grouse art until the hen model lands. */
public class HenEntity extends GrouseEntity implements Livestock {
    private final LivestockCore core = new LivestockCore(LivestockSpecs.of("hen"));

    public HenEntity(EntityType<? extends HenEntity> type, Level level) {
        super(type, level);
        core.female = random.nextBoolean();
    }

    @Override
    public LivestockCore core() { return core; }

    @Override
    public boolean shouldBeSaved() { return true; }

    @Override
    public void tick() {
        super.tick();
        LivestockSystem.tick(this, core);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult r = LivestockSystem.interact(this, core, player, hand);
        return r != InteractionResult.PASS ? r : super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag t = new CompoundTag();
        core.save(t);
        tag.put("yoi_livestock", t);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("yoi_livestock")) core.load(tag.getCompound("yoi_livestock"));
    }
}
