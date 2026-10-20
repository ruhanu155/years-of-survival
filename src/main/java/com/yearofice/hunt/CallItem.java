package com.yearofice.hunt;

import com.google.gson.JsonObject;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.CallSoundedEvent;
import com.yearofice.sim.hunt.Calls;
import com.yearofice.sim.hunt.HuntData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A call (data {@code call/<id>.json}; Blueprint › Calling animals). Hold right mouse and let go: a tap is a short bull
 * grunt, a 2-4 s hold the cow moan, three wails of 2-3 s the distress squeak. Quality q comes from how close the hold
 * is to the call's rhythm (the thin bar at the crosshair is the client's), scaled by practice (0.8, 1.0, 1.2 at 10 and
 * 30 uses). The server posts a {@link CallSoundedEvent}; ecosystem rolls the answers.
 */
public class CallItem extends Item {
    /** GameTest hook: {wind to x, wind to z, m/s} replaces the world wind so a call's range is deterministic. */
    public static volatile double[] windOverride;

    private final String callId;

    public CallItem(Properties p, String callId) {
        super(p);
        this.callId = callId;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) { return UseAnim.TOOT_HORN; }

    @Override
    public int getUseDuration(ItemStack s, LivingEntity e) { return 72_000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(p.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity who, int timeLeft) {
        if (!(level instanceof ServerLevel sl) || !(who instanceof ServerPlayer p)) return;
        double heldS = (72_000 - timeLeft) / 20.0;
        CallSoundedEvent e = sound(sl, p, callId, heldS);
        if (e != null) p.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "%s (q %.2f)", e.sound().replace('_', ' '), e.quality())), true);
    }

    /** Builds, counts and posts the call for a hold of {@code heldS} seconds; the practice counter lives on the player. */
    public static CallSoundedEvent sound(ServerLevel sl, ServerPlayer p, String callId, double heldS) {
        JsonObject def = HuntData.call(callId);
        JsonObject sounds = def.getAsJsonObject("sounds");
        String sound;
        double target;
        if (callId.equals("moose_call")) {
            if (heldS < 0.6) { sound = "bull_grunt"; target = 0.3; } else { sound = "cow_moan"; target = 3.0; }
        } else { sound = "distress_squeak"; target = 2.5; }
        JsonObject s = sounds.getAsJsonObject(sound);
        double err = Math.abs(heldS - target);
        int uses = p.getPersistentData().getInt("yoi_call_uses_" + callId);
        p.getPersistentData().putInt("yoi_call_uses_" + callId, uses + 1);
        double q = Math.max(0.3, Math.min(1.0, 1 - 0.5 * err)) * Calls.practice(uses);
        Map<String, Double> base = new HashMap<>();
        for (var en : def.getAsJsonObject("species").entrySet()) {
            JsonObject sp = en.getValue().getAsJsonObject();
            double bp = sp.has("base_p") ? sp.get("base_p").getAsDouble() : s.has("base_p") ? s.get("base_p").getAsDouble() : 0;
            base.put(en.getKey(), bp);
        }
        double wx = 0, wz = 0, wms = 0;
        if (windOverride != null) { wx = windOverride[0]; wz = windOverride[1]; wms = windOverride[2]; }
        else try {
            var w = EnvService.get().wind(sl, p.blockPosition());
            double r = Math.toRadians(w.fromDeg());
            wx = -Math.sin(r);
            wz = Math.cos(r);
            wms = w.ms();
        } catch (IllegalStateException ignored) { /* env not installed */ }
        CallSoundedEvent ev = new CallSoundedEvent(sl, p, p.position(), callId, sound, q, s.get("range_m").getAsDouble(),
                s.has("falloff") && s.get("falloff").getAsString().equals("flat"), base, wx, wz, wms);
        NeoForge.EVENT_BUS.post(ev);
        return ev;
    }
}
