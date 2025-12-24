package org.aouessar.core.gen.biome;

import org.aouessar.core.util.noise.FastNoiseLite;
import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.LayerRect;

import static org.aouessar.core.util.EngineConstants.*;

public final class DefaultBiomeGenerator implements BiomeGenerator {

    @Override
    public BiomeMap generateBiomes(long seed, Heightmap heightmap) {
        LayerRect rect = heightmap.rect();

        FastNoiseLite temp = new FastNoiseLite((int)(seed ^ 0xBADC0FFEE0DDF00DL));
        temp.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        temp.SetFrequency(0.0015f);

        FastNoiseLite humid = new FastNoiseLite((int)(seed ^ 0x12345678ABCDEF01L));
        humid.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        humid.SetFrequency(0.0015f);

        byte[] b = new byte[rect.sizeX() * rect.sizeZ()];
        for (int z = rect.minZ(); z < rect.minZ() + rect.sizeZ(); z++) {
            for (int x = rect.minX(); x < rect.minX() + rect.sizeX(); x++) {
                int h = heightmap.heightAt(x, z);
                if (h <= SEA_LEVEL - 2) {
                    b[rect.index2D(x, z)] = BiomeMap.OCEAN;
                    continue;
                }
                float t = (temp.GetNoise(x, z) + 1f) * 0.5f;
                float u = (humid.GetNoise(x, z) + 1f) * 0.5f;

                if (h > SEA_LEVEL + 70) b[rect.index2D(x, z)] = BiomeMap.MOUNTAINS;
                else if (t > 0.70f && u < 0.35f) b[rect.index2D(x, z)] = BiomeMap.DESERT;
                else if (u > 0.65f) b[rect.index2D(x, z)] = BiomeMap.SWAMP;
                else if (u > 0.45f) b[rect.index2D(x, z)] = BiomeMap.FOREST;
                else b[rect.index2D(x, z)] = BiomeMap.PLAINS;
            }
        }
        return new BiomeMap(rect, b);
    }
}