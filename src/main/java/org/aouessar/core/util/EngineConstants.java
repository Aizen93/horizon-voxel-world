package org.aouessar.core.util;

/**
 * Single source of truth for engine + world constants.
 * Keep values grouped and documented. No runtime mutation.
 */
public final class EngineConstants {
    private EngineConstants() {}

    // ---------------------------
    // World dimensions
    // ---------------------------
    public static final int CHUNK_SIZE = 32;
    public static final int WORLD_HEIGHT = 256;
    public static final int SEA_LEVEL = 62;

    // Generation window (bootstrap): layers are generated for a rectangular window around (0,0).
    public static final int GEN_RADIUS_CHUNKS = 8;

    // ---------------------------
    // Safety limits (avoid accidental huge allocations)
    // ---------------------------
    public static final int MAX_WORLD_HEIGHT = 4096;
    public static final int MAX_LAYER_AREA = 16_777_216; // 4096*4096
    public static final long MAX_LAYER_VOLUME = 268_435_456L; // example cap for 3D masks

    // ---------------------------
    // Terrain noise parameters
    // ---------------------------
    public static final float CONTINENTS_FREQ = 0.0008f;
    public static final float MOUNTAINS_FREQ  = 0.0025f;

    // Continents shaping curve
    public static final float CONTINENTS_EDGE0 = 0.35f;
    public static final float CONTINENTS_EDGE1 = 0.65f;

    // Base terrain ranges around sea level
    public static final float BASE_MIN_OFFSET = -18f; // seaLevel + offset
    public static final float BASE_MAX_OFFSET = +26f;

    // Mountains amplitude
    public static final float MOUNTAINS_MAX_ADD = 120f;

    // ---------------------------
    // River carving parameters (bootstrap)
    // ---------------------------
    public static final float RIVER_FREQ = 0.0012f;
    public static final float RIVER_BAND = 0.06f; // narrower => fewer rivers
    public static final int   RIVER_MIN_DEPTH = 2;
    public static final int   RIVER_MAX_DEPTH = 8;

    public static final int REGION_SIZE_CHUNKS = 16;

    // ---------------------------
    // Startup validation
    // ---------------------------
    public static void validate() {
        if (CHUNK_SIZE <= 0) throw new IllegalStateException("CHUNK_SIZE must be > 0");
        if (WORLD_HEIGHT <= 0 || WORLD_HEIGHT > MAX_WORLD_HEIGHT)
            throw new IllegalStateException("WORLD_HEIGHT out of range: " + WORLD_HEIGHT);
        if (SEA_LEVEL < 1 || SEA_LEVEL >= WORLD_HEIGHT)
            throw new IllegalStateException("SEA_LEVEL must be in [1.." + (WORLD_HEIGHT - 1) + "]");
        if (GEN_RADIUS_CHUNKS <= 0) throw new IllegalStateException("GEN_RADIUS_CHUNKS must be > 0");
    }

}