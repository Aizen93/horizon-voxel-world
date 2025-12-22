package org.aouessar.core.world;

import org.aouessar.utils.Utils;

/**
 * Near-field voxel chunk (column):
 * Dimensions are CHUNK_SIZE (X) × WORLD_MAX_Y (Y) × CHUNK_SIZE (Z).
 *
 * This avoids the classic bug where a 32×32×32 chunk silently caps the world height at 32.
 */
public final class Chunk {

    /** Chunk width/depth in blocks. */
    public static final int SIZE = Utils.CHUNK_SIZE;

    /** Chunk height in blocks (world vertical range). */
    public static final int HEIGHT = Utils.WORLD_MAX_Y; // y in [0, HEIGHT-1]

    public final ChunkPos pos;

    private final short[] blocks = new short[SIZE * HEIGHT * SIZE];

    public Chunk(ChunkPos pos) {
        this.pos = pos;
    }

    public short get(int x, int y, int z) {
        if (!inBounds(x, y, z)) return BlockId.AIR;
        return blocks[idx(x, y, z)];
    }

    public void set(int x, int y, int z, short id) {
        if (!inBounds(x, y, z)) return;
        blocks[idx(x, y, z)] = id;
    }

    public static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE
                && y >= 0 && y < HEIGHT
                && z >= 0 && z < SIZE;
    }

    /**
     * Layout: X fastest, then Z, then Y
     * index = ((y * SIZE) + z) * SIZE + x
     */
    private static int idx(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }
}