package com.yearofice.travel.canoe;

import com.yearofice.api.weather.WeatherService;
import com.yearofice.build.metal.MetalConfig;
import com.yearofice.sim.travel.CanoeMath;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * {@code yearofice:canoe} (Homestead, built on the canoe bed): a boat that carries a paddler and a load. It rides and
 * paddles as a boat; wind past the limit on open water, rapids and an overload each add to a per-second chance of going
 * over ({@link CanoeMath}). A capsized canoe throws its riders and lies swamped for half a minute before it can be
 * boarded again. Sneak and use on it lifts it to the bank as the carried canoe, the portage.
 */
public class CanoeEntity extends Boat {
    /** Ticks a swamped canoe cannot be boarded. */
    public static final int SWAMP_TICKS = 600;

    private int swamped;
    /** GameTests pin wind, flow and load; null reads the world. */
    public Double windOverride;
    public Boolean rapidsOverride;

    public CanoeEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.BIRCH);
    }

    @Override public Item getDropItem() { return CanoeRegistry.CANOE.get(); }

    public boolean swamped() { return swamped > 0; }

    @Override
    protected boolean canAddPassenger(Entity e) { return swamped <= 0 && getPassengers().size() < 2; }

    /** Kilograms the riders carry, from the weight of every stack in their packs. */
    public double loadKg() {
        double g = 0;
        for (Entity e : getPassengers()) {
            if (!(e instanceof Player p)) continue;
            for (ItemStack s : p.getInventory().items) {
                Integer w = s.get(com.yearofice.item.component.YoiComponents.WEIGHT_G.get());
                if (w != null) g += (double) w * s.getCount();
            }
        }
        return g / 1000.0;
    }

    double wind() {
        if (windOverride != null) return windOverride;
        return WeatherService.installed() && level() instanceof net.minecraft.server.level.ServerLevel sl
                ? WeatherService.get().current(sl, blockPosition()).windMs() : 0;
    }

    boolean rapids() {
        if (rapidsOverride != null) return rapidsOverride;
        var fs = level().getFluidState(blockPosition());
        return !fs.isEmpty() && fs.getFlow(level(), blockPosition()).length() >= CanoeMath.RAPIDS_FLOW;
    }

    /** Rolls one second's chance of going over; returns true when it capsized. */
    public boolean capsizeRoll(double draw) {
        if (getPassengers().isEmpty()) return false;
        boolean exposed = level().canSeeSky(blockPosition());
        double p = CanoeMath.capsizeChancePerS(wind(), MetalConfig.get("canoe_capsize_wind_ms"), exposed, rapids(), loadKg(),
                MetalConfig.get("canoe_load_cap_kg"), MetalConfig.get("canoe_capsize_chance_per_s"));
        if (draw >= p) return false;
        capsize();
        return true;
    }

    public void capsize() {
        for (Entity e : new java.util.ArrayList<>(getPassengers())) {
            e.stopRiding();
            if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("entity.yearofice.canoe.capsized"), true);
        }
        swamped = SWAMP_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (swamped > 0) swamped--;
        else if (tickCount % 20 == 0) capsizeRoll(random.nextDouble());
    }

    @Override
    public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
        if (player.isSecondaryUseActive() && getPassengers().isEmpty()) {
            if (!level().isClientSide) {
                ItemStack carried = new ItemStack(CanoeRegistry.CANOE.get());
                if (!player.getInventory().add(carried)) player.drop(carried, false);
                discard();
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return swamped > 0 ? net.minecraft.world.InteractionResult.PASS : super.interact(player, hand);
    }
}
