package org.aouessar.core.gen.carver;

import org.aouessar.core.world.layer.CarveMask;
import org.aouessar.core.world.layer.Heightmap;

public interface WorldCarver {

    CarveMask generateCarveMask(long seed, Heightmap heightmap);

}