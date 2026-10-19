package com.yearofice.worldgen;

/**
 * A binary min-heap of (key, cell) pairs for the priority floods. Ties on the key break on the cell index, so
 * the pop order, and every layer built from it, is fully determined by the input.
 */
final class CellHeap {
    private double[] keys;
    private int[] cells;
    private int size;

    CellHeap(int capacity) {
        keys = new double[Math.max(16, capacity)];
        cells = new int[keys.length];
    }

    int size() { return size; }

    boolean isEmpty() { return size == 0; }

    private boolean less(int a, int b) {
        return keys[a] < keys[b] || (keys[a] == keys[b] && cells[a] < cells[b]);
    }

    void push(double key, int cell) {
        if (size == keys.length) {
            keys = java.util.Arrays.copyOf(keys, size * 2);
            cells = java.util.Arrays.copyOf(cells, size * 2);
        }
        int i = size++;
        keys[i] = key;
        cells[i] = cell;
        while (i > 0) {
            int p = (i - 1) >>> 1;
            if (!less(i, p)) break;
            swap(i, p);
            i = p;
        }
    }

    double topKey() { return keys[0]; }

    /** Removes the smallest pair and returns its cell. */
    int pop() {
        int out = cells[0];
        size--;
        if (size > 0) {
            keys[0] = keys[size];
            cells[0] = cells[size];
            int i = 0;
            while (true) {
                int l = 2 * i + 1, r = l + 1, m = i;
                if (l < size && less(l, m)) m = l;
                if (r < size && less(r, m)) m = r;
                if (m == i) break;
                swap(i, m);
                i = m;
            }
        }
        return out;
    }

    private void swap(int a, int b) {
        double k = keys[a]; keys[a] = keys[b]; keys[b] = k;
        int c = cells[a]; cells[a] = cells[b]; cells[b] = c;
    }
}
