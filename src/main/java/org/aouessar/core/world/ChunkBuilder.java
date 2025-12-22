package org.aouessar.core.world;

public final class ChunkBuilder {
    private ChunkBuilder() {}

    public static void fillChunk(Chunk chunk, WorldGenerator gen) {
        int baseX = chunk.pos.cx() * Chunk.SIZE;
        int baseY = chunk.pos.cy() * Chunk.SIZE;
        int baseZ = chunk.pos.cz() * Chunk.SIZE;

        for (int y = 0; y < Chunk.SIZE; y++) {
            int wy = baseY + y;
            for (int z = 0; z < Chunk.SIZE; z++) {
                int wz = baseZ + z;
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int wx = baseX + x;
                    chunk.set(x, y, z, gen.blockAt(wx, wy, wz));
                }
            }
        }
    }
}