package org.aouessar.core.world.chunk;

public interface ChunkBuilder {

    Chunk buildChunk(int cx, int cz);

    short blockAt(int wx, int wy, int wz);

}