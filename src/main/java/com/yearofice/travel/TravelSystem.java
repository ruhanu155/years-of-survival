package com.yearofice.travel;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.world.IceService;
import com.yearofice.body.clothing.WearMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.WarnOnce;
import com.yearofice.sim.travel.Travel;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Winter travel: the snowshoes slot's gear changes walking speed by surface, broken trails set overnight. */
public final class TravelSystem {
    private TravelSystem() {}

    static final String MODIFIER_PATH = "travel_speed";
    static final ResourceLocation MOD_ID = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, MODIFIER_PATH);
    /** Per-player warn-once memory; not saved (ponytail: a restart re-arms the warnings — upgrade: story attachment). */
    static final Map<UUID, WarnOnce> WARN = new ConcurrentHashMap<>();
    static int coldHours;

    public static void init(IEventBus modBus) {
        TravelItems.ITEMS.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(WearMap.WEAR));
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(TravelItems.SKATES.get()); e.accept(TravelItems.SKIS.get()); e.accept(TravelItems.CLIMBING_SKINS.get());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !p.isSpectator() && p.tickCount % 5 == 0) tick(p);
        });
        NeoForge.EVENT_BUS.addListener(TravelSystem::onHour);
        NeoForge.EVENT_BUS.addListener(TravelSystem::onMinute);
    }

    public static ItemStack slot(ServerPlayer p) {
        return CuriosApi.getCuriosInventory(p).flatMap(i -> i.getStacksHandler("snowshoes"))
                .map(h -> h.getStacks().getSlots() > 0 ? h.getStacks().getStackInSlot(0) : ItemStack.EMPTY).orElse(ItemStack.EMPTY);
    }

    static Travel.Gear gear(ServerPlayer p) {
        Item it = slot(p).getItem();
        if (it == TravelItems.SKATES.get()) return Travel.Gear.SKATES;
        if (it == TravelItems.SKIS.get()) {
            // ponytail: skins count while carried anywhere in the inventory — upgrade: a worn/applied state on the skis
            return p.getInventory().contains(new ItemStack(TravelItems.CLIMBING_SKINS.get())) ? Travel.Gear.SKIS_SKINS : Travel.Gear.SKIS;
        }
        return Travel.Gear.NONE;
    }

    /** Multiplier for this player right now (also read by GameTests). */
    public static double multiplier(ServerLevel level, BlockPos feet, Travel.Gear g, double slope) {
        BlockState at = level.getBlockState(feet);
        boolean snow = at.is(WorldBlocks.SNOW_COVER.get());
        boolean ice = IceBlocks.isIce(level.getBlockState(feet.below())) || IceBlocks.isIce(at);
        Travel.Surface s = snow ? Travel.Surface.SNOW : ice ? Travel.Surface.ICE : Travel.Surface.BARE;
        double cm = snow ? at.getValue(SnowCoverBlock.LAYERS) * 12.5 : 0;
        double m = Travel.speed(g, s, cm, slope);
        if (snow) m *= Travel.trailBonus(Travel.Trail.values()[at.getValue(SnowCoverBlock.TRAIL_WEAR)]);
        return m;
    }

    static void tick(ServerPlayer p) {
        Travel.Gear g = gear(p);
        double hd = Math.hypot(p.getX() - p.xo, p.getZ() - p.zo);
        double slope = hd > 0.01 ? (p.getY() - p.yo) / hd : 0;
        double m = multiplier(p.serverLevel(), p.blockPosition(), g, slope);
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) {
            a.removeModifier(MOD_ID);
            if (Math.abs(m - 1) > 0.01) a.addTransientModifier(new AttributeModifier(MOD_ID, m - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        ServerLevel level = p.serverLevel();
        BlockPos feet = p.blockPosition();
        BlockState at = level.getBlockState(feet);
        if (at.is(WorldBlocks.SNOW_COVER.get()) && at.getValue(SnowCoverBlock.TRAIL_WEAR) == 0 && hd > 0.05) // walking breaks a trail
            level.setBlock(feet, at.setValue(SnowCoverBlock.TRAIL_WEAR, 1), 2);
        WarnOnce w = WARN.computeIfAbsent(p.getUUID(), k -> new WarnOnce());
        long minute = level.getGameTime() / 20;
        boolean skating = g == Travel.Gear.SKATES;
        boolean thin = false;
        if (skating) thin = thinIce(level, feet);
        if (w.update("skate_thin", thin, minute)) p.displayClientMessage(Component.translatable("warn.yearofice.skate_thin"), true);
        boolean alone = skating && level.players().size() <= 1 && IceBlocks.isIce(level.getBlockState(feet.below()));
        if (w.update("skate_alone", alone, minute)) p.displayClientMessage(Component.translatable("warn.yearofice.skate_alone"), true);
    }

    /** True when the ice under {@code feet} is thin enough to crackle under a skater. */
    public static boolean thinIce(ServerLevel level, BlockPos feet) {
        if (!IceBlocks.isIce(level.getBlockState(feet.below()))) return false;
        try { return Travel.cracklesUnder(IceService.get().thicknessCm(level, feet.below())); }
        catch (IllegalStateException ignored) { return false; /* ice service not installed in this world */ }
    }

    /** Hourly: after 6 hours of hard frost every broken trail near a player is set. */
    static void onHour(ClockEvent.Hour e) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            hourStep(p.serverLevel(), p.blockPosition(), EnvService.get().sample(p.serverLevel(), p.blockPosition()).airC());
    }

    /** One game hour at {@code c} with air {@code airC}: trails within 16 blocks set once the frost has held. */
    public static void hourStep(ServerLevel level, BlockPos c, double airC) {
        coldHours = Travel.coldHours(coldHours, airC);
        if (coldHours < Travel.SET_AFTER_H) return;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-16, -3, -16), c.offset(16, 3, 16))) {
            BlockState s = level.getBlockState(q);
            if (s.is(WorldBlocks.SNOW_COVER.get()) && s.getValue(SnowCoverBlock.TRAIL_WEAR) == 1)
                level.setBlock(q.immutable(), s.setValue(SnowCoverBlock.TRAIL_WEAR, 2), 2);
        }
    }

    static final Map<UUID, Double> AIR = new ConcurrentHashMap<>();

    /** Each game minute: players in a closed snow room breathe it down; warn once when it goes stale. */
    static void onMinute(ClockEvent.Minute e) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            BlockPos head = BlockPos.containing(p.getEyePosition());
            SnowRoom.Scan s = SnowRoom.scan(p.serverLevel(), head);
            double air = s.snowRoom() ? Travel.air(AIR.getOrDefault(p.getUUID(), 1.0), e.count(), 1, s.flames(), s.vented(), s.volume()) : 1.0;
            AIR.put(p.getUUID(), air);
            WarnOnce w = WARN.computeIfAbsent(p.getUUID(), k -> new WarnOnce());
            if (w.update("snow_vent", Travel.stale(air), e.first())) p.displayClientMessage(Component.translatable("warn.yearofice.snow_vent"), true);
        }
    }
}
