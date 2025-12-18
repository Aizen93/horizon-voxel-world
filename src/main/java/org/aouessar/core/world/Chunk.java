package org.aouessar.core.world;

public final class Chunk {

    public static final int SIZE = 32;

    public final ChunkPos pos;

    private final short[] blocks = new short[SIZE * SIZE * SIZE];

    public Chunk(ChunkPos pos) {
        this.pos = pos;
    }

    public short get(int x, int y, int z) {
        return blocks[idx(x, y, z)];

    }

    public void  set(int x, int y, int z, short id) {
        blocks[idx(x, y, z)] = id;
    }

    private static int idx(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }
}
