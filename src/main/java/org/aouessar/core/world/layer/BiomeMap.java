package org.aouessar.core.world.layer;

import java.io.Serializable;

public final class BiomeMap implements Serializable {

    public static final byte OCEAN = 0;
    public static final byte PLAINS = 1;
    public static final byte FOREST = 2;
    public static final byte DESERT = 3;
    public static final byte MOUNTAINS = 4;
    public static final byte SWAMP = 5;

    private final LayerRect rect;
    private final byte[] biomes;

    public BiomeMap(LayerRect rect, byte[] biomes) {
        this.rect = rect;
        this.biomes = biomes.clone();
    }

    public byte biomeAt(int x, int z) {
        if (!rect.contains(x, z)) {
            throw new IllegalArgumentException("Out of rect");
        }

        return biomes[rect.index2D(x, z)];
    }

    public LayerRect rect() {
        return rect;
    }

}