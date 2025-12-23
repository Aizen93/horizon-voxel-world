// SimpleWorldGenerator.java
package org.aouessar.core.world;


import static org.aouessar.core.world.BlockId.*;
import static org.aouessar.utils.Utils.*;

/**
 * Terrain generator v2:
 * - Fixes "far sand = grass" root cause indirectly by making deserts strongly temperature/latitude driven
 *   and beaches more consistently height-based (so far-field tinting has a better chance to match).
 *   (If your far-field mesh always uses a fixed material id, you'll still need to feed it a surface material id.
 *    But this version makes desert/beach detection more coherent and stable at distance.)
 * - Fixes floating shore water (+1/+2) by restricting river/lake water-level uplift to river/lake cores only.
 * - Rivers are now much more visible and biased to flow toward oceans via a cheap "flow-to-ocean" warp.
 * - Adds rare mega-mountain mask so some ranges hit WORLD_MAX_Y frequently (true 460 requires raising WORLD_MAX_Y).
 */
public final class SimpleWorldGenerator implements WorldGenerator {

    private final long seed;

    public SimpleWorldGenerator(WorldSeed worldSeed) {
        this.seed = worldSeed.value();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public int heightAt(int wx, int wz) {
        // Continents
        final float cont = continents(wx, wz); // ~[-1..1]
        final float landMask = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, cont); // [0..1]

        // Ocean floor
        final float oceanNoise = fbm2(wx, wz, TG_OCEAN_FLOOR_FREQ, 4, 0.55f, 2.0f);
        final float oceanFloor = SEA_LEVEL - TG_OCEAN_DEPTH + (oceanNoise * TG_OCEAN_FLOOR_VARIATION);

        // Base hills
        final float hills = fbm2(wx, wz, TG_HILLS_FREQ, 5, 0.5f, 2.0f);
        final float hills01 = 0.5f + 0.5f * hills; // [0..1]

        // Mountains (ridged), suppressed near coasts
        final float ridge = ridged2(wx, wz, TG_MOUNTAINS_FREQ, 5, 0.5f, 2.05f); // [0..1]
        final float inland = smoothstep(0.18f, 0.90f, landMask);
        final float mountainStrength = inland * inland;

        // Rare mega mountains: extremely low frequency ridges that sometimes "want" to exceed WORLD_MAX_Y.
        // (If you truly want peaks around 460, you must raise WORLD_MAX_Y and adjust vertical chunking accordingly.)
        final float mega = ridged2(wx + 7711, wz - 991, TG_MEGA_MOUNTAINS_FREQ, 4, 0.55f, 2.15f); // [0..1]
        final float megaMask = smoothstep(TG_MEGA_MOUNTAINS_THRESH0, TG_MEGA_MOUNTAINS_THRESH1, mega) * inland;

        float landHeight =
                (SEA_LEVEL + TG_LAND_BASE_RISE)
                        + (hills01 * TG_HILLS_AMPLITUDE)
                        + (ridge * ridge * TG_MOUNTAINS_AMPLITUDE * mountainStrength)
                        + (megaMask * TG_MEGA_MOUNTAINS_AMPLITUDE);

        // Blend ocean/land
        float h = lerp(oceanFloor, landHeight, landMask);

        // Rivers carve (more visible now)
        final float riverCore = riverCoreMask(wx, wz, landMask); // [0..1]
        if (riverCore > 0f) {
            h -= (TG_RIVER_DEPTH * riverCore);
        }

        // Lakes carve (sparse inland basins)
        final float lake = lakeMask(wx, wz, landMask);
        if (lake > 0f) {
            h -= TG_LAKE_CARVE_DEPTH * lake;
        }

        // Clamp
        int hi = fastFloor(h);
        if (hi < 1) hi = 1;
        if (hi > WORLD_MAX_Y - 1) hi = WORLD_MAX_Y - 1;
        return hi;
    }

