package com.yearofice.build.craft;

import com.yearofice.sim.build.Practice;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/** The station screen's two payloads and the server side that fills them. The client installs {@link #sink} at start-up. */
public final class StationNet {
    private StationNet() {}

    /** Where the client's station screen takes its list; installed by {@code client.craft}, so a server never loads client code. */
    public static volatile Consumer<StationListPayload> sink = p -> { };

    static void payloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(StationListPayload.TYPE, StationListPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> sink.accept(p)));
        r.playToServer(StationPickPayload.TYPE, StationPickPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) StationCraft.start(sp, p.recipe(), p.hasBench() ? p.bench() : null);
        }));
    }

    /** Opens the screen at a workbench. */
    public static void openAt(ServerPlayer p, BlockPos bench) { PacketDistributor.sendToPlayer(p, list(p, bench)); }

    /** Opens the screen for the Hands (and any bench or fire close by). */
    public static void openHands(ServerPlayer p) { PacketDistributor.sendToPlayer(p, list(p, null)); }

    /** The list a player sees at {@code bench} (or with the Hands alone). */
    public static StationListPayload list(ServerPlayer p, @Nullable BlockPos bench) {
        BlockPos near = bench != null ? bench : StationCraft.benchNear(p.serverLevel(), p.blockPosition());
        Set<ResourceLocation> stations = StationCraft.stations(p, near);
        boolean hasBench = near != null && stations.contains(StationRecipe.WORKBENCH);
        List<StationListPayload.Row> rows = new ArrayList<>();
        for (RecipeHolder<StationRecipe> h : StationCraft.all(p.level())) {
            StationRecipe r = h.value();
            StationCraft.Plan plan = StationCraft.plan(p, h, stations);
            // Recipes of a closed gate stay hidden; a recipe at a station you are not at shows greyed ("at a workbench").
            if (plan.problem() == StationCraft.Problem.GATE) continue;
            List<StationListPayload.Need> needs = new ArrayList<>();
            for (SizedIngredient need : r.ingredients()) {
                ItemStack[] items = need.ingredient().getItems();
                ItemStack icon = items.length == 0 ? ItemStack.EMPTY : items[0].copyWithCount(1);
                Component name = items.length == 0 ? Component.empty() : items.length == 1 ? items[0].getHoverName()
                        : Component.empty().append(items[0].getHoverName()).append(" +");
                needs.add(new StationListPayload.Need(icon, name, need.count(), Math.min(need.count(), StationCraft.count(p.getInventory(), need))));
            }
            Component tool = r.tool().map(ToolReq::display).orElse(Component.empty());
            boolean toolOk = r.tool().isEmpty() || !StationCraft.findTool(p.getInventory(), r).isEmpty();
            rows.add(new StationListPayload.Row(h.id(), r.result().copy(), r.tier().number, plan.problem().ordinal(), (float) plan.minutes(),
                    (int) Math.round(plan.kcal()), plan.word().ordinal(), needs, tool, toolOk));
        }
        return new StationListPayload(hasBench, near == null ? BlockPos.ZERO : near, rows);
    }

    static Practice.Word word(int ordinal) { return Practice.Word.values()[Math.max(0, Math.min(2, ordinal))]; }
}
