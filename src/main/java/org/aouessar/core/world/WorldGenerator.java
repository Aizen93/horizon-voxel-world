package org.aouessar.core.world;

public interface WorldGenerator {
    /** Returns block id at world coordinates. Must be deterministic for same seed. */
    short blockAt(int wx, int wy, int wz);
}