    @Override
    public short blockAt(int wx, int wy, int wz) {
        if (wy < 0 || wy >= WORLD_MAX_Y) return AIR;

        final int h = heightAt(wx, wz);

        // Above ground: water volumes
        if (wy > h) {
            final int waterLevel = waterLevelAt(wx, wz, h);
            return (wy <= waterLevel) ? WATER : AIR;
        }

        // Biome/material decision
        final Biome biome = biomeAt(wx, wz);

        final float cont = continents(wx, wz);
        final float landMask = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, cont);

        // River influence for banks
        final float riverCore = riverCoreMask(wx, wz, landMask);
        final float riverBank = riverBankMask(wx, wz, landMask);

        // Beaches: height-based so it stays consistent (also helps far-field if it only has height).
        final boolean isBeach = (h <= SEA_LEVEL + TG_BEACH_MAX_ABOVE_SEA) && (landMask < 0.70f);

        // Top block
        if (wy == h) {
            if (isBeach) return SAND;

            // River banks / wet edges:
            if (riverBank > 0.15f) return SAND;

            // Desert surface:
            return (biome == Biome.DESERT) ? SAND : GRASS;
        }

        // Under top: soil then stone
        final int depth = h - wy;
        final boolean underwater = (h <= SEA_LEVEL);

        final int topSoil = underwater ? TG_SEABED_SOIL_DEPTH : TG_TOPSOIL_DEPTH;

        if (depth <= topSoil) {
            if (underwater) return SAND;
            if (biome == Biome.DESERT) return SAND;
            if (riverBank > 0.15f) return SAND;
            return DIRT;
        }

        // Stone exposure at high altitudes
        if (h > TG_STONE_EXPOSE_HEIGHT && depth <= TG_STONE_EXPOSE_DEPTH) return STONE;

