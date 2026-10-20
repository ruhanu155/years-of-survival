package com.yearofice.build.craft;

import com.yearofice.api.build.GateOpenedEvent;
import com.yearofice.api.build.GateService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.Gates;
import com.yearofice.sim.build.Tier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The gates' only writer (the {@code yearofice:gates} attachment). Opening one posts its one Trail Messenger line
 * ({@code messenger.yearofice.event.gate_<tier>}) and a {@link GateOpenedEvent}. Gates never close.
 */
public final class GateStore implements GateService {

    @Override public boolean isOpen(Player p, Tier tier) { return Gates.isOpen(mask(p), tier); }

    @Override public int mask(Player p) { return p.getData(CraftAttachments.GATES.get()).mask() | Gates.NEW_WORLD; }

    @Override
    public boolean open(ServerPlayer p, Tier tier, String cause) {
        if (isOpen(p, tier)) return false;
        p.setData(CraftAttachments.GATES.get(), new GateData(Gates.open(mask(p), tier)));
        YearOfIce.LOG.info("Gate {} opened for {} ({})", tier.id(), p.getGameProfile().getName(), cause);
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.EVENT, "gate_" + tier.id(), "building"));
        NeoForge.EVENT_BUS.post(new GateOpenedEvent(p, tier, cause));
        return true;
    }
}
