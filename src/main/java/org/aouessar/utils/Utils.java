package org.aouessar.utils;

public final class Utils {
    private Utils() {}

    // ----------------------------
    // World dimensions
    // ----------------------------
    public static final int SEA_LEVEL = 62;

    // World vertical range (used by generator, far tiles, and near streaming)
    public static final int WORLD_MIN_Y = -64;
    public static final int WORLD_MAX_Y = 260;

    // Near-field chunk dimensions
    public static final int CHUNK_SIZE = 32;   // 32x32x32 chunks are a good default

    // Far-field tile size in blocks (XZ only)
    public static final int TILE_SIZE = 256;   // keep aligned everywhere

    // ----------------------------
    // Far field LOD
    // ----------------------------
    // LOD -> sampling step in blocks (bigger = cheaper)
    public static final int FAR_LOD0_STEP = 4;
    public static final int FAR_LOD1_STEP = 8;
    public static final int FAR_LOD2_STEP = 16;
    public static final int FAR_LOD3_STEP = 128;

    // Rings (in TILE units)
    public static final int FAR_LOD0_RMIN = 0,  FAR_LOD0_RMAX = 2;
    public static final int FAR_LOD1_RMIN = 2,  FAR_LOD1_RMAX = 5;
    public static final int FAR_LOD2_RMIN = 5,  FAR_LOD2_RMAX = 12;
    public static final int FAR_LOD3_RMIN = 16, FAR_LOD3_RMAX = 64;

    // Must be >= FAR_LOD3_RMAX (and a bit bigger helps)
    public static final int FAR_KEEP_RADIUS = 70;

    // Skirts hide cracks between LOD tiles
    public static final float FAR_SKIRT_DEPTH = 40f;
    public static final float FAR_Y_BIAS = 0.15f; // push far mesh slightly down to avoid z-fight

    // ----------------------------
    // Far shading helpers
    // ----------------------------
    /** Biome noise cell size: 1 << FAR_BIOME_SHIFT blocks (7 => 128 blocks). */
    public static final int FAR_BIOME_SHIFT = 7;

    /** Height normalization used by *far* meshes when they want to encode height into UVs. */
    public static final float FAR_HEIGHT_NORM_MIN = 20f;
    public static final float FAR_HEIGHT_NORM_RANGE = 220f;

    // Streaming / throughput caps
    public static final int FAR_MAX_IN_FLIGHT = 128;
    public static final int FAR_MAX_READY = 512;
    public static final int FAR_MAX_PENDING = 8192;

    // Look-ahead for directional prefetch
    public static final float FAR_LOOKAHEAD_FACTOR = 2.5f;
    public static final float FAR_LOOKAHEAD_MAX = 12000f;

    // Optional: hard time budget for uploads each frame (nanoseconds)
    public static final long FAR_UPLOAD_BUDGET_NS = 2_000_000L; // 2ms

    // ----------------------------
    // Far-field upload pacing
    // Upload limits (per frame) — scaled by speed (see renderer)
    // ----------------------------
    public static final int FAR_UPLOAD_BASE = 6;
    public static final int FAR_UPLOAD_FAST = 32;
    public static final int FAR_UPLOAD_VERY_FAST = 40;
    public static final int FAR_UPLOAD_ULTRA = 60;

    // Camera movement threshold (blocks per frame)
    public static final float CAMERA_FAST_MOVE_THRESHOLD = 1.5f;

    // ----------------------------
    // Fog (shared by renderer & shader)
    // ----------------------------
    public static final float FOG_START = 600f;
    public static final float FOG_RANGE = 1200f;
}