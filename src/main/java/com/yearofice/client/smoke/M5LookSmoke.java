package com.yearofice.client.smoke;

import com.yearofice.core.YearOfIce;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * {@code YEAROFICE_SMOKE=m5look}: creates a flat creative world, builds a row of M5 things (racks, cache, deadfalls, snare
 * funnel, placer gravel), a pool with a gill net and a fish, a berry patch and a row of dropped items, and saves
 * {@code <game dir>/screenshots/m5_*.png}. Never active otherwise.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class M5LookSmoke {
    private M5LookSmoke() {}

    static final boolean ON = "m5look".equals(System.getenv("YEAROFICE_SMOKE"));
    private static final String[] ITEMS = {"hen_egg", "goat_milk", "barley_flour", "sauerkraut", "birch_sap", "birch_syrup", "dish",
            "gold_pan", "birch_pan", "black_sand", "pemmican", "raw_moose_meat", "chum_salmon", "spoon", "fly", "roe"};
    private record Cam(String name, double x, double y, double z, float yaw, float pitch) {}
    private static final List<Cam> CAMS = List.of(
            new Cam("racks", 4, -59.2, 7, 180, 15), new Cam("traps", 11, -59.2, 7, 180, 15),
            new Cam("water", 25, -58.5, 6, 180, 30), new Cam("plants", 40, -59, 6, 180, 20), new Cam("items", 58, -59.4, 4, 180, 35));
    private static int ticks = -1, inWorld = 0, shot = 0, wait = 0;
    private static boolean created = false;

    private static void set(ServerLevel l, int x, int z, String id, int dy) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        l.setBlockAndUpdate(new BlockPos(x, -60 + dy, z), b.defaultBlockState());
    }

    private static void build(ServerLevel l) {
        set(l, 0, 0, "yearofice:drying_rack", 0);
        set(l, 3, 0, "yearofice:smoke_rack", 0);
        set(l, 6, 0, "yearofice:food_cache", 0);
        set(l, 9, 0, "yearofice:deadfall", 0);
        Block d = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("yearofice:deadfall"));
        l.setBlockAndUpdate(new BlockPos(11, -60, 0), d.defaultBlockState().setValue((BooleanProperty) d.getStateDefinition().getProperty("sprung"), true));
        set(l, 13, 0, "yearofice:mouse_deadfall", 0);
        set(l, 15, 0, "yearofice:snare_funnel", 0);
        set(l, 17, 0, "yearofice:placer_gravel", 0);
        for (int x = 22; x <= 28; x++) for (int z = -2; z <= 2; z++) for (int dy = -2; dy <= -1; dy++)
            l.setBlockAndUpdate(new BlockPos(x, -60 + dy, z), Blocks.WATER.defaultBlockState());
        Block net = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("yearofice:gill_net"));
        BlockState ns = net.defaultBlockState();
        if (net.getStateDefinition().getProperty("waterlogged") instanceof BooleanProperty wl) ns = ns.setValue(wl, true);
        l.setBlockAndUpdate(new BlockPos(25, -61, 0), ns);
        l.setBlockAndUpdate(new BlockPos(24, -61, 0), ns);
        l.setBlockAndUpdate(new BlockPos(26, -61, 0), ns);
        String[] fish = {"arctic_grayling", "northern_pike"};
        for (int i = 0; i < fish.length; i++) {
            Entity e = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("yearofice:" + fish[i])).create(l);
            if (e == null) continue;
            e.moveTo(23.5 + i * 2.5, -61.5, 1.5, 0, 0);
            if (e instanceof Mob m) m.setNoAi(true);
            l.addFreshEntity(e);
        }
        String[] plants = {"blueberry_bush", "lingonberry_bush", "raspberry_bush", "cloudberry_bush", "fireweed", "yarrow", "morel", "wild_rhubarb", "labrador_tea", "cattail", "usnea", "sweetgrass"};
        for (int i = 0; i < plants.length; i++) set(l, 36 + i, (i % 2) * 1, "yearofice:" + plants[i], 0);
        for (int i = 0; i < ITEMS.length; i++) {
            var it = BuiltInRegistries.ITEM.get(ResourceLocation.parse("yearofice:" + ITEMS[i]));
            ItemEntity ie = new ItemEntity(l, 52.5 + i * 0.75, -59.2, 0.5, new ItemStack(it));
            ie.setNoGravity(true);
            ie.setDeltaMovement(0, 0, 0);
            ie.setUnlimitedLifetime();
            l.addFreshEntity(ie);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        ticks++;
        if (!created && mc.level == null && ticks > 40) {
            mc.options.onboardAccessibility = false;
            created = true;
            LevelSettings settings = new LevelSettings("yoi_m5look", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("yoi_m5look", settings, new WorldOptions(2718281L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null && inWorld < 120) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        inWorld++;
        if (inWorld == 10) {
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getToasts().clear();
            mc.options.hideGui = true;
        }
        if (inWorld == 60) mc.getSingleplayerServer().execute(() -> {
            ServerLevel l = mc.getSingleplayerServer().overworld();
            for (int cx = -2; cx <= 5; cx++) l.getChunk(cx, 0);
            build(l);
        });
        if (inWorld < 120) return;
        if (shot >= CAMS.size()) {
            if (shot == CAMS.size()) {
                YearOfIce.LOG.info("SMOKE M5LOOK DONE: {} screenshots", CAMS.size());
                shot++;
                mc.stop();
            }
            return;
        }
        Cam c = CAMS.get(shot);
        if (wait == 0) mc.getSingleplayerServer().execute(() -> {
            ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            p.getAbilities().flying = true;
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            p.teleportTo(mc.getSingleplayerServer().overworld(), c.x(), c.y(), c.z(), c.yaw(), c.pitch());
        });
        wait++;
        if (wait < 50) return;
        Screenshot.grab(mc.gameDirectory, "m5_" + c.name() + ".png", mc.getMainRenderTarget(), msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
        shot++;
        wait = 0;
    }
}
