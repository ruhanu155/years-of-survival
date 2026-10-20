package com.yearofice.client.light;

import com.yearofice.api.env.LightState;
import com.yearofice.client.hud.HudSmoke;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.light.LightItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The client smoke for held light (prototype P4). It rides on the HUD smoke ({@code YEAROFICE_SMOKE=hud}): it queues two
 * shots, a plain flat world and the same view with a lit bark torch in hand, and checks the mixin's number. With the
 * torch in hand the drawn light of a block three blocks from the eye must be lifted off the flat world's 0: the line
 * {@code SMOKE HELDLIGHT: ok} (or {@code FAILED}) in the log is the result, and the screenshots are
 * {@code hud_heldlight_off_s2.png} and {@code hud_heldlight_on_s2.png}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HeldLightSmoke {
    private HeldLightSmoke() {}

    static final boolean ON = "hud".equals(System.getenv("YEAROFICE_SMOKE"));
    private static boolean queued;
    private static int since = -1;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!queued) {
            queued = true;
            HudSmoke.queue(new HudSmoke.Shot("heldlight_off", 2, () -> {
                mc.options.hideGui = false;
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }));
            HudSmoke.queue(new HudSmoke.Shot("heldlight_on", 2, () -> {
                ItemStack torch = new ItemStack(LightItems.BIRCH_BARK_TORCH.get());
                LightState.set(torch, true, 15);
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, torch);
                since = 0;
            }));
            return;
        }
        if (since < 0 || mc.level == null || mc.player == null) return;
        if (++since == 8) {
            BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
            int near = (LevelRenderer.getLightColor(mc.level, eye) >> 4) & 0xF;
            int three = (LevelRenderer.getLightColor(mc.level, eye.east(3)) >> 4) & 0xF;
            int far = (LevelRenderer.getLightColor(mc.level, eye.east(20)) >> 4) & 0xF;
            boolean ok = near >= 8 && three >= 5 && three < near && far == 0 && !HeldLight.sources().isEmpty();
            YearOfIce.LOG.info("SMOKE HELDLIGHT: {} (block light at the eye {}, 3 blocks {}, 20 blocks {}, {} source(s))",
                    ok ? "ok" : "FAILED", near, three, far, HeldLight.sources().size());
        }
    }
}
