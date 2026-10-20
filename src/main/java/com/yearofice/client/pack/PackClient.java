package com.yearofice.client.pack;

import com.yearofice.client.hud.ClientHud;
import com.yearofice.client.hud.LoadMeterLayer;
import com.yearofice.client.ui.Units;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.inventory.LoadCalculator;
import com.yearofice.item.inventory.PackConfig;
import com.yearofice.item.inventory.PackRules;
import com.yearofice.net.payload.PickupPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The pack's client half:
 *
 * <ul>
 *   <li><b>Pickup by use.</b> Item entities are not hit by Minecraft's crosshair, so each tick the client ray-tests
 *       item boxes within reach and shows "RMB  Pick up flint (0.4 kg)"; the use key on one sends
 *       {@code yearofice:pickup} and the server decides.</li>
 *   <li><b>The load meter's reading</b>: the same sum the server makes, from synced slots and data maps.</li>
 *   <li><b>No ghost slots</b> (video 2, A9): with no pack, the inventory screen shows only the 9 belt slots (the
 *       locked rows are inactive, so they are not drawn) and, where the rows would be, a pack outline with "No
 *       pack: 9 belt slots, 5 kg. A pack adds rows."</li>
 * </ul>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class PackClient {
    private PackClient() {}

    private static @Nullable ItemEntity target;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        com.yearofice.net.payload.PackReadoutPayload.sink = r -> readout = r;
        LoadMeterLayer.reading = () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p == null || p.isSpectator() ? null : new LoadMeterLayer.Reading(LoadCalculator.kgOf(p), LoadCalculator.maxKgOf(p));
        };
    }

    /** The server's last pack readout: Strength-scaled band edges and the cap. */
    static volatile @Nullable com.yearofice.net.payload.PackReadoutPayload readout;

    /** The line the pack screen shows: "Light to 12 kg, heavy to 24, loaded to 36 · cap 25 kg". */
    public static Component bandLine(com.yearofice.net.payload.PackReadoutPayload r) {
        return Component.translatable("pack.yearofice.bands", Units.fixed(Math.round(r.edge0() * 100), "kg"),
                Units.fixed(Math.round(r.edge1() * 100), "kg"), Units.fixed(Math.round(r.edge2() * 100), "kg"),
                Units.fixed(Math.round(r.capKg() * 100), "kg"));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        target = mc.player == null || mc.screen != null || mc.player.isSpectator() ? null : ray(mc);
        if (target != null) {
            String kg = Units.fixed(Math.round(LoadCalculator.stackKg(target.getItem(), true) * 100), "kg");
            ClientHud.localPrompt("pickup", Component.translatable("messenger.yearofice.pickup.prompt",
                    mc.options.keyUse.getTranslatedKeyMessage(), target.getItem().getHoverName(), kg));
        }
    }

    static @Nullable ItemEntity ray(Minecraft mc) {
        LocalPlayer p = mc.player;
        double reach = PackConfig.get("pickup_reach_m");
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f);
        Vec3 end = eye.add(look.scale(reach));
        double blockDist = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                ? mc.hitResult.getLocation().distanceToSqr(eye) : reach * reach;
        AABB box = p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box,
                (Entity en) -> en instanceof ItemEntity && en.isAlive(), 0.3f);
        if (hit == null || hit.getLocation().distanceToSqr(eye) > blockDist + 0.5) return null;
        return (ItemEntity) hit.getEntity();
    }

    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isUseItem() || e.getHand() != InteractionHand.MAIN_HAND || target == null) return;
        PacketDistributor.sendToServer(new PickupPayload(target.getId()));
        e.setSwingHand(true);
        e.setCanceled(true);
    }

    /**
     * Locked rows are never drawn (A9), in any container screen: each player-inventory slot of a locked row is
     * painted over with the panel colour, since the screen's background texture has the empty frame printed on it.
     * With no pack, the inventory screen also gets a pack outline and the line where the rows would be.
     */
    @SubscribeEvent
    public static void screen(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> s)) return;
        if (s instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        int rows = PackRules.openRows(p);
        GuiGraphics g = e.getGuiGraphics();
        var ro = readout;
        if (ro != null && s instanceof InventoryScreen) {
            var font = Minecraft.getInstance().font;
            g.drawString(font, bandLine(ro), s.getGuiLeft() + 8, s.getGuiTop() + s.getYSize() + 3, 0xFFDDDDDD, true);
        }
        if (rows >= 3) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        for (net.minecraft.world.inventory.Slot slot : s.getMenu().slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory && PackRules.row(slot.getContainerSlot()) >= rows) {
                int x = s.getGuiLeft() + slot.x - 1, y = s.getGuiTop() + slot.y - 1;
                g.fill(x, y, x + 18, y + 18, 0xFFC6C6C6);
            }
        }
        if (rows == 0 && s instanceof InventoryScreen) {
            int x = s.getGuiLeft() + 7, y = s.getGuiTop() + 83, w = 162, h = 54;
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0xFF8B8B8B);
            Component line = Component.translatable("pack.yearofice.no_pack", 9, Units.fixed(Math.round(PackConfig.get("no_pack_kg") * 100), "kg"));
            var font = Minecraft.getInstance().font;
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(line, w - 14);
            int ty = y + h / 2 - lines.size() * font.lineHeight / 2;
            for (var l : lines) {
                g.drawString(font, l, x + w / 2 - font.width(l) / 2, ty, 0xFF404040, false);
                ty += font.lineHeight;
            }
        }
        g.pose().popPose();
    }
}
