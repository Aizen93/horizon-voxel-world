package org.aouessar.core.world;

public final class SimpleWorldGenerator implements WorldGenerator {

    private final long seed;
    private final int seaLevel;

    public SimpleWorldGenerator(WorldSeed seed, int seaLevel) {
        this.seed = seed.value();
        this.seaLevel = seaLevel;
        System.out.println("BlockId class used by generator = " + BlockId.class.getName());
        System.out.println("BlockId values: AIR=" + BlockId.AIR + " GRASS=" + BlockId.GRASS
                + " DIRT=" + BlockId.DIRT + " STONE=" + BlockId.STONE + " WATER=" + BlockId.WATER);
    }

    @Override
    public short blockAt(int wx, int wy, int wz) {
        int h = heightAt(wx, wz);

        // Above surface
        if (wy > h) {
            return (wy <= seaLevel) ? BlockId.WATER : BlockId.AIR;
        }

        // Surface (must be BEFORE subsurface checks)
        if (wy == h) {
            if (h <= seaLevel + 1) return BlockId.SAND;
            return BlockId.GRASS;
        }

        // Subsurface band
        if (wy >= h - 3) {
            return BlockId.DIRT;
        }

        // Deep
        return BlockId.STONE;
    }


    @Override
    public int heightAt(int x, int z) {
        // Continents (very low frequency)
        float c = fbm(x * 0.0015f, z * 0.0015f, 4, 2.0f, 0.5f);
        c = (c + 1f) * 0.5f; // [0..1]

        // Detail (higher frequency)
        float d = fbm(x * 0.01f, z * 0.01f, 5, 2.1f, 0.5f);
        d = (d + 1f) * 0.5f; // [0..1]

        // Blend: continents define base, detail adds bumps
        float base = 25f + c * 70f;
        float bumps = (d - 0.5f) * 18f;

        int h = Math.round(base + bumps);

        // clamp to world range (defensive)
        if (h < 1) h = 1;
        if (h >= org.aouessar.utils.Utils.WORLD_MAX_Y) h = org.aouessar.utils.Utils.WORLD_MAX_Y - 1;

        return h;
    }

    // -------------------- Noise --------------------

    private float fbm(float x, float z, int octaves, float lacunarity, float gain) {
        float amp = 1f;
        float freq = 1f;
        float sum = 0f;
        float norm = 0f;

        for (int i = 0; i < octaves; i++) {
            sum += amp * noise2(x * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return (norm == 0f) ? 0f : (sum / norm);
    }

    private float noise2(float x, float z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float tz = z - z0;

        float sx = smoothstep(tx);
        float sz = smoothstep(tz);

        float n00 = gradDot(x0, z0, tx, tz);
        float n10 = gradDot(x1, z0, tx - 1f, tz);
        float n01 = gradDot(x0, z1, tx, tz - 1f);
        float n11 = gradDot(x1, z1, tx - 1f, tz - 1f);

        float nx0 = lerp(n00, n10, sx);
        float nx1 = lerp(n01, n11, sx);

        return lerp(nx0, nx1, sz);
    }

    private float gradDot(int xi, int zi, float x, float z) {
        long h = hash2(xi, zi, seed);

        // 8 directions (unit-ish), chosen by low bits
        return switch ((int) (h & 7L)) {
            case 0 -> x + z;
            case 1 -> -x + z;
            case 2 -> x - z;
            case 3 -> -x - z;
            case 4 -> x;
            case 5 -> -x;
            case 6 -> z;
            default -> -z;
        };
    }

    private static long hash2(int x, int z, long seed) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private static int fastFloor(float v) {
        int i = (int) v;
        return (v < i) ? (i - 1) : i;
    }
}