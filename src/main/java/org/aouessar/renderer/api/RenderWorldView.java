package org.aouessar.renderer.api;

import org.aouessar.core.world.chunk.Chunk;
import org.aouessar.core.world.layer.Heightmap;

public interface RenderWorldView {

    Chunk chunkAt(int cx, int cz);

    Heightmap heightmap(); // for far-field mesh

}
