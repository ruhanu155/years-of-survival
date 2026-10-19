package com.yearofice.build.camp;

import com.yearofice.build.BuildConfig;
import com.yearofice.build.BuildRegistry;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.HoldWork;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Cut boughs (Crafting and building in depth › More Start recipes: "The lower branches of a living spruce; 1 min;
 * 10 spruce boughs"). Hold the use key on a spruce leaf block ({@code #yearofice:bough_sources}) with an empty hand,
 * or with boughs in it; the tree is not harmed beyond the docs' vigor cost, so no block changes. The held key repeats
 * every 4 ticks, and while the hold is live the clock runs the Work rate; after {@code bough_cut_min} game minutes
 * the boughs go into the open slots (the rest drops at the feet) and the next cut starts.
 *
 * <p>Not yet: the knife or hatchet (the crate kit lands in M3, so the slice cuts by hand) and the tree's vigor −0.02 a
 * cut (Plants and fungi › Trees), which waits for the {@code yearofice:trees} record of the felling lane. The docs set
 * no per-tree limit beyond vigor, so none is applied.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BoughCutting {
    private BoughCutting() {}

    public static final TagKey<Block> SOURCES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "bough_sources"));

    private static final Map<UUID, HoldWork.Hold> HOLDS = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerPlayer> WHO = new ConcurrentHashMap<>();

    /** The hand may be empty or hold boughs already. */
    static boolean handFree(Player p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() || s.is(BuildRegistry.SPRUCE_BOUGHS.get());
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!e.getLevel().getBlockState(e.getPos()).is(SOURCES) || !handFree(e.getEntity())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer sp) use(sp, e.getPos(), sp.serverLevel().getGameTime());
    }

    /**
     * One repeat of the held use key on the leaf block at {@code pos} at game tick {@code tick}; returns the boughs
     * cut by this repeat (0 until the job is done).
     */
    public static int use(ServerPlayer p, BlockPos pos, long tick) {
        HoldWork.Hold was = HOLDS.get(p.getUUID());
        HoldWork.Hold h = was == null ? HoldWork.Hold.start(pos.asLong(), tick) : was.seen(pos.asLong(), tick);
        HOLDS.put(p.getUUID(), h);
        WHO.put(p.getUUID(), p);
        if (h.held(tick) < Work.ticks(BuildConfig.boughCutMin())) return 0;
        HOLDS.put(p.getUUID(), HoldWork.Hold.start(pos.asLong(), tick));
        int n = BuildConfig.boughsPerCut();
        ItemStack boughs = new ItemStack(BuildRegistry.SPRUCE_BOUGHS.get(), n);
        p.getInventory().add(boughs);
        if (!boughs.isEmpty()) p.drop(boughs, false);
        p.level().playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.8f, 0.9f);
        p.displayClientMessage(Component.translatable("building.yearofice.boughs_cut", n, carried(p)), true);
        return n;
    }

    /** Spruce boughs anywhere in the player's inventory. */
    public static int carried(Player p) {
        int c = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(BuildRegistry.SPRUCE_BOUGHS.get())) c += s.getCount();
        }
        return c;
    }

    /** While a hold is live its worker asks the Work rate; dead holds are dropped. Runs before the clock's step. */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        for (Iterator<Map.Entry<UUID, HoldWork.Hold>> it = HOLDS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, HoldWork.Hold> en = it.next();
            ServerPlayer p = WHO.get(en.getKey());
            if (p == null || !en.getValue().live(p.serverLevel().getGameTime())) {
                it.remove();
                WHO.remove(en.getKey());
                continue;
            }
            Work.tick(p, en.getValue().held(p.serverLevel().getGameTime()), "cutting boughs");
        }
    }

    /** True while {@code p} is holding the use key on a bough source. */
    public static boolean cutting(ServerPlayer p) {
        HoldWork.Hold h = HOLDS.get(p.getUUID());
        return h != null && h.live(p.serverLevel().getGameTime());
    }

    public static void forget(ServerPlayer p) {
        HOLDS.remove(p.getUUID());
        WHO.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        HOLDS.remove(e.getEntity().getUUID());
        WHO.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        HOLDS.clear();
        WHO.clear();
    }
}
