package com.yearofice.story.radio;

import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** The crate on its parachute: sinks at a steady speed, lands on the ground and becomes a filled barrel (the manifest is data). */
public class SupplyParachute extends Entity {
    public SupplyParachute(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            setPos(getX(), getY() - RadioConfig.get("drop_fall_mps") / 20.0, getZ());
            return;
        }
        double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition().getX(), blockPosition().getZ());
        double next = getY() - RadioConfig.get("drop_fall_mps") / 20.0;
        if (next <= ground) {
            land((ServerLevel) level(), new BlockPos(blockPosition().getX(), (int) ground, blockPosition().getZ()));
            discard();
        } else {
            setPos(getX(), next, getZ());
        }
    }

    /** Places the crate (a barrel, ponytail: no custom crate block) and fills it from {@code radio/supply_drop.json}. */
    static void land(ServerLevel level, BlockPos at) {
        level.setBlock(at, Blocks.BARREL.defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof Container c) {
            int slot = 0;
            for (var e : RadioData.manifest().getAsJsonArray("manifest")) {
                JsonObject o = e.getAsJsonObject();
                var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(o.get("item").getAsString()));
                if (item.isEmpty() || slot >= c.getContainerSize()) {
                    if (item.isEmpty()) YearOfIce.LOG.warn("Supply drop manifest: unknown item {}", o.get("item").getAsString());
                    continue;
                }
                c.setItem(slot++, new ItemStack(item.get(), o.get("count").getAsInt()));
            }
        }
        String line = RadioData.skeds().getAsJsonObject("drop").get("landed_line").getAsString();
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(at.getX(), at.getY(), at.getZ()) < 600 * 600) p.sendSystemMessage(Component.literal(line));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
