package org.aouessar.core.gen.surface;

import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.SurfaceRules;

public interface SurfaceDecorator {

    SurfaceRules generateSurfaceRules(Heightmap heightmap, BiomeMap biomeMap);

}