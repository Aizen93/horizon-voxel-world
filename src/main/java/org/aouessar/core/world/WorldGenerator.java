package org.aouessar.core.world;

public interface WorldGenerator {

    /**
     * Returns block id at world coordinates. Must be deterministic for the same seed.
     */
    short blockAt(int wx, int wy, int wz);

    /**
     * Returns the terrain surface height (world Y) at the given world XZ coordinates.
     * Used by far-field height LOD and near-field terrain filling.
     */
    int heightAt(int wx, int wz);
}