package org.aouessar.utils;

public final class Utils {
    private Utils() {}

    // ----------------------------
    // World dimensions
    // ----------------------------
    public static final int SEA_LEVEL = 62;

    // World vertical range (used by generator, far tiles, and near streaming)
    public static final int WORLD_MIN_Y = -64;
    public static final int WORLD_MAX_Y = 460;

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


    // -------------------------------------------------------------------------
    // Terrain Generator Tuning (production-safe defaults)
    // Prefix TG_ to avoid collisions with your existing constants.
    // -------------------------------------------------------------------------

    // Continents
    public static final float TG_CONTINENT_FREQ = 1.0f / 4200.0f;
    public static final float TG_CONTINENT_WARP_FREQ = 1.0f / 2200.0f;
    public static final float TG_CONTINENT_WARP_AMP = 380.0f;
    public static final float TG_CONTINENT_EDGE0 = -0.10f;
    public static final float TG_CONTINENT_EDGE1 = 0.18f;

    // Land shaping
    public static final int   TG_LAND_BASE_RISE = 8;
    public static final float TG_HILLS_FREQ = 1.0f / 520.0f;
    public static final int   TG_HILLS_AMPLITUDE = 30;

    // Mountains (normal)
    public static final float TG_MOUNTAINS_FREQ = 1.0f / 1200.0f;
    public static final int   TG_MOUNTAINS_AMPLITUDE = 185;          // a bit stronger to hit 230–260 often
    public static final int   TG_STONE_EXPOSE_HEIGHT = 140;
    public static final int   TG_STONE_EXPOSE_DEPTH = 2;

    // Mega mountains (rare "Everest/Himalaya" feel)
    // NOTE: With WORLD_MAX_Y=260, these will clamp at the top. For true ~460 peaks, raise WORLD_MAX_Y accordingly.
    public static final float TG_MEGA_MOUNTAINS_FREQ = 1.0f / 6400.0f;
    public static final float TG_MEGA_MOUNTAINS_THRESH0 = 0.68f;
    public static final float TG_MEGA_MOUNTAINS_THRESH1 = 0.88f;
    public static final int   TG_MEGA_MOUNTAINS_AMPLITUDE = 420;

    // Oceans
    public static final int   TG_OCEAN_DEPTH = 45;
    public static final float TG_OCEAN_FLOOR_FREQ = 1.0f / 900.0f;
    public static final int   TG_OCEAN_FLOOR_VARIATION = 10;

    // Rivers (more visible + biased toward ocean)
    public static final float TG_RIVER_FREQ = 1.0f / 720.0f;
    public static final float TG_RIVER_WARP_FREQ = 1.0f / 1400.0f;
    public static final float TG_RIVER_WARP_AMP = 240.0f;
    public static final float TG_RIVER_LINE0 = 0.04f;               // wider than before
    public static final float TG_RIVER_LINE1 = 0.14f;
    public static final int   TG_RIVER_DEPTH = 14;
    public static final float TG_RIVER_INLAND_RISE = 6.0f;          // reduced to avoid shore flooding

    // "Flow to ocean" warp params (cheap gradient)
    public static final int   TG_FLOW_GRAD_STEP = 64;
    public static final float TG_RIVER_FLOW_AMP = 520.0f;

    // Water-surface safety caps (prevents +1/+2 floating water near shores)
    public static final float TG_RIVER_WATER_CORE_MIN = 0.55f;       // only lift water in river core, not banks
    public static final int   TG_RIVER_SURFACE_CAP_ABOVE_BED = 6;    // water surface <= (riverbed + cap)

    // Lakes
    public static final float TG_LAKE_FREQ = 1.0f / 1800.0f;
    public static final float TG_LAKE_THRESH0 = 0.72f;
    public static final float TG_LAKE_THRESH1 = 0.92f;
    public static final int   TG_LAKE_CARVE_DEPTH = 8;
    public static final int   TG_LAKE_LEVEL_RISE = 5;

    public static final float TG_LAKE_WATER_CORE_MIN = 0.45f;
    public static final int   TG_LAKE_SURFACE_CAP_ABOVE_BED = 5;

    // Beaches / soil
    public static final int   TG_BEACH_MAX_ABOVE_SEA = 2;
    public static final int   TG_TOPSOIL_DEPTH = 3;
    public static final int   TG_SEABED_SOIL_DEPTH = 5;

    // Biomes
    public static final float TG_TEMP_FREQ = 1.0f / 6500.0f;
    public static final float TG_MOIST_FREQ = 1.0f / 5200.0f;

    // Latitude model: equator at wz=0, "poles" at +-TG_LATITUDE_SCALE
    public static final float TG_LATITUDE_SCALE = 22000.0f;         // increase for slower climate change over distance
    public static final float TG_TEMP_LAT_WEIGHT = 0.72f;           // 0..1, higher = deserts concentrate near equator

    public static final float TG_DESERT_TEMP_MIN = 0.64f;
    public static final float TG_DESERT_MOIST_MAX = 0.30f;
    public static final float TG_FOREST_MOIST_MIN = 0.62f;

    public static final int NEAR_RING_RADIUS_CHUNKS = 10;
    // Far tiles inside this distance are NOT rendered (prevents far under near).
    public static final float FAR_HIDE_UNDER_NEAR_EXTRA_BLOCKS = 24f;
}