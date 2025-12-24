package org.aouessar.core.world.chunk;

public final class Chunk {

    public final int cx, cz;
    public final int size;
    public final int height;
    public final short[] blocks; // packed Y-major

    public Chunk(int cx, int cz, int size, int height, short[] blocks) {
        this.cx = cx;
        this.cz = cz;
        this.size = size;
        this.height = height;
        this.blocks = blocks;
    }

}