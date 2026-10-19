package com.yearofice.body;

import com.yearofice.core.YearOfIce;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Vanilla needs replaced (Minecraft development › Player systems and client): the food level is held at 20 and
 * saturation and exhaustion at 0 every tick, so sprint and regeneration never read it; natural regeneration is
 * off by game rule; health is held at full (damage is routed to the body in {@link DamageRouter}). The hunger and
 * health bars are hidden on the client ({@code client.hud.HudLayers}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class VanillaNeeds {
    private VanillaNeeds() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        regenOff(e.getServer());
    }

    public static void regenOff(MinecraftServer server) {
        GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        if (rule.get()) {
            rule.set(false, server);
            YearOfIce.LOG.info("Game rule naturalRegeneration set to false (the body model owns healing)");
        }
    }

    /** The FoodData override: after vanilla's own food tick, put everything back. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) hold(p);
    }

    /** Also once a server tick, for players whose own tick is not driven by a connection (GameTest mocks). */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) hold(p);
    }

    public static void hold(Player p) {
        FoodData f = p.getFoodData();
        if (f.getFoodLevel() != 20) f.setFoodLevel(20);
        if (f.getSaturationLevel() != 0) f.setSaturation(0);
        if (f.getExhaustionLevel() != 0) f.setExhaustion(0);
        if (p.isAlive() && p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
