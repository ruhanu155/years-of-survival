package com.yearofice.data.migrate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

/**
 * The versioned codec (Saves across versions): wraps a record's codec so older saves are upgraded on the raw map
 * before the record decodes. A record starts with no version field (v1); its first change sets {@code current} to 2,
 * adds a step from v1 to v2 (see {@link Migrations}) and from then on writes {@code v}. A record from a newer jar
 * fails to decode with an "update the mod" error instead of being read wrong. A record at v1 writes no {@code v}.
 */
public final class Versioned {
    private Versioned() {}

    public static <T> Codec<T> codec(Codec<T> inner, String migrationId, int current) {
        return codec(inner, migrationId, current, Migrations.DEFAULT_KEY, Migrations.shared());
    }

    public static <T> Codec<T> codec(Codec<T> inner, String migrationId, int current, String key, Migrations migrations) {
        if (current < 1) throw new IllegalArgumentException("record versions start at 1");
        return new Codec<>() {
            @Override
            public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U input) {
                Dynamic<U> up;
                try {
                    up = migrations.upgrade(migrationId, new Dynamic<>(ops, input), current, key);
                } catch (IllegalStateException e) {
                    return DataResult.error(e::getMessage);
                }
                return inner.decode(ops, up.getValue());
            }

            @Override
            public <U> DataResult<U> encode(T value, DynamicOps<U> ops, U prefix) {
                DataResult<U> r = inner.encode(value, ops, prefix);
                if (current == 1) return r;
                return r.flatMap(t -> ops.mergeToMap(t, ops.createString(key), ops.createInt(current)));
            }

            @Override
            public String toString() { return "Versioned[" + migrationId + " v" + current + "]"; }
        };
    }
}
