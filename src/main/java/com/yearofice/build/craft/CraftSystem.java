package com.yearofice.build.craft;

import com.yearofice.api.build.GateService;
import com.yearofice.api.fire.FireStateEvent;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.fire.FireHeat;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Building's M3 crafting system: practice ({@link PracticeStore}), the gates ({@link GateStore}), the
 * {@code yearofice:station} recipe type and the workbench, tool wear and the knapping bench items. Called once from
 * {@code BuildSystem.init}.
 *
 * <p>Gate events wired here: the first fire a player lights opens Bushcraft, a first sinew in the pack opens Hide and
 * bone, and a first iron in the pack opens Iron (the items of {@code #yearofice:gate_hide_and_bone} and
 * {@code #yearofice:gate_iron}). Homestead waits for the river breakup with a warm room (M9); {@code /yoi gate open}
 * opens any. Until the notebook exists (M4), the Hands' recipes open on a right-click in the air with a knife.
 */
public final class CraftSystem {
    private CraftSystem() {}

    static final TagKey<Item> HIDE_AND_BONE_INPUTS = TagKey.create(Registries.ITEM, id("gate_hide_and_bone"));
    static final TagKey<Item> IRON_INPUTS = TagKey.create(Registries.ITEM, id("gate_iron"));
    static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, id("knives"));
    /** Ticks between looks at a pack for a gate's first input. */
    static final int GATE_SCAN_TICKS = 20;

    static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path);
    }

    public static void init(IEventBus modBus) {
        CraftRegistry.register(modBus);
        PracticeService.install(new PracticeStore());
        GateService.install(new GateStore());
        modBus.addListener(RegisterDataMapTypesEvent.class, e -> e.register(WearData.MAP));
        modBus.addListener(RegisterPayloadHandlersEvent.class, StationNet::payloads);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, CraftSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> StationCraft.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> StationCraft.reset());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) StationCraft.cancel(p);
        });
        NeoForge.EVENT_BUS.addListener(CraftSystem::knifeInAir);
        NeoForge.EVENT_BUS.addListener(CraftSystem::fireLit);
        NeoForge.EVENT_BUS.addListener(CraftSystem::scanForGates);
        NeoForge.EVENT_BUS.addListener(HomesteadGate::onTick);
        NeoForge.EVENT_BUS.addListener(CraftCommands::register);
    }

    static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(CraftRegistry.KNIFE.get());
            e.accept(CraftRegistry.FLINT_KNIFE.get());
            e.accept(CraftRegistry.TOOL_HANDLE.get());
            e.accept(CraftRegistry.STAKE.get());
        }
        if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            e.accept(CraftRegistry.ROUGH_STONE.get());
            e.accept(CraftRegistry.FLINT_NODULE.get());
        }
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(CraftRegistry.WORKBENCH_ITEM.get());
    }

    /** A knife in the main hand, used on the air, opens the Hands' recipes. */
    static void knifeInAir(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getLevel().isClientSide() || p.isShiftKeyDown()) return;
        if (!e.getItemStack().is(KNIVES)) return;
        StationNet.openHands(p);
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** The first fire a player lights, by match or bow drill, opens Bushcraft for every player standing at it. */
    static void fireLit(FireStateEvent e) {
        if (e.level().isClientSide() || !e.to().flaming() || e.from().flaming()) return;
        openBushcraftAt(e.pos(), ((net.minecraft.server.level.ServerLevel) e.level()).players());
    }

    /** Opens Bushcraft for the players within the fire's reach of {@code fire}; returns how many it opened it for. */
    public static int openBushcraftAt(net.minecraft.core.BlockPos fire, Iterable<ServerPlayer> players) {
        double r = FireHeat.ZERO_RADIUS_M;
        int n = 0;
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(fire.getCenter()) <= r * r && GateService.get().open(p, Tier.BUSHCRAFT, "first fire")) n++;
        }
        return n;
    }

    /** A first sinew or first iron in the pack opens its gate. */
    static void scanForGates(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % GATE_SCAN_TICKS != 7 || p.isSpectator()) return;
        scan(p);
    }

    /** One look at the pack for a first sinew or first iron (public so a GameTest can run it without ticking a player). */
    public static void scan(ServerPlayer p) {
        GateService g = GateService.get();
        boolean hide = g.isOpen(p, Tier.HIDE_AND_BONE), iron = g.isOpen(p, Tier.IRON);
        if (hide && iron) return;
        Inventory inv = p.getInventory();
        for (ItemStack s : StationCraft.held(inv)) {
            if (s.isEmpty()) continue;
            if (!hide && s.is(HIDE_AND_BONE_INPUTS)) g.open(p, Tier.HIDE_AND_BONE, "first sinew");
            if (!iron && s.is(IRON_INPUTS)) g.open(p, Tier.IRON, "first iron");
        }
    }
}
