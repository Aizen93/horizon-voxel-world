package org.aouessar.core.world.layer;

import java.io.Serializable;

/**
 * Carve mask / density modifier.
 * For simplicity: stores "isCarvedAir" for each (x,y,z) inside the window volume.
 * In production, you can compress (RLE/bitset) or store 2D river depths + 3D caves.
 */
public final class CarveMask implements Serializable {

    private final LayerRect rect;
    private final int minY;
    private final int sizeY;
    private final byte[] carved; // 0/1 bit-ish

    public CarveMask(LayerRect rect, int minY, int sizeY, byte[] carved) {
        if (rect == null) throw new IllegalArgumentException("rect is null");
        if (sizeY <= 0) throw new IllegalArgumentException("sizeY must be > 0");
        if (carved == null) throw new IllegalArgumentException("carved is null");

        this.rect = rect;
        this.minY = minY;
        this.sizeY = sizeY;
        this.carved = carved.clone();
    }

    public boolean isCarvedToAir(int x, int y, int z) {
        if (!rect.contains(x, z) || y < minY || y >= minY + sizeY) return false;
        int sx = rect.sizeX();
        int sy = sizeY;
        int idx = ((y - minY) * rect.sizeZ() + (z - rect.minZ())) * sx + (x - rect.minX());
        return carved[idx] != 0;
    }

    public LayerRect rect() {
        return rect;
    }

    public int minY() {
        return minY;
    }

    public int sizeY() {
        return sizeY;
    }

}