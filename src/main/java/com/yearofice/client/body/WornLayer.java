package com.yearofice.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yearofice.body.clothing.ClothingSlots;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.Zone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Map;

/**
 * Draws the worn garments on the player: each outer garment paints its zone of the armor-shaped model in the garment's
 * cloth colour (inner layers show only where no outer garment covers them). The colour per item id is the same ramp its
 * icon is painted from (tools/artgen/garments.py), so the body matches the inventory. Client only; the server decides
 * what is worn.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class WornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation CLOTH = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/models/cloth.png");
    /** Same sheet with only the lower half of each limb opaque: mitts and boots show as cuffs, not whole limbs. */
    private static final ResourceLocation CLOTH_LOW = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/models/cloth_low.png");

    /** Item id → ARGB cloth colour (mid step of the icon's main ramp). */
    private static final Map<String, Integer> COLOUR = Map.ofEntries(
            Map.entry("fleece_jacket", 0x353d63), Map.entry("rain_shell", 0xd68b49), Map.entry("work_pants", 0x7d6446),
            Map.entry("wool_socks", 0xa6a993), Map.entry("hiking_boots", 0x57452f), Map.entry("wool_beanie", 0xa83634),
            Map.entry("work_gloves", 0x9a8466), Map.entry("parka", 0xa59c8a), Map.entry("fur_mittens", 0x76603f),
            Map.entry("fur_hat", 0x76603f), Map.entry("hide_pants", 0x9a8466), Map.entry("mukluks", 0xbca885),
            Map.entry("hare_fur_liners", 0xcdd0d2));

    private final HumanoidModel<AbstractClientPlayer> inner, outer;

    private WornLayer(PlayerRenderer parent, HumanoidModel<AbstractClientPlayer> inner, HumanoidModel<AbstractClientPlayer> outer) {
        super(parent);
        this.inner = inner;
        this.outer = outer;
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (var skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer pr) {
                pr.addLayer(new WornLayer(pr,
                        new HumanoidModel<>(e.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(e.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (p.isInvisible()) return;
        for (Zone z : Zone.values()) {
            ItemStack st = ClothingSlots.stackAt(p, z, Layer.OUTER);
            boolean outerOn = draw(st, z, outer, pose, buf, light, p);
            if (!outerOn) draw(ClothingSlots.stackAt(p, z, Layer.INNER), z, inner, pose, buf, light, p);
        }
    }

    private boolean draw(ItemStack st, Zone z, HumanoidModel<AbstractClientPlayer> m, PoseStack pose, MultiBufferSource buf, int light, AbstractClientPlayer p) {
        if (st.isEmpty()) return false;
        Integer rgb = COLOUR.get(st.getItem().builtInRegistryHolder().key().location().getPath());
        if (rgb == null) return false;
        getParentModel().copyPropertiesTo(m);
        m.setAllVisible(false);
        switch (z) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case TORSO -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case HANDS -> { m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            case FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
        }
        boolean cuff = z == Zone.HANDS || z == Zone.FEET;
        var vc = buf.getBuffer(RenderType.entityCutoutNoCull(cuff ? CLOTH_LOW : CLOTH));
        pose.pushPose();
        if (cuff) pose.scale(1.02f, 1.0f, 1.02f);
        m.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 0xFF000000 | rgb);
        pose.popPose();
        return true;
    }
}
