package com.yearofice.ecosystem.bird;

import com.yearofice.core.YearOfIce;
import com.yearofice.food.YoiFoodItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The M6 birds' registry (Birds and the air › the M6 sheets): nine entity types on the bird rigs and the one shared
 * item {@code yearofice:bird_meat} their carcasses dress into. Sizes are the sheets' (width x height).
 */
public final class BirdRegistry {
    private BirdRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    /** Species ids in sheet order, with the size of each body. */
    public static final List<String> IDS = List.of("raven", "gray_jay", "stellers_jay", "bald_eagle", "great_horned_owl",
            "mallard", "northern_pintail", "common_loon", "mew_gull");
    private static final Map<String, float[]> SIZE = Map.of("raven", new float[] {0.5f, 0.5f}, "gray_jay", new float[] {0.3f, 0.3f},
            "stellers_jay", new float[] {0.3f, 0.3f}, "bald_eagle", new float[] {0.8f, 0.9f}, "great_horned_owl", new float[] {0.5f, 0.6f},
            "mallard", new float[] {0.55f, 0.45f}, "northern_pintail", new float[] {0.55f, 0.45f}, "common_loon", new float[] {0.7f, 0.55f},
            "mew_gull", new float[] {0.5f, 0.45f});
    /** Render scale against the grouse rig the models share (the grouse body is about 0.4 m long). */
    public static final Map<String, Float> SCALE = Map.of("raven", 1.2f, "gray_jay", 0.8f, "stellers_jay", 0.8f, "bald_eagle", 2.0f,
            "great_horned_owl", 1.4f, "mallard", 1.3f, "northern_pintail", 1.3f, "common_loon", 1.7f, "mew_gull", 1.2f);

    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<BirdEntity>>> TYPES = new LinkedHashMap<>();

    static {
        for (String id : IDS) {
            float[] sz = SIZE.get(id);
            TYPES.put(id, ENTITIES.register(id, () -> EntityType.Builder.of(BirdEntity::new, MobCategory.CREATURE)
                    .sized(sz[0], sz[1]).clientTrackingRange(10).build("yearofice:" + id)));
        }
    }

    /** Dressed bird: one item for all nine, with the species' own yield in its butchering table. */
    public static final DeferredItem<Item> BIRD_MEAT = ITEMS.register("bird_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));

    public static EntityType<BirdEntity> type(String path) {
        var h = TYPES.get(path);
        return h == null ? null : h.get();
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
    }
}
