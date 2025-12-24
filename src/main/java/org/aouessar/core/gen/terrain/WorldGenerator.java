package org.aouessar.core.gen.terrain;

import org.aouessar.core.world.layer.Heightmap;

public interface WorldGenerator {

    Heightmap generateHeightmap(long seed);

}