package com.yearofice.ecosystem.species.rig;

import com.yearofice.ecosystem.LandAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The ungulate rig's base. Animals over 40 kg cannot be pushed by a player (No vanilla mob mechanics, row 35).
 */
public abstract class UngulateEntity extends LandAnimal {
    protected UngulateEntity(EntityType<? extends UngulateEntity> type, Level level) { super(type, level); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void push(Entity other) {
        if (other instanceof Player) return;
        super.push(other);
    }
}
