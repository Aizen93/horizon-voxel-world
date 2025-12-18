package org.aouessar.core.world;

public interface ChunkStore {

    /**
     * already available
     * @param pos
     * @return
     */
    Chunk getOrNull(ChunkPos pos);

    /**
     * async request/generation
     * @param pos
     */
    void request(ChunkPos pos);

}
