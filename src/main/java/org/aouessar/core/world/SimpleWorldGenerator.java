package org.aouessar.core.world;

public final class SimpleWorldGenerator implements WorldGenerator {

    private final long seed;
    private final int seaLevel;

    public SimpleWorldGenerator(WorldSeed seed, int seaLevel) {
        this.seed = seed.value();
        this.seaLevel = seaLevel;
    }

    @Override
    public short blockAt(int wx, int wy, int wz) {
        if (wy < 0) return BlockId.STONE;

        int h = heightAt(wx, wz);

        if (wy > h) {
            return (wy <= seaLevel) ? BlockId.WATER : BlockId.AIR;
        }

        // ground layers
        if (wy == h) return BlockId.GRASS;
        if (wy >= h - 3) return BlockId.DIRT;
        return BlockId.STONE;
    }

    public int heightAt(int x, int z) {
        // Simple FBM value noise => mountains + variation.
        float n1 = fbm2(x * 0.004f, z * 0.004f, 5, 2.0f, 0.5f);
        float n2 = fbm2(x * 0.0015f, z * 0.0015f, 4, 2.0f, 0.55f);

        float base = 40f;
        float hills = n1 * 35f;
        float mountains = (float)Math.pow(Math.max(0f, n2), 1.7) * 80f;

        return (int)(base + hills + mountains);
    }

    private float fbm2(float x, float z, int octaves, float lacunarity, float gain) {
        float amp = 1f;
        float freq = 1f;
        float sum = 0f;
        float norm = 0f;

        for (int i = 0; i < octaves; i++) {
            sum += amp * valueNoise2(x * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return sum / norm; // 0..1-ish
    }

    // Deterministic 2D value noise (0..1), smooth interpolated.
    private float valueNoise2(float x, float z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float tz = z - z0;

        float v00 = hash01(x0, z0);
        float v10 = hash01(x1, z0);
        float v01 = hash01(x0, z1);
        float v11 = hash01(x1, z1);

        float sx = smoothstep(tx);
        float sz = smoothstep(tz);

        float a = lerp(v00, v10, sx);
        float b = lerp(v01, v11, sx);
        return lerp(a, b, sz);
    }

    private float hash01(int x, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        // Map to [0,1)
        return (h & 0xFFFFFF) / (float)0x1000000;
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
