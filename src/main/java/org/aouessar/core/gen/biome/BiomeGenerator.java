package org.aouessar.core.gen.biome;

import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.Heightmap;

public interface BiomeGenerator {

    BiomeMap generateBiomes(long seed, Heightmap heightmap);

}