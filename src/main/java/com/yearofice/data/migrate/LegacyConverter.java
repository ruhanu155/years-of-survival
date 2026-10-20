package com.yearofice.data.migrate;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Retired data components (Saves across versions): a retired component type stays registered as a legacy type, and
 * a rule here rewrites its value onto the live component (or drops it) the next time the stack is met. The pure
 * part, {@link #convertStack}, works on a stack's saved form ({@code id}, {@code count}, {@code components}) and also
 * applies the item alias; {@link #convertPlayer} runs it over a player's inventory on login. Container block entities
 * and item entities call {@link #convertStack} the same way when their owners adopt it.
 */
public final class LegacyConverter {
    /** {@code replacement} is {@code null} to drop the component; {@code step} (nullable) rewrites its value. */
    public record Rule(String legacy, String replacement, Step step) {}

    /** The rewritten stack, and whether anything changed. */
    public record Converted<U>(Dynamic<U> stack, boolean changed) {}

    private static final List<Rule> RULES = new ArrayList<>();

    private LegacyConverter() {}

    public static synchronized void register(Rule rule) { RULES.add(rule); }

    public static synchronized void clearForTest() { RULES.clear(); }

    public static synchronized List<Rule> rules() { return List.copyOf(RULES); }

    public static <U> Converted<U> convertStack(Dynamic<U> stack) { return convertStack(stack, rules(), Aliases.shared()); }

    @SuppressWarnings("unchecked")
    public static <U> Converted<U> convertStack(Dynamic<U> stack, List<Rule> rules, Aliases aliases) {
        Dynamic<?> d = stack;
        boolean changed = false;
        var id = d.get("id").asString().result();
        if (id.isPresent()) {
            String live = aliases.resolve("item", id.get());
            if (!live.equals(id.get())) { d = d.set("id", d.createString(live)); changed = true; }
        }
        var comps = d.get("components").result();
        if (comps.isPresent()) {
            Dynamic<?> c = comps.get();
            for (Rule r : rules) {
                var v = c.get(r.legacy()).result();
                if (v.isEmpty()) continue;
                c = c.remove(r.legacy());
                if (r.replacement() != null && c.get(r.replacement()).result().isEmpty()) {
                    Dynamic<?> nv = v.get();
                    if (r.step() != null) nv = r.step().apply(nv);
                    c = c.set(r.replacement(), nv);
                }
                changed = true;
            }
            if (changed) d = d.set("components", c);
        }
        return new Converted<>((Dynamic<U>) d, changed);
    }

    /** Rewrites a player's stacks; returns how many changed. Does nothing when no rule or alias applies. */
    public static int convertPlayer(ServerPlayer player) {
        List<Rule> rules = rules();
        Aliases aliases = Aliases.shared();
        if (rules.isEmpty() && aliases.of("item").isEmpty()) return 0;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());
        Inventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            Tag tag = ItemStack.CODEC.encodeStart(ops, s).result().orElse(null);
            if (tag == null) continue;
            Converted<Tag> c = convertStack(new Dynamic<>(ops, tag), rules, aliases);
            if (!c.changed()) continue;
            ItemStack back = ItemStack.CODEC.parse(ops, c.stack().getValue()).result().orElse(null);
            if (back != null) { inv.setItem(i, back); n++; }
        }
        return n;
    }

    /** Test helper: a JSON object as a stack in the ops of the caller's choice. */
    public static <U> Dynamic<U> fromJson(com.google.gson.JsonElement json, DynamicOps<U> ops) {
        return new Dynamic<>(JsonOps.INSTANCE, json).convert(ops);
    }
}