        return STONE;
    }

    // -------------------------------------------------------------------------
    // Water / biome
    // -------------------------------------------------------------------------

    /**
     * Water level used ONLY for "wy > h" cells.
     * Key fix: we only raise water level for river/lake CORES (not banks), so we don't get +1/+2 floating sheets near shore.
     */
    private int waterLevelAt(int wx, int wz, int h) {
        int level = SEA_LEVEL;

        final float cont = continents(wx, wz);
        final float landMask = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, cont);

        // Rivers: only if we're in the core (deepest part), and bias to go to ocean.
        final float riverCore = riverCoreMask(wx, wz, landMask);
        if (riverCore > TG_RIVER_WATER_CORE_MIN) {
            // inland lift based on latitude+landness, but limited to avoid flooding above banks
            final float landish = landMask;
            final float lat = latitude01(wz);
            final float inlandRise = TG_RIVER_INLAND_RISE * landish * (0.65f + 0.35f * lat);

            int riverLevel = SEA_LEVEL + fastFloor(inlandRise);

            // Safety cap: never allow water surface to exceed "ground + max surface offset" for this column.
            // This prevents the "water hovering 1-2 blocks above shore ground" artifacts.
            final int cap = h + TG_RIVER_SURFACE_CAP_ABOVE_BED;
            if (riverLevel > cap) riverLevel = cap;

            if (riverLevel > level) level = riverLevel;
        }

        // Lakes: only inside lake mask (core), small lift
        final float lake = lakeMask(wx, wz, landMask);
        if (lake > TG_LAKE_WATER_CORE_MIN) {
            int lakeLevel = SEA_LEVEL + TG_LAKE_LEVEL_RISE;
            final int cap = h + TG_LAKE_SURFACE_CAP_ABOVE_BED;
            if (lakeLevel > cap) lakeLevel = cap;
            if (lakeLevel > level) level = lakeLevel;
        }

        if (level < 0) level = 0;
        if (level > WORLD_MAX_Y - 1) level = WORLD_MAX_Y - 1;
        return level;
    }

    /**
     * Deserts mostly near equator:
     * - temperature is strongly latitude-driven (equator hottest),
     * - moisture still noise-based,
     * - deserts need high temp + low moisture.
     */
    private Biome biomeAt(int wx, int wz) {
        final float lat = latitude01(wz); // 1 at equator, 0 at poles (in our world coords)

        // temp: mostly latitude + a bit of noise
        final float tNoise = 0.5f + 0.5f * fbm2(wx, wz, TG_TEMP_FREQ, 4, 0.5f, 2.0f);
        final float temp = clamp01((lat * TG_TEMP_LAT_WEIGHT) + (tNoise * (1f - TG_TEMP_LAT_WEIGHT)));

        // moisture: noise + tiny latitude effect (slightly drier near tropics if you want)
        final float mNoise = 0.5f + 0.5f * fbm2(wx, wz, TG_MOIST_FREQ, 4, 0.55f, 2.0f);
        final float moist = clamp01(mNoise);

        if (temp > TG_DESERT_TEMP_MIN && moist < TG_DESERT_MOIST_MAX) return Biome.DESERT;
        if (moist > TG_FOREST_MOIST_MIN) return Biome.FOREST;
        return Biome.PLAINS;
    }

    private float latitude01(int wz) {
        // 1 at equator (wz=0), falls to 0 at +/- TG_LATITUDE_SCALE
        float a = fastAbs((float) wz) / TG_LATITUDE_SCALE;
        if (a > 1f) a = 1f;
        return 1f - a;
    }

    // -------------------------------------------------------------------------
    // Continents / rivers / lakes (geometry fields)
    // -------------------------------------------------------------------------

    private float continents(int wx, int wz) {
        final float warpX = fbm2(wx, wz, TG_CONTINENT_WARP_FREQ, 3, 0.6f, 2.0f) * TG_CONTINENT_WARP_AMP;
        final float warpZ = fbm2(wx + 1337, wz - 7331, TG_CONTINENT_WARP_FREQ, 3, 0.6f, 2.0f) * TG_CONTINENT_WARP_AMP;
        return fbm2(wx + (int) warpX, wz + (int) warpZ, TG_CONTINENT_FREQ, 5, 0.5f, 2.0f);
    }

    /**
     * Rivers: we create a cheap "flow to ocean" warp by using the gradient of continent landMask.
     * This biases river lines to trend toward coasts/ocean without pathfinding or allocations.
     */
    private float riverCoreMask(int wx, int wz, float landMask) {
        if (landMask < 0.10f) return 0f; // no rivers in deep ocean

        // Compute a "downhill-to-ocean" flow using gradient of landMask (ocean is where landMask is low).
        // Central differences (very cheap: 4 continent samples).
        final float g = continents(wx, wz);
        final float lm = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, g);

        final float lmx1 = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, continents(wx + TG_FLOW_GRAD_STEP, wz));
        final float lmx0 = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, continents(wx - TG_FLOW_GRAD_STEP, wz));
        final float lmz1 = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, continents(wx, wz + TG_FLOW_GRAD_STEP));
        final float lmz0 = smoothstep(TG_CONTINENT_EDGE0, TG_CONTINENT_EDGE1, continents(wx, wz - TG_FLOW_GRAD_STEP));

        float dx = (lmx1 - lmx0);
        float dz = (lmz1 - lmz0);

        // We want to flow toward decreasing landMask (toward ocean), so invert gradient
        dx = -dx;
        dz = -dz;

        // Normalize-ish (avoid sqrt): scale by max(|dx|,|dz|)
        float adx = fastAbs(dx), adz = fastAbs(dz);
        float inv = 1f;
        float m = (adx > adz) ? adx : adz;
        if (m > 0.0001f) inv = 1f / m;

        dx *= inv;
        dz *= inv;

        // Flow-warped coordinates
        final float lat = latitude01(wz);
        final float flowAmp = TG_RIVER_FLOW_AMP * (0.6f + 0.4f * lat) * smoothstep(0.10f, 0.90f, lm);
        final int fx = wx + fastFloor(dx * flowAmp);
        final int fz = wz + fastFloor(dz * flowAmp);

        // Additional small domain warp to avoid straight lines
        final float w1 = fbm2(fx, fz, TG_RIVER_WARP_FREQ, 2, 0.65f, 2.0f) * TG_RIVER_WARP_AMP;
        final float w2 = fbm2(fx + 9123, fz - 117, TG_RIVER_WARP_FREQ, 2, 0.65f, 2.0f) * TG_RIVER_WARP_AMP;

        final float n = noise2((fx + (int) w1) * TG_RIVER_FREQ, (fz + (int) w2) * TG_RIVER_FREQ); // [-1..1]
        float a = fastAbs(n);

        // Wider rivers than before (so you can actually see them)
        float line = 1.0f - smoothstep(TG_RIVER_LINE0, TG_RIVER_LINE1, a);

        // Favor inland and ensure they have a chance to reach coast:
        // - allow rivers near coast a bit, but fade them in deep ocean
        line *= smoothstep(0.05f, 0.25f, landMask);

        return clamp01(line);
    }

    private float riverBankMask(int wx, int wz, float landMask) {
        // A slightly wider version used for sand banks (no extra noise calls: just re-map core)
        final float core = riverCoreMask(wx, wz, landMask);
        // Expand and soften
        float bank = core * 0.85f + 0.10f;
        if (bank > 1f) bank = 1f;
        return bank;
    }

    private float lakeMask(int wx, int wz, float landMask) {
        if (landMask < 0.25f) return 0f;

        final float n = fbm2(wx, wz, TG_LAKE_FREQ, 3, 0.55f, 2.0f); // [-1..1]
        float v = smoothstep(TG_LAKE_THRESH0, TG_LAKE_THRESH1, (0.5f + 0.5f * n)); // [0..1]
        v *= smoothstep(0.30f, 0.90f, landMask);
        return clamp01(v);
    }

    // -------------------------------------------------------------------------
    // Noise (deterministic, no allocations)
    // -------------------------------------------------------------------------

    private float noise2(float x, float z) {
        final int xi = fastFloor(x);
        final int zi = fastFloor(z);
        final float xf = x - xi;
        final float zf = z - zi;

        final float u = fade(xf);
        final float v = fade(zf);

        final int aa = hash2(xi, zi);
        final int ab = hash2(xi, zi + 1);
        final int ba = hash2(xi + 1, zi);
        final int bb = hash2(xi + 1, zi + 1);

        final float x1 = lerp(grad2(aa, xf, zf), grad2(ba, xf - 1f, zf), u);
        final float x2 = lerp(grad2(ab, xf, zf - 1f), grad2(bb, xf - 1f, zf - 1f), u);
        return lerp(x1, x2, v);
    }

    private float fbm2(int wx, int wz, float freq, int octaves, float gain, float lacunarity) {
        float amp = 1.0f;
        float sum = 0.0f;
        float norm = 0.0f;

        float x = wx * freq;
        float z = wz * freq;

        for (int i = 0; i < octaves; i++) {
            sum += noise2(x, z) * amp;
            norm += amp;
            amp *= gain;
            x *= lacunarity;
            z *= lacunarity;
        }
        return sum / norm;
    }

    private float ridged2(int wx, int wz, float freq, int octaves, float gain, float lacunarity) {
        float amp = 1.0f;
        float sum = 0.0f;
        float norm = 0.0f;

        float x = wx * freq;
        float z = wz * freq;

        for (int i = 0; i < octaves; i++) {
            float n = noise2(x, z);
            n = 1.0f - fastAbs(n); // [0..1]
            n *= n;
            sum += n * amp;
            norm += amp;
            amp *= gain;
            x *= lacunarity;
            z *= lacunarity;
        }
        return clamp01(sum / norm);
    }

    private int hash2(int x, int z) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return (int) h;
    }

    private float grad2(int hash, float x, float z) {
        final int h = hash & 7;
        final float u = (h < 4) ? x : z;
        final float v = (h < 4) ? z : x;
        final float a = ((h & 1) == 0) ? u : -u;
        final float b = ((h & 2) == 0) ? v : -v;
        return a + b;
    }

    // -------------------------------------------------------------------------
    // Math utils
    // -------------------------------------------------------------------------

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = (x - e0) / (e1 - e0);
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    private static float clamp01(float x) {
        if (x < 0f) return 0f;
        if (x > 1f) return 1f;
        return x;
    }

    private static int fastFloor(float x) {
        final int i = (int) x;
        return (x < i) ? (i - 1) : i;
    }

    private static float fastAbs(float x) {
        return (x < 0f) ? -x : x;
    }

    private enum Biome {
        PLAINS, FOREST, DESERT
    }
}
