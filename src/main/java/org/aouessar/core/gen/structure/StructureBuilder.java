package org.aouessar.core.gen.structure;

import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.StructureMap;

public interface StructureBuilder {

    StructureMap placeStructures(long seed, Heightmap heightmap, BiomeMap biomeMap);

}