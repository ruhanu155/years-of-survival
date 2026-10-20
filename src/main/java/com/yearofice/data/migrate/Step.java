package com.yearofice.data.migrate;

import com.mojang.serialization.Dynamic;

/** One migration step: a pure function from a record's raw map to the next version's map, before its codec runs. */
@FunctionalInterface
public interface Step {
    Dynamic<?> apply(Dynamic<?> record);
}
