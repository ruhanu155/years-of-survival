package com.yearofice.ecosystem.livestock;

/** Marks a kept animal; the shared state is {@link LivestockCore}. */
public interface Livestock {
    LivestockCore core();
}
