package org.aouessar.core.gen.terrain;

import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.util.Mathx;
import org.aouessar.core.util.noise.FastNoiseLite;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.LayerRect;

public class SimpleWorldGenerator implements WorldGenerator {

    @Override
    public Heightmap generateHeightmap(long seed) {
        int size = EngineConstants.GEN_RADIUS_CHUNKS * 2 * EngineConstants.CHUNK_SIZE;
        LayerRect rect = new LayerRect(-size / 2, -size / 2, size, size);

        // NOTE: These are private methods of THIS class (not FastNoiseLite methods)
        FastNoiseLite continents = createContinentsNoise(seed);
        FastNoiseLite mountains  = createMountainsNoise(seed);

        short[] heights = new short[rect.area()];

        for (int z = rect.minZ(); z < rect.minZ() + rect.sizeZ(); z++) {
            for (int x = rect.minX(); x < rect.minX() + rect.sizeX(); x++) {

                float c = Mathx.noise01(continents.GetNoise(x, z)); // 0..1
                c = Mathx.smoothstep(EngineConstants.CONTINENTS_EDGE0, EngineConstants.CONTINENTS_EDGE1, c);

                float m = Mathx.noise01(mountains.GetNoise(x, z));  // 0..1
                m = m * m;

                float base = Mathx.lerp(
                        EngineConstants.SEA_LEVEL + EngineConstants.BASE_MIN_OFFSET,
                        EngineConstants.SEA_LEVEL + EngineConstants.BASE_MAX_OFFSET,
                        c
                );

                float mountainAdd = Mathx.lerp(0f, EngineConstants.MOUNTAINS_MAX_ADD, m) * c;

                int h = Mathx.clamp((int) (base + mountainAdd), 1, EngineConstants.WORLD_HEIGHT - 1);
                heights[rect.index2D(x, z)] = (short) h;
            }
        }

        return new Heightmap(rect, heights);
    }

    private static FastNoiseLite createContinentsNoise(long seed) {
        FastNoiseLite n = new FastNoiseLite((int) seed);
        n.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        n.SetFrequency(EngineConstants.CONTINENTS_FREQ);
        return n;
    }

    private static FastNoiseLite createMountainsNoise(long seed) {
        FastNoiseLite n = new FastNoiseLite((int) (seed ^ 0x9E3779B97F4A7C15L));
        n.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        n.SetFrequency(EngineConstants.MOUNTAINS_FREQ);
        return n;
    }

}