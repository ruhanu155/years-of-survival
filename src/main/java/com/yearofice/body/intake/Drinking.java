package com.yearofice.body.intake;

import com.yearofice.api.body.BodyService;
import com.yearofice.body.BodyConfig;
import com.yearofice.sim.body.WaterPurity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.Holder;

/**
 * Drinking from open water with an empty hand (Blueprint › Fire, water and light › Water). One use is one sip
 * ({@code body.sip_l}, 0.25 L, about 8 water points). Purity comes from {@link WaterPurity}: rivers, creeks and
 * lakes are raw (the preset's chance), swamps stand in for beaver ponds (double). Boiling and containers are M3.
 */
public final class Drinking {
    private Drinking() {}

    /** Ticks between sips, so a held key cannot drink a lake in a second. */
    public static final int SIP_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> LAST_SIP = new ConcurrentHashMap<>();

    /** The server half of the drink request: checks the hand, the ray and the block, then drinks. */
    public static boolean tryDrinkLookedAt(ServerPlayer p) {
        if (!p.getMainHandItem().isEmpty() || p.isSpectator()) return false;
        long now = p.serverLevel().getGameTime();
        Long last = LAST_SIP.get(p.getUUID());
        if (last != null && now - last < SIP_COOLDOWN_TICKS) return false;
        HitResult hit = p.pick(p.blockInteractionRange(), 1.0f, true);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = bh.getBlockPos();
        if (!isWaterSource(p.serverLevel(), pos)) return false;
        LAST_SIP.put(p.getUUID(), now);
        return drinkAt(p, pos);
    }

    public static boolean isWaterSource(ServerLevel level, BlockPos pos) {
        FluidState f = level.getFluidState(pos);
        return f.is(FluidTags.WATER) && f.isSource();
    }

    /** Drinks one sip from the water at {@code pos}. */
    public static boolean drinkAt(ServerPlayer p, BlockPos pos) {
        WaterPurity.Source src = sourceAt(p.serverLevel(), pos);
        BodyService.get().drink(p, BodyConfig.tuning().sipLitres(), src, false);
        p.serverLevel().playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    /** What kind of water this is, by biome, until the World department tags creeks and beaver ponds. */
    public static WaterPurity.Source sourceAt(ServerLevel level, BlockPos pos) {
        Holder<Biome> b = level.getBiome(pos);
        if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return WaterPurity.Source.BEAVER_POND;
        if (b.is(BiomeTags.IS_RIVER)) return WaterPurity.Source.RIVER;
        return WaterPurity.Source.LAKE;
    }
}
