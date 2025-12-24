package org.aouessar.core.world.layer;

import org.aouessar.core.util.EngineConstants;

/**
 * A rectangular window in world XZ coordinates that a layer covers.
 * @param minX
 * @param minZ
 * @param sizeX
 * @param sizeZ
 */
public record LayerRect(int minX, int minZ, int sizeX, int sizeZ) {

    public LayerRect {
        if (sizeX <= 0 || sizeZ <= 0) throw new IllegalArgumentException("LayerRect sizes must be > 0");
        long area = (long) sizeX * (long) sizeZ;
        if (area > EngineConstants.MAX_LAYER_AREA) {
            throw new IllegalArgumentException("LayerRect too large: " + sizeX + "x" + sizeZ);
        }
    }

    public boolean contains(int x, int z) {
        return x >= minX() && z >= minZ() && x < (minX() + sizeX()) && z < (minZ() + sizeZ());
    }

    /** 2D index for packed arrays sized sizeX*sizeZ (row-major by z then x). */
    public int index2D(int x, int z) {
        return (z - minZ()) * sizeX() + (x - minX());
    }

    public int area() {
        return sizeX() * sizeZ();
    }

}