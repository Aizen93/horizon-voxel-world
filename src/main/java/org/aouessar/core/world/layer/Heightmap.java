package org.aouessar.core.world.layer;

import java.io.Serializable;
import java.util.Arrays;

public final class Heightmap implements Serializable {

    private final LayerRect rect;
    private final short[] heights; // sizeX*sizeZ, short is enough up to 32767

    public Heightmap(LayerRect rect, short[] heights) {
        if (rect == null) throw new IllegalArgumentException("rect is null");
        if (heights == null) throw new IllegalArgumentException("heights is null");
        if (heights.length != rect.area()) {
            throw new IllegalArgumentException("heights length mismatch: " + heights.length + " != " + rect.area());
        }
        this.rect = rect;
        this.heights = Arrays.copyOf(heights, heights.length);
    }

    public LayerRect rect() {
        return rect;
    }

    public int heightAt(int x, int z) {
        if (!rect.contains(x, z)) {
            throw new IllegalArgumentException("Out of rect");
        }

        return heights[rect.index2D(x, z)] & 0xFFFF;
    }
}