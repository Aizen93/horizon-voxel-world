package org.aouessar.core.mesh;

import org.aouessar.core.world.Chunk;
import org.aouessar.core.world.WorldGenerator;

public interface Mesher {

    MeshData buildChunkMesh(Chunk chunk, WorldGenerator gen);
}

