package com.yearofice.api.build;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Posted on {@code NeoForge.EVENT_BUS} when a station recipe finishes (Crafting and building in depth › Data and
 * tests: "build.stations ... posting StationCraftEvent"). {@code made} is empty when the attempt failed (a knapped
 * core shattered).
 */
public class StationCraftEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation recipe;
    private final ResourceLocation station;
    private final ItemStack made;

    public StationCraftEvent(ServerPlayer player, ResourceLocation recipe, ResourceLocation station, ItemStack made) {
        this.player = player;
        this.recipe = recipe;
        this.station = station;
        this.made = made;
    }

    public ServerPlayer player() { return player; }
    public ResourceLocation recipe() { return recipe; }
    public ResourceLocation station() { return station; }
    public ItemStack made() { return made; }
    public boolean failed() { return made.isEmpty(); }
}